package com.sitesnag.tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sitesnag.tracker.domain.model.CustomFieldType
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectName: String = "SnagOps Project",
    val projectCode: String = "PRJ-001",
    val clientContractorName: String = "General Contractor / Client",
    val spreadsheetId: String = "",
    val defaultCcEmails: String = "",
    val emailFrequency: String = "Daily", // Daily, Alternate Days, Weekly
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "blocks",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["recordUuid"], unique = true)
    ]
)
data class BlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val name: String
)

@Entity(
    tableName = "floors",
    foreignKeys = [
        ForeignKey(
            entity = BlockEntity::class,
            parentColumns = ["id"],
            childColumns = ["blockId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["blockId"]),
        Index(value = ["projectId"]),
        Index(value = ["recordUuid"], unique = true)
    ]
)
data class FloorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val blockId: Long,
    val name: String
)

@Entity(
    tableName = "sub_locations",
    foreignKeys = [
        ForeignKey(
            entity = FloorEntity::class,
            parentColumns = ["id"],
            childColumns = ["floorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["floorId"]),
        Index(value = ["projectId"]),
        Index(value = ["recordUuid"], unique = true)
    ]
)
data class SubLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val floorId: Long,
    val name: String
)

@Entity(
    tableName = "vendors",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["recordUuid"], unique = true)
    ]
)
data class VendorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val name: String,
    val trade: String,
    val primaryEmail: String,
    val ccEmails: String = ""
)

@Entity(
    tableName = "custom_field_definitions",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["recordUuid"], unique = true)
    ]
)
data class CustomFieldDefinitionEntity(
    @PrimaryKey val id: String,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val name: String,
    val type: CustomFieldType,
    val optionsJson: String = "[]",
    val isRequired: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "observations",
    indices = [
        Index(value = ["recordUuid"], unique = true),
        Index(value = ["projectId", "entryNumber"]),
        Index(value = ["projectId"]),
        Index(value = ["recordType"]),
        Index(value = ["status"]),
        Index(value = ["vendorName"]),
        Index(value = ["block"])
    ]
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val entryNumber: String, // e.g. OBS-RK-001 or NCR-AS-001
    val recordType: RecordType,
    val dateLogged: Long = System.currentTimeMillis(),
    val loggedBy: String = "Site Engineer (SE)",
    val loggedByInitials: String = "SE",
    val block: String,
    val floor: String,
    val specificLocation: String,
    val vendorId: Long? = null,
    val vendorName: String,
    val vendorEmail: String = "",
    val vendorCc: String = "",
    val trade: String,
    val priority: Priority,
    val targetDate: Long,
    val issueDescription: String,
    val rootCause: String? = null,
    val correctiveAction: String? = null,
    val customFieldsJson: String = "{}", // Map<String, String> as JSON
    val status: Status = Status.OPEN,
    val rectifiedDate: Long? = null,
    val beforePhotoPath: String? = null,
    val afterPhotoPath: String? = null,
    val pdfPath: String? = null,
    val remotePhotoUrl: String? = null,
    val remotePdfUrl: String? = null,
    val synced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sync_outbox",
    indices = [Index(value = ["projectId"]), Index(value = ["status"])]
)
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordUuid: String = UUID.randomUUID().toString(),
    val projectId: Long = 1L,
    val entityType: String, // OBSERVATION, NCR_PDF, PHOTO
    val entityId: Long,
    val action: String, // CREATE, UPDATE_RECTIFIED
    val payload: String, // JSON representation
    val filePath: String? = null,
    val attemptCount: Int = 0,
    val lastAttempt: Long = 0L,
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)
