package com.sitesnag.tracker.data.repository

import com.sitesnag.tracker.data.local.dao.CustomFieldDao
import com.sitesnag.tracker.data.local.dao.LocationDao
import com.sitesnag.tracker.data.local.dao.ProjectDao
import com.sitesnag.tracker.data.local.dao.VendorDao
import com.sitesnag.tracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val locationDao: LocationDao,
    private val vendorDao: VendorDao,
    private val customFieldDao: CustomFieldDao
) {
    // Projects
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>> = projectDao.getAllProjectsFlow()
    suspend fun getAllProjects(): List<ProjectEntity> = projectDao.getAllProjects()
    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    fun getActiveProjectFlow(): Flow<ProjectEntity?> = projectDao.getActiveProjectFlow()

    suspend fun getActiveProject(): ProjectEntity? {
        val active = projectDao.getActiveProject()
        if (active != null) return active
        val first = projectDao.getAllProjects().firstOrNull()
        if (first != null) {
            projectDao.setActiveProject(first.id)
            return first
        }
        return null
    }

    suspend fun setActiveProject(projectId: Long) {
        projectDao.setActiveProject(projectId)
    }

    suspend fun saveProject(project: ProjectEntity): Long {
        val id = projectDao.insertProject(project)
        val all = projectDao.getAllProjects()
        if (all.size == 1 || project.isActive) {
            projectDao.setActiveProject(if (id > 0) id else project.id)
        }
        return id
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.deleteProject(project)
        // If deleted project was active, switch to first available project
        val active = projectDao.getActiveProject()
        if (active == null) {
            val remaining = projectDao.getAllProjects().firstOrNull()
            if (remaining != null) {
                projectDao.setActiveProject(remaining.id)
            }
        }
    }

    // Blocks
    fun getBlocksFlow(projectId: Long): Flow<List<BlockEntity>> = locationDao.getAllBlocksFlow(projectId)
    suspend fun getBlocks(projectId: Long): List<BlockEntity> = locationDao.getAllBlocks(projectId)
    suspend fun addBlock(projectId: Long, name: String): Long =
        locationDao.insertBlock(BlockEntity(projectId = projectId, name = name))
    suspend fun updateBlock(block: BlockEntity) = locationDao.updateBlock(block)
    suspend fun deleteBlock(block: BlockEntity) = locationDao.deleteBlock(block)

    // Floors
    fun getFloorsForBlockFlow(blockId: Long): Flow<List<FloorEntity>> = locationDao.getFloorsForBlockFlow(blockId)
    suspend fun getFloorsForBlock(blockId: Long): List<FloorEntity> = locationDao.getFloorsForBlock(blockId)
    suspend fun addFloor(projectId: Long, blockId: Long, name: String): Long =
        locationDao.insertFloor(FloorEntity(projectId = projectId, blockId = blockId, name = name))
    suspend fun updateFloor(floor: FloorEntity) = locationDao.updateFloor(floor)
    suspend fun deleteFloor(floor: FloorEntity) = locationDao.deleteFloor(floor)

    // Sub-locations
    fun getSubLocationsForFloorFlow(floorId: Long): Flow<List<SubLocationEntity>> =
        locationDao.getSubLocationsForFloorFlow(floorId)
    suspend fun getSubLocationsForFloor(floorId: Long): List<SubLocationEntity> =
        locationDao.getSubLocationsForFloor(floorId)
    suspend fun addSubLocation(projectId: Long, floorId: Long, name: String): Long =
        locationDao.insertSubLocation(SubLocationEntity(projectId = projectId, floorId = floorId, name = name))
    suspend fun deleteSubLocation(subLocation: SubLocationEntity) = locationDao.deleteSubLocation(subLocation)

    // Vendors
    fun getVendorsFlow(projectId: Long): Flow<List<VendorEntity>> = vendorDao.getAllVendorsFlow(projectId)
    suspend fun getVendors(projectId: Long): List<VendorEntity> = vendorDao.getAllVendors(projectId)
    suspend fun getVendorById(id: Long): VendorEntity? = vendorDao.getVendorById(id)
    suspend fun addVendor(vendor: VendorEntity): Long = vendorDao.insertVendor(vendor)
    suspend fun updateVendor(vendor: VendorEntity) = vendorDao.updateVendor(vendor)
    suspend fun deleteVendor(vendor: VendorEntity) = vendorDao.deleteVendor(vendor)

    // Custom Fields
    fun getCustomFieldsFlow(projectId: Long): Flow<List<CustomFieldDefinitionEntity>> =
        customFieldDao.getAllFieldsFlow(projectId)
    suspend fun getCustomFields(projectId: Long): List<CustomFieldDefinitionEntity> =
        customFieldDao.getAllFields(projectId)
    suspend fun addCustomField(field: CustomFieldDefinitionEntity) = customFieldDao.insertField(field)
    suspend fun updateCustomField(field: CustomFieldDefinitionEntity) = customFieldDao.updateField(field)
    suspend fun deleteCustomField(field: CustomFieldDefinitionEntity) = customFieldDao.deleteField(field)
}
