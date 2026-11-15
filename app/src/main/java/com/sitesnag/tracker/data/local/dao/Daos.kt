package com.sitesnag.tracker.data.local.dao

import androidx.room.*
import com.sitesnag.tracker.data.local.entity.*
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY isActive DESC, projectName ASC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY isActive DESC, projectName ASC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    fun getActiveProjectFlow(): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProject(): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectByIdFlow(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    suspend fun getFirstActiveProject(): ProjectEntity?

    @Query("UPDATE projects SET isActive = CASE WHEN id = :projectId THEN 1 ELSE 0 END")
    suspend fun setActiveProject(projectId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface LocationDao {
    // Blocks
    @Query("SELECT * FROM blocks WHERE projectId = :projectId ORDER BY name ASC")
    fun getAllBlocksFlow(projectId: Long): Flow<List<BlockEntity>>

    @Query("SELECT * FROM blocks WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getAllBlocks(projectId: Long): List<BlockEntity>

    @Query("SELECT * FROM blocks WHERE recordUuid = :recordUuid LIMIT 1")
    suspend fun getBlockByUuid(recordUuid: String): BlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockEntity): Long

    @Update
    suspend fun updateBlock(block: BlockEntity)

    @Delete
    suspend fun deleteBlock(block: BlockEntity)

    // Floors
    @Query("SELECT * FROM floors WHERE blockId = :blockId ORDER BY name ASC")
    fun getFloorsForBlockFlow(blockId: Long): Flow<List<FloorEntity>>

    @Query("SELECT * FROM floors WHERE blockId = :blockId ORDER BY name ASC")
    suspend fun getFloorsForBlock(blockId: Long): List<FloorEntity>

    @Query("SELECT * FROM floors WHERE projectId = :projectId ORDER BY name ASC")
    fun getFloorsForProjectFlow(projectId: Long): Flow<List<FloorEntity>>

    @Query("SELECT * FROM floors WHERE recordUuid = :recordUuid LIMIT 1")
    suspend fun getFloorByUuid(recordUuid: String): FloorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloor(floor: FloorEntity): Long

    @Update
    suspend fun updateFloor(floor: FloorEntity)

    @Delete
    suspend fun deleteFloor(floor: FloorEntity)

    // SubLocations
    @Query("SELECT * FROM sub_locations WHERE floorId = :floorId ORDER BY name ASC")
    fun getSubLocationsForFloorFlow(floorId: Long): Flow<List<SubLocationEntity>>

    @Query("SELECT * FROM sub_locations WHERE floorId = :floorId ORDER BY name ASC")
    suspend fun getSubLocationsForFloor(floorId: Long): List<SubLocationEntity>

    @Query("SELECT * FROM sub_locations WHERE recordUuid = :recordUuid LIMIT 1")
    suspend fun getSubLocationByUuid(recordUuid: String): SubLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubLocation(subLocation: SubLocationEntity): Long

    @Delete
    suspend fun deleteSubLocation(subLocation: SubLocationEntity)
}

@Dao
interface VendorDao {
    @Query("SELECT * FROM vendors WHERE projectId = :projectId ORDER BY name ASC")
    fun getAllVendorsFlow(projectId: Long): Flow<List<VendorEntity>>

    @Query("SELECT * FROM vendors WHERE projectId = :projectId ORDER BY name ASC")
    suspend fun getAllVendors(projectId: Long): List<VendorEntity>

    @Query("SELECT * FROM vendors WHERE id = :id LIMIT 1")
    suspend fun getVendorById(id: Long): VendorEntity?

    @Query("SELECT * FROM vendors WHERE recordUuid = :recordUuid LIMIT 1")
    suspend fun getVendorByUuid(recordUuid: String): VendorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendor(vendor: VendorEntity): Long

    @Update
    suspend fun updateVendor(vendor: VendorEntity)

    @Delete
    suspend fun deleteVendor(vendor: VendorEntity)
}

@Dao
interface CustomFieldDao {
    @Query("SELECT * FROM custom_field_definitions WHERE projectId = :projectId ORDER BY orderIndex ASC, name ASC")
    fun getAllFieldsFlow(projectId: Long): Flow<List<CustomFieldDefinitionEntity>>

    @Query("SELECT * FROM custom_field_definitions WHERE projectId = :projectId ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllFields(projectId: Long): List<CustomFieldDefinitionEntity>

    @Query("SELECT * FROM custom_field_definitions WHERE recordUuid = :recordUuid LIMIT 1")
    suspend fun getFieldByUuid(recordUuid: String): CustomFieldDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: CustomFieldDefinitionEntity)

    @Update
    suspend fun updateField(field: CustomFieldDefinitionEntity)

    @Delete
    suspend fun deleteField(field: CustomFieldDefinitionEntity)
}

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations WHERE projectId = :projectId ORDER BY dateLogged DESC")
    fun getAllObservationsFlow(projectId: Long): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE projectId = :projectId ORDER BY dateLogged DESC")
    suspend fun getAllObservations(projectId: Long): List<ObservationEntity>

    @Query("SELECT * FROM observations WHERE projectId = :projectId AND recordType = :type ORDER BY dateLogged DESC")
    fun getObservationsByTypeFlow(projectId: Long, type: RecordType): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE projectId = :projectId AND recordType = :type ORDER BY dateLogged DESC")
    suspend fun getObservationsByType(projectId: Long, type: RecordType): List<ObservationEntity>

    @Query("SELECT * FROM observations WHERE id = :id LIMIT 1")
    suspend fun getObservationById(id: Long): ObservationEntity?

    @Query("SELECT * FROM observations WHERE recordUuid = :recordUuid LIMIT 1")
    suspend fun getObservationByUuid(recordUuid: String): ObservationEntity?

    @Query("SELECT * FROM observations WHERE projectId = :projectId AND entryNumber = :entryNumber LIMIT 1")
    suspend fun getObservationByEntryNumber(projectId: Long, entryNumber: String): ObservationEntity?

    @Query("SELECT COUNT(*) FROM observations WHERE projectId = :projectId AND recordType = :type")
    suspend fun getCountByRecordType(projectId: Long, type: RecordType): Int

    @Query("SELECT COUNT(*) FROM observations WHERE projectId = :projectId AND recordType = :type AND loggedByInitials = :initials")
    suspend fun getCountByInitials(projectId: Long, type: RecordType, initials: String): Int

    @Query("SELECT COUNT(*) FROM observations WHERE projectId = :projectId AND recordType = :type AND status = :status")
    suspend fun getCountByRecordTypeAndStatus(projectId: Long, type: RecordType, status: Status): Int

    @Query("SELECT COUNT(*) FROM observations WHERE projectId = :projectId AND status = 'OPEN' AND targetDate < :currentTime")
    suspend fun getOverdueCount(projectId: Long, currentTime: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ObservationEntity): Long

    @Update
    suspend fun updateObservation(observation: ObservationEntity)

    @Delete
    suspend fun deleteObservation(observation: ObservationEntity)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox WHERE projectId = :projectId AND status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingItemsFlow(projectId: Long): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE projectId = :projectId AND status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItems(projectId: Long): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getAllPendingItemsFlow(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT * FROM sync_outbox WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getAllPendingItems(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: SyncOutboxEntity): Long

    @Update
    suspend fun updateQueueItem(item: SyncOutboxEntity)

    @Query("UPDATE sync_outbox SET status = :status, lastAttempt = :timestamp, attemptCount = attemptCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, timestamp: Long)

    @Query("DELETE FROM sync_outbox WHERE status = 'SYNCED'")
    suspend fun purgeSynced()
}
