package com.sitesnag.tracker.ui.screens.ncr

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

data class NcrListUiState(
    val ncrs: List<ObservationEntity> = emptyList(),
    val filteredNcrs: List<ObservationEntity> = emptyList(),
    val statusFilter: Status? = null,
    val searchQuery: String = "",
    val selectedNcrForRectify: ObservationEntity? = null
)

class NcrListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SiteSnagApplication
    private val projectRepo = app.projectRepository
    private val observationRepo = app.observationRepository

    private val _uiState = MutableStateFlow(NcrListUiState())
    val uiState: StateFlow<NcrListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeActiveProject()
    }

    private fun observeActiveProject() {
        viewModelScope.launch {
            projectRepo.getActiveProjectFlow().collect { active ->
                val projectId = active?.id ?: projectRepo.getActiveProject()?.id ?: 1L
                loadNcrs(projectId)
            }
        }
    }

    private fun loadNcrs(projectId: Long) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            observationRepo.getObservationsByTypeFlow(projectId, RecordType.NCR).collect { list ->
                _uiState.update { state ->
                    state.copy(
                        ncrs = list,
                        filteredNcrs = applyFilters(list, state.statusFilter, state.searchQuery)
                    )
                }
            }
        }
    }

    fun onStatusFilterChanged(status: Status?) {
        _uiState.update { state ->
            state.copy(
                statusFilter = status,
                filteredNcrs = applyFilters(state.ncrs, status, state.searchQuery)
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredNcrs = applyFilters(state.ncrs, state.statusFilter, query)
            )
        }
    }

    fun onOpenRectifyDialog(ncr: ObservationEntity) {
        _uiState.update { it.copy(selectedNcrForRectify = ncr) }
    }

    fun onDismissRectifyDialog() {
        _uiState.update { it.copy(selectedNcrForRectify = null) }
    }

    fun onConfirmRectification(rectifiedDate: Long, afterPhotoPath: String?) {
        val ncr = _uiState.value.selectedNcrForRectify ?: return
        confirmRectification(ncr.id, rectifiedDate, afterPhotoPath)
    }

    fun confirmRectification(id: Long, rectifiedDate: Long, afterPhotoPath: String?) {
        viewModelScope.launch {
            observationRepo.rectifyObservation(id, rectifiedDate, afterPhotoPath)
            _uiState.update { it.copy(selectedNcrForRectify = null) }
        }
    }

    fun onDeleteNcr(ncr: ObservationEntity) {
        viewModelScope.launch {
            observationRepo.deleteObservation(ncr)
        }
    }

    private fun applyFilters(
        list: List<ObservationEntity>,
        status: Status?,
        query: String
    ): List<ObservationEntity> {
        return list.filter { ncr ->
            val matchesStatus = status == null || ncr.status == status
            val matchesQuery = query.isBlank() ||
                    ncr.entryNumber.contains(query, ignoreCase = true) ||
                    ncr.issueDescription.contains(query, ignoreCase = true) ||
                    ncr.vendorName.contains(query, ignoreCase = true) ||
                    ncr.block.contains(query, ignoreCase = true) ||
                    ncr.floor.contains(query, ignoreCase = true) ||
                    ncr.specificLocation.contains(query, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }
}
