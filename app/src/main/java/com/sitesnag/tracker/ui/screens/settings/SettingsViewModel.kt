package com.sitesnag.tracker.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sitesnag.tracker.SiteSnagApplication
import com.sitesnag.tracker.data.local.entity.*
import com.sitesnag.tracker.domain.model.CustomFieldType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SettingsUiState(
    val project: ProjectEntity = ProjectEntity(),
    val allProjects: List<ProjectEntity> = emptyList(),
    val blocks: List<BlockEntity> = emptyList(),
    val selectedBlock: BlockEntity? = null,
    val floors: List<FloorEntity> = emptyList(),
    val selectedFloor: FloorEntity? = null,
    val subLocations: List<SubLocationEntity> = emptyList(),
    val vendors: List<VendorEntity> = emptyList(),
    val customFields: List<CustomFieldDefinitionEntity> = emptyList(),
    val saveMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SiteSnagApplication
    private val projectRepo = app.projectRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var projectDataJob: Job? = null

    init {
        observeProjects()
    }

    private fun observeProjects() {
        // Observe all projects
        viewModelScope.launch {
            projectRepo.getAllProjectsFlow().collect { projects ->
                _uiState.update { it.copy(allProjects = projects) }
            }
        }

        // Observe active project
        viewModelScope.launch {
            projectRepo.getActiveProjectFlow().collect { active ->
                val project = active ?: projectRepo.getActiveProject() ?: ProjectEntity()
                _uiState.update { it.copy(project = project) }
                loadProjectScopedData(project.id)
            }
        }
    }

    private fun loadProjectScopedData(projectId: Long) {
        projectDataJob?.cancel()
        projectDataJob = viewModelScope.launch {
            launch {
                projectRepo.getBlocksFlow(projectId).collect { blocks ->
                    _uiState.update { state ->
                        val selBlock = if (state.selectedBlock != null && blocks.any { it.id == state.selectedBlock.id }) {
                            state.selectedBlock
                        } else {
                            blocks.firstOrNull()
                        }
                        state.copy(blocks = blocks, selectedBlock = selBlock)
                    }
                    _uiState.value.selectedBlock?.let { loadFloors(it.id) }
                }
            }

            launch {
                projectRepo.getVendorsFlow(projectId).collect { vendors ->
                    _uiState.update { it.copy(vendors = vendors) }
                }
            }

            launch {
                projectRepo.getCustomFieldsFlow(projectId).collect { fields ->
                    _uiState.update { it.copy(customFields = fields) }
                }
            }
        }
    }

    // --- Multi-Project Management ---
    fun switchActiveProject(projectId: Long) {
        viewModelScope.launch {
            projectRepo.setActiveProject(projectId)
        }
    }

    fun saveProject(
        id: Long = 0L,
        name: String,
        code: String,
        clientContractor: String,
        spreadsheetId: String,
        defaultCc: String,
        frequency: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val project = ProjectEntity(
                id = id,
                projectName = name.trim(),
                projectCode = code.trim(),
                clientContractorName = clientContractor.trim(),
                spreadsheetId = spreadsheetId.trim(),
                defaultCcEmails = defaultCc.trim(),
                emailFrequency = frequency.trim(),
                isActive = (id == 0L && _uiState.value.allProjects.isEmpty()) || (id != 0L && id == _uiState.value.project.id),
                updatedAt = System.currentTimeMillis()
            )
            projectRepo.saveProject(project)
            _uiState.update { it.copy(saveMessage = "Project '${name.trim()}' saved.") }
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepo.deleteProject(project)
            _uiState.update { it.copy(saveMessage = "Project '${project.projectName}' deleted.") }
        }
    }

    // --- Project Metadata Update (from Settings screen) ---
    fun updateProjectMetadata(
        name: String,
        code: String,
        clientContractor: String,
        spreadsheetId: String,
        defaultCc: String,
        frequency: String
    ) {
        viewModelScope.launch {
            val updated = _uiState.value.project.copy(
                projectName = name.trim(),
                projectCode = code.trim(),
                clientContractorName = clientContractor.trim(),
                spreadsheetId = spreadsheetId.trim(),
                defaultCcEmails = defaultCc.trim(),
                emailFrequency = frequency.trim(),
                updatedAt = System.currentTimeMillis()
            )
            projectRepo.saveProject(updated)
            _uiState.update { it.copy(saveMessage = "Project settings updated successfully.") }
        }
    }

    // --- Blocks & Hierarchy ---
    fun selectBlock(block: BlockEntity) {
        _uiState.update { it.copy(selectedBlock = block, selectedFloor = null, subLocations = emptyList()) }
        loadFloors(block.id)
    }

    private fun loadFloors(blockId: Long) {
        viewModelScope.launch {
            val floors = projectRepo.getFloorsForBlock(blockId)
            val selFloor = floors.firstOrNull()
            _uiState.update { it.copy(floors = floors, selectedFloor = selFloor) }
            selFloor?.let { loadSubLocations(it.id) }
        }
    }

    fun selectFloor(floor: FloorEntity) {
        _uiState.update { it.copy(selectedFloor = floor) }
        loadSubLocations(floor.id)
    }

    private fun loadSubLocations(floorId: Long) {
        viewModelScope.launch {
            val subLocs = projectRepo.getSubLocationsForFloor(floorId)
            _uiState.update { it.copy(subLocations = subLocs) }
        }
    }

    fun addBlock(name: String) {
        if (name.isBlank()) return
        val projectId = _uiState.value.project.id
        viewModelScope.launch {
            val id = projectRepo.addBlock(projectId, name.trim())
            _uiState.update { it.copy(saveMessage = "Block '$name' added.") }
            loadFloors(id)
        }
    }

    fun deleteBlock(block: BlockEntity) {
        viewModelScope.launch {
            projectRepo.deleteBlock(block)
        }
    }

    fun addFloor(name: String) {
        val blockId = _uiState.value.selectedBlock?.id ?: return
        if (name.isBlank()) return
        val projectId = _uiState.value.project.id
        viewModelScope.launch {
            val id = projectRepo.addFloor(projectId, blockId, name.trim())
            _uiState.update { it.copy(saveMessage = "Floor '$name' added.") }
            loadSubLocations(id)
        }
    }

    fun deleteFloor(floor: FloorEntity) {
        viewModelScope.launch {
            projectRepo.deleteFloor(floor)
            _uiState.value.selectedBlock?.let { loadFloors(it.id) }
        }
    }

    fun addSubLocation(name: String) {
        val floorId = _uiState.value.selectedFloor?.id ?: return
        if (name.isBlank()) return
        val projectId = _uiState.value.project.id
        viewModelScope.launch {
            projectRepo.addSubLocation(projectId, floorId, name.trim())
            _uiState.update { it.copy(saveMessage = "Sub-location '$name' added.") }
            loadSubLocations(floorId)
        }
    }

    fun deleteSubLocation(subLocation: SubLocationEntity) {
        viewModelScope.launch {
            projectRepo.deleteSubLocation(subLocation)
            _uiState.value.selectedFloor?.let { loadSubLocations(it.id) }
        }
    }

    // --- Vendors ---
    fun saveVendor(
        id: Long = 0L,
        name: String,
        trade: String,
        email: String,
        cc: String
    ) {
        if (name.isBlank() || email.isBlank()) return
        val projectId = _uiState.value.project.id
        viewModelScope.launch {
            val vendor = VendorEntity(
                id = id,
                projectId = projectId,
                name = name.trim(),
                trade = trade.trim(),
                primaryEmail = email.trim(),
                ccEmails = cc.trim()
            )
            if (id == 0L) {
                projectRepo.addVendor(vendor)
                _uiState.update { it.copy(saveMessage = "Vendor '$name' registered.") }
            } else {
                projectRepo.updateVendor(vendor)
                _uiState.update { it.copy(saveMessage = "Vendor '$name' updated.") }
            }
        }
    }

    fun deleteVendor(vendor: VendorEntity) {
        viewModelScope.launch {
            projectRepo.deleteVendor(vendor)
        }
    }

    // --- Custom Fields ---
    fun saveCustomField(
        id: String? = null,
        name: String,
        type: CustomFieldType,
        options: List<String>,
        isRequired: Boolean
    ) {
        if (name.isBlank()) return
        val projectId = _uiState.value.project.id
        viewModelScope.launch {
            val fieldId = id ?: "custom_${UUID.randomUUID().toString().take(8)}"
            val optionsJson = Gson().toJson(options)
            val field = CustomFieldDefinitionEntity(
                id = fieldId,
                projectId = projectId,
                name = name.trim(),
                type = type,
                optionsJson = optionsJson,
                isRequired = isRequired,
                orderIndex = _uiState.value.customFields.size + 1
            )
            projectRepo.addCustomField(field)
            _uiState.update { it.copy(saveMessage = "Custom field '$name' saved.") }
        }
    }

    fun deleteCustomField(field: CustomFieldDefinitionEntity) {
        viewModelScope.launch {
            projectRepo.deleteCustomField(field)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }
}
