package com.sitesnag.tracker.data.repository

import android.content.Context
import com.google.gson.Gson
import com.sitesnag.tracker.data.local.dao.ObservationDao
import com.sitesnag.tracker.data.local.dao.ProjectDao
import com.sitesnag.tracker.data.local.dao.SyncOutboxDao
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.data.local.entity.ProjectEntity
import com.sitesnag.tracker.data.local.entity.SyncOutboxEntity
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import com.sitesnag.tracker.util.PdfGenerator
import kotlinx.coroutines.flow.Flow

class ObservationRepository(
    private val context: Context,
    private val observationDao: ObservationDao,
    private val projectDao: ProjectDao,
    private val syncOutboxDao: SyncOutboxDao
) {
    private val gson = Gson()

    fun getAllObservationsFlow(projectId: Long): Flow<List<ObservationEntity>> =
        observationDao.getAllObservationsFlow(projectId)

    suspend fun getAllObservations(projectId: Long): List<ObservationEntity> =
        observationDao.getAllObservations(projectId)

    fun getObservationsByTypeFlow(projectId: Long, type: RecordType): Flow<List<ObservationEntity>> =
        observationDao.getObservationsByTypeFlow(projectId, type)

    suspend fun getObservationsByType(projectId: Long, type: RecordType): List<ObservationEntity> =
        observationDao.getObservationsByType(projectId, type)

    suspend fun getObservationById(id: Long): ObservationEntity? = observationDao.getObservationById(id)

    /**
     * Generates collision-free sequential display IDs:
     * OBS-<Initials>-001, OBS-<Initials>-002... for Normal Observations
     * NCR-<Initials>-001, NCR-<Initials>-002... for Non-Conformance Reports
     */
    suspend fun generateNextEntryNumber(projectId: Long, type: RecordType, initials: String = "SE"): String {
        val cleanInitials = initials.trim().uppercase().filter { it.isLetter() }.take(4).ifBlank { "SE" }
        val count = observationDao.getCountByInitials(projectId, type, cleanInitials)
        return String.format("%s-%s-%03d", type.prefix, cleanInitials, count + 1)
    }

    /**
     * Saves a new observation or NCR.
     * If NCR: Immediately generates formatted A4 PDF offline.
     * Enqueues record into persistent SyncOutbox queue.
     */
    suspend fun saveObservation(observation: ObservationEntity): Long {
        val rowId = observationDao.insertObservation(observation)
        val saved = observationDao.getObservationById(rowId) ?: observation.copy(id = rowId)

        var finalEntity = saved

        // If NCR, generate A4 PDF immediately offline
        if (saved.recordType == RecordType.NCR) {
            val project = projectDao.getProjectById(saved.projectId) ?: ProjectEntity(id = saved.projectId)
            val pdfPath = PdfGenerator.generateNcrPdf(context, project, saved)
            finalEntity = saved.copy(pdfPath = pdfPath)
            observationDao.updateObservation(finalEntity)

            // Enqueue PDF for Drive sync
            syncOutboxDao.insertQueueItem(
                SyncOutboxEntity(
                    projectId = finalEntity.projectId,
                    entityType = "NCR_PDF",
                    entityId = finalEntity.id,
                    action = "CREATE",
                    payload = gson.toJson(mapOf("entryNumber" to finalEntity.entryNumber, "vendor" to finalEntity.vendorName)),
                    filePath = pdfPath
                )
            )
        }

        // Enqueue observation record for Sheet sync
        syncOutboxDao.insertQueueItem(
            SyncOutboxEntity(
                projectId = finalEntity.projectId,
                entityType = "OBSERVATION",
                entityId = finalEntity.id,
                action = "CREATE",
                payload = gson.toJson(finalEntity),
                filePath = finalEntity.beforePhotoPath
            )
        )

        return rowId
    }

    /**
     * Closure flow: Marks observation/NCR as RECTIFIED, attaches closure photo and rectified date.
     * Enqueues update to sync queue.
     */
    suspend fun rectifyObservation(
        id: Long,
        rectifiedDate: Long,
        afterPhotoPath: String?
    ) {
        val existing = observationDao.getObservationById(id) ?: return
        val updated = existing.copy(
            status = Status.RECTIFIED,
            rectifiedDate = rectifiedDate,
            afterPhotoPath = afterPhotoPath,
            updatedAt = System.currentTimeMillis()
        )
        observationDao.updateObservation(updated)

        syncOutboxDao.insertQueueItem(
            SyncOutboxEntity(
                projectId = updated.projectId,
                entityType = "OBSERVATION",
                entityId = updated.id,
                action = "UPDATE_RECTIFIED",
                payload = gson.toJson(updated),
                filePath = afterPhotoPath
            )
        )
    }

    suspend fun deleteObservation(observation: ObservationEntity) {
        observationDao.deleteObservation(observation)
    }

    suspend fun getDashboardKpis(projectId: Long): DashboardKpis {
        val all = observationDao.getAllObservations(projectId)
        val normal = all.filter { it.recordType == RecordType.NORMAL_OBSERVATION }
        val ncrs = all.filter { it.recordType == RecordType.NCR }
        val now = System.currentTimeMillis()

        val totalObs = normal.size
        val openObs = normal.count { it.status == Status.OPEN }
        val rectifiedObs = normal.count { it.status == Status.RECTIFIED }
        val obsClosureRate = if (totalObs > 0) (rectifiedObs.toFloat() / totalObs.toFloat()) * 100f else 100f

        val totalNcrs = ncrs.size
        val openNcrs = ncrs.count { it.status == Status.OPEN }
        val overdueItems = all.count { it.status == Status.OPEN && it.targetDate < now }

        return DashboardKpis(
            totalObservations = totalObs,
            openObservations = openObs,
            rectifiedObservations = rectifiedObs,
            observationClosureRate = obsClosureRate,
            totalNcrs = totalNcrs,
            openNcrs = openNcrs,
            overdueItems = overdueItems
        )
    }
}

data class DashboardKpis(
    val totalObservations: Int = 0,
    val openObservations: Int = 0,
    val rectifiedObservations: Int = 0,
    val observationClosureRate: Float = 100f,
    val totalNcrs: Int = 0,
    val openNcrs: Int = 0,
    val overdueItems: Int = 0
)
