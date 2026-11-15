package com.sitesnag.tracker.ui.screens.observations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sitesnag.tracker.SiteSnagApplication
import com.sitesnag.tracker.data.local.entity.ObservationEntity
import com.sitesnag.tracker.domain.model.RecordType
import com.sitesnag.tracker.domain.model.Status
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ObservationListUiState(
    val observations: List<ObservationEntity> = emptyList(),
    val filteredObservations: List<ObservationEntity> = emptyList(),
    val statusFilter: Status? = null, // null means All
    val searchQuery: String = "",
    val selectedObservationForRectify: ObservationEntity? = null
)

class ObservationListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SiteSnagApplication
    private val projectRepo = app.projectRepository
    private val observationRepo = app.observationRepository

    private val _uiState = MutableStateFlow(ObservationListUiState())
    val uiState: StateFlow<ObservationListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeActiveProject()
    }

    private fun observeActiveProject() {
        viewModelScope.launch {
            projectRepo.getActiveProjectFlow().collect { active ->
                val projectId = active?.id ?: projectRepo.getActiveProject()?.id ?: 1L
                loadObservations(projectId)
            }
        }
    }

    private fun loadObservations(projectId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            observationRepo.getObservationsByTypeFlow(projectId, RecordType.NORMAL_OBSERVATION).collect { list ->
                _uiState.update { state ->
                    state.copy(
                        observations = list,
                        filteredObservations = applyFilters(list, state.statusFilter, state.searchQuery)
                    )
                }
            }
        }
    }

    fun onStatusFilterChanged(status: Status?) {
        _uiState.update { state ->
            state.copy(
                statusFilter = status,
                filteredObservations = applyFilters(state.observations, status, state.searchQuery)
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredObservations = applyFilters(state.observations, state.statusFilter, query)
            )
        }
    }

    fun onOpenRectifyDialog(observation: ObservationEntity) {
        _uiState.update { it.copy(selectedObservationForRectify = observation) }
    }

    fun onDismissRectifyDialog() {
        _uiState.update { it.copy(selectedObservationForRectify = null) }
    }

    fun onConfirmRectification(rectifiedDate: Long, afterPhotoPath: String?) {
        val obs = _uiState.value.selectedObservationForRectify ?: return
        confirmRectification(obs.id, rectifiedDate, afterPhotoPath)
    }

    fun confirmRectification(id: Long, rectifiedDate: Long, afterPhotoPath: String?) {
        viewModelScope.launch {
            observationRepo.rectifyObservation(id, rectifiedDate, afterPhotoPath)
            _uiState.update { it.copy(selectedObservationForRectify = null) }
        }
    }

    fun onDeleteObservation(observation: ObservationEntity) {
        viewModelScope.launch {
            observationRepo.deleteObservation(observation)
        }
    }

    private fun applyFilters(
        list: List<ObservationEntity>,
        status: Status?,
        query: String
    ): List<ObservationEntity> {
        return list.filter { obs ->
            val matchesStatus = status == null || obs.status == status
            val matchesQuery = query.isBlank() ||
                    obs.entryNumber.contains(query, ignoreCase = true) ||
                    obs.issueDescription.contains(query, ignoreCase = true) ||
                    obs.vendorName.contains(query, ignoreCase = true) ||
                    obs.block.contains(query, ignoreCase = true) ||
                    obs.floor.contains(query, ignoreCase = true) ||
                    obs.specificLocation.contains(query, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }
}
