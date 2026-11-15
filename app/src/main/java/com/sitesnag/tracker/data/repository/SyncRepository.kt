package com.sitesnag.tracker.data.repository

import com.sitesnag.tracker.data.local.dao.SyncOutboxDao
import com.sitesnag.tracker.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

class SyncRepository(
    private val syncOutboxDao: SyncOutboxDao
) {
    fun getPendingQueueFlow(projectId: Long): Flow<List<SyncOutboxEntity>> =
        syncOutboxDao.getPendingItemsFlow(projectId)

    fun getAllPendingQueueFlow(): Flow<List<SyncOutboxEntity>> =
        syncOutboxDao.getAllPendingItemsFlow()

    suspend fun getPendingQueue(projectId: Long? = null): List<SyncOutboxEntity> {
        return if (projectId != null) {
            syncOutboxDao.getPendingItems(projectId)
        } else {
            syncOutboxDao.getAllPendingItems()
        }
    }

    suspend fun enqueueItem(
        projectId: Long,
        entityType: String,
        entityId: Long,
        action: String,
        payload: String,
        filePath: String? = null
    ): Long {
        return syncOutboxDao.insertQueueItem(
            SyncOutboxEntity(
                projectId = projectId,
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = payload,
                filePath = filePath,
                status = "PENDING"
            )
        )
    }

    suspend fun markSynced(id: Long) {
        syncOutboxDao.updateStatus(id, "SYNCED", System.currentTimeMillis())
    }

    suspend fun markFailed(id: Long) {
        syncOutboxDao.updateStatus(id, "FAILED", System.currentTimeMillis())
    }

    suspend fun purgeSynced() {
        syncOutboxDao.purgeSynced()
    }
}
