package com.sitesnag.tracker.ui.screens.capture

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sitesnag.tracker.SiteSnagApplication
import com.sitesnag.tracker.data.local.EngineerProfileManager
import com.sitesnag.tracker.data.local.entity.*
import com.sitesnag.tracker.domain.model.Priority
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import com.sitesnag.tracker.util.ImageCompressor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.UUID

data class CaptureUiState(
    val activeProject: ProjectEntity = ProjectEntity(),
    val recordType: RecordType = RecordType.NORMAL_OBSERVATION,
    val blocks: List<BlockEntity> = emptyList(),
    val selectedBlock: BlockEntity? = null,
    val floors: List<FloorEntity> = emptyList(),
    val selectedFloor: FloorEntity? = null,
    val subLocations: List<SubLocationEntity> = emptyList(),
    val specificLocationText: String = "",
    val vendors: List<VendorEntity> = emptyList(),
    val selectedVendor: VendorEntity? = null,
    val tradeText: String = "",
    val priority: Priority = Priority.MEDIUM,
    val targetDateMillis: Long = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis,
    val issueDescription: String = "",
    val rootCause: String = "",
    val correctiveAction: String = "",
    val photoPath: String? = null,
    val customFields: List<CustomFieldDefinitionEntity> = emptyList(),
    val customFieldValues: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SiteSnagApplication
    private val projectRepo = app.projectRepository
    private val observationRepo = app.observationRepository

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var projectDataJob: Job? = null

    init {
        observeActiveProject()
    }

    private fun observeActiveProject() {
        viewModelScope.launch {
            projectRepo.getActiveProjectFlow().collect { active ->
                val project = active ?: projectRepo.getActiveProject() ?: ProjectEntity()
                _uiState.update { it.copy(activeProject = project) }
                loadProjectScopedMasterData(project.id)
            }
        }
    }

    private fun loadProjectScopedMasterData(projectId: Long) {
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
                    _uiState.value.selectedBlock?.let { loadFloorsForBlock(it.id) }
                }
            }

            launch {
                projectRepo.getVendorsFlow(projectId).collect { vendors ->
                    _uiState.update { state ->
                        val selVendor = if (state.selectedVendor != null && vendors.any { it.id == state.selectedVendor.id }) {
                            state.selectedVendor
                        } else {
                            vendors.firstOrNull()
                        }
                        state.copy(
                            vendors = vendors,
                            selectedVendor = selVendor,
                            tradeText = selVendor?.trade ?: state.tradeText
                        )
                    }
                }
            }

            launch {
                projectRepo.getCustomFieldsFlow(projectId).collect { fields ->
                    _uiState.update { it.copy(customFields = fields) }
                }
            }
        }
    }

    fun onRecordTypeChanged(type: RecordType) {
        _uiState.update { it.copy(recordType = type) }
    }

    fun onBlockSelected(block: BlockEntity) {
        _uiState.update { it.copy(selectedBlock = block, selectedFloor = null, subLocations = emptyList()) }
        loadFloorsForBlock(block.id)
    }

    private fun loadFloorsForBlock(blockId: Long) {
        viewModelScope.launch {
            val floors = projectRepo.getFloorsForBlock(blockId)
            val selFloor = floors.firstOrNull()
            _uiState.update { it.copy(floors = floors, selectedFloor = selFloor) }
            selFloor?.let { loadSubLocationsForFloor(it.id) }
        }
    }

    fun onFloorSelected(floor: FloorEntity) {
        _uiState.update { it.copy(selectedFloor = floor) }
        loadSubLocationsForFloor(floor.id)
    }

    private fun loadSubLocationsForFloor(floorId: Long) {
        viewModelScope.launch {
            val subLocs = projectRepo.getSubLocationsForFloor(floorId)
            _uiState.update { it.copy(subLocations = subLocs) }
        }
    }

    fun onSubLocationPresetSelected(subLocation: SubLocationEntity) {
        _uiState.update { it.copy(specificLocationText = subLocation.name) }
    }

    fun onSpecificLocationTextChanged(text: String) {
        _uiState.update { it.copy(specificLocationText = text) }
    }

    fun onVendorSelected(vendor: VendorEntity) {
        _uiState.update { it.copy(selectedVendor = vendor, tradeText = vendor.trade) }
    }

    fun onTradeChanged(trade: String) {
        _uiState.update { it.copy(tradeText = trade) }
    }

    fun onPrioritySelected(priority: Priority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onTargetDateSelected(millis: Long) {
        _uiState.update { it.copy(targetDateMillis = millis) }
    }

    fun onIssueDescriptionChanged(desc: String) {
        _uiState.update { it.copy(issueDescription = desc) }
    }

    fun onRootCauseChanged(text: String) {
        _uiState.update { it.copy(rootCause = text) }
    }

    fun onCorrectiveActionChanged(text: String) {
        _uiState.update { it.copy(correctiveAction = text) }
    }

    fun onCustomFieldValueChanged(fieldId: String, value: String) {
        _uiState.update { state ->
            val updated = state.customFieldValues.toMutableMap()
            updated[fieldId] = value
            state.copy(customFieldValues = updated)
        }
    }

    fun onPhotoCaptured(uri: Uri) {
        viewModelScope.launch {
            try {
                val compressedPath = ImageCompressor.compressAndSaveImage(
                    getApplication(),
                    uri,
                    fileNamePrefix = if (_uiState.value.recordType == RecordType.NCR) "ncr" else "snag"
                )
                _uiState.update { it.copy(photoPath = compressedPath) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Photo processing failed: ${e.message}") }
            }
        }
    }

    fun onRemovePhoto() {
        _uiState.value.photoPath?.let { File(it).delete() }
        _uiState.update { it.copy(photoPath = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(saveSuccessMessage = null, errorMessage = null) }
    }

    fun submitRecord() {
        val state = _uiState.value

        // Validate basic fields
        if (state.issueDescription.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an issue description.") }
            return
        }
        val blockName = state.selectedBlock?.name ?: "General Site"
        val floorName = state.selectedFloor?.name ?: "Ground"
        val vendorName = state.selectedVendor?.name ?: "Unassigned Vendor"
        val projectId = state.activeProject.id

        // Validate mandatory custom fields
        for (field in state.customFields) {
            if (field.isRequired && state.customFieldValues[field.id].isNullOrBlank()) {
                _uiState.update { it.copy(errorMessage = "Mandatory custom field required: ${field.name}") }
                return
            }
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val profile = EngineerProfileManager.getInstance(getApplication()).getProfile()
                val nextEntryId = observationRepo.generateNextEntryNumber(projectId, state.recordType, profile.initials)

                // Map custom field names to values for JSON
                val customMap = mutableMapOf<String, String>()
                for (field in state.customFields) {
                    val value = state.customFieldValues[field.id] ?: ""
                    customMap[field.name] = value
                }
                val customJson = Gson().toJson(customMap)

                val observation = ObservationEntity(
                    recordUuid = UUID.randomUUID().toString(),
                    projectId = projectId,
                    entryNumber = nextEntryId,
                    recordType = state.recordType,
                    dateLogged = System.currentTimeMillis(),
                    loggedBy = profile.displayTag,
                    loggedByInitials = profile.initials,
                    block = blockName,
                    floor = floorName,
                    specificLocation = state.specificLocationText.ifBlank { "General Area" },
                    vendorId = state.selectedVendor?.id,
                    vendorName = vendorName,
                    vendorEmail = state.selectedVendor?.primaryEmail ?: "",
                    vendorCc = state.selectedVendor?.ccEmails ?: "",
                    trade = state.tradeText.ifBlank { state.selectedVendor?.trade ?: "General" },
                    priority = state.priority,
                    targetDate = state.targetDateMillis,
                    issueDescription = state.issueDescription.trim(),
                    rootCause = if (state.recordType == RecordType.NCR) state.rootCause.trim() else null,
                    correctiveAction = if (state.recordType == RecordType.NCR) state.correctiveAction.trim() else null,
                    customFieldsJson = customJson,
                    status = Status.OPEN,
                    beforePhotoPath = state.photoPath
                )

                observationRepo.saveObservation(observation)

                val successMsg = if (state.recordType == RecordType.NCR) {
                    "Successfully logged $nextEntryId & generated A4 PDF offline!"
                } else {
                    "Successfully logged observation $nextEntryId!"
                }

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccessMessage = successMsg,
                        issueDescription = "",
                        rootCause = "",
                        correctiveAction = "",
                        photoPath = null,
                        customFieldValues = emptyMap(),
                        specificLocationText = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}") }
            }
        }
    }
}
