package com.sitesnag.tracker.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sitesnag.tracker.SiteSnagApplication
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.data.local.entity.ProjectEntity
import com.sitesnag.tracker.data.repository.DashboardKpis
import com.sitesnag.tracker.sync.SyncManager
import com.sitesnag.tracker.util.ExcelExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val project: ProjectEntity = ProjectEntity(),
    val allProjects: List<ProjectEntity> = emptyList(),
    val kpis: DashboardKpis = DashboardKpis(),
    val allObservations: List<ObservationEntity> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isExportingExcel: Boolean = false,
    val isSyncing: Boolean = false,
    val exportedExcelPath: String? = null,
    val message: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SiteSnagApplication
    private val projectRepo = app.projectRepository
    private val observationRepo = app.observationRepository
    private val syncRepo = app.syncRepository

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var projectDataJob: Job? = null

    init {
        observeProjects()
    }

    private fun observeProjects() {
        // Observe all projects for switcher
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
                observationRepo.getAllObservationsFlow(projectId).collect { list ->
                    val kpis = observationRepo.getDashboardKpis(projectId)
                    _uiState.update { it.copy(allObservations = list, kpis = kpis) }
                }
            }
            launch {
                syncRepo.getPendingQueueFlow(projectId).collect { pending ->
                    _uiState.update { it.copy(pendingSyncCount = pending.size) }
                }
            }
        }
    }

    fun switchActiveProject(projectId: Long) {
        viewModelScope.launch {
            projectRepo.setActiveProject(projectId)
        }
    }

    fun triggerManualSync() {
        _uiState.update { it.copy(isSyncing = true, message = "Background sync initiated...") }
        SyncManager.triggerManualSync(getApplication())
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(isSyncing = false, message = "Sync queued to WorkManager.") }
        }
    }

    fun exportExcelWorkbook(onSuccess: (String) -> Unit) {
        _uiState.update { it.copy(isExportingExcel = true) }
        viewModelScope.launch {
            try {
                val currentProject = _uiState.value.project
                val customFields = projectRepo.getCustomFields(currentProject.id).map { it.name }
                val path = ExcelExporter.generateMultiTabWorkbook(
                    context = getApplication(),
                    project = currentProject,
                    observations = _uiState.value.allObservations,
                    customFieldHeaders = customFields
                )
                _uiState.update { it.copy(isExportingExcel = false, exportedExcelPath = path) }
                onSuccess(path)
            } catch (e: Exception) {
                _uiState.update { it.copy(isExportingExcel = false, message = "Excel export failed: ${e.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, exportedExcelPath = null) }
    }
}
