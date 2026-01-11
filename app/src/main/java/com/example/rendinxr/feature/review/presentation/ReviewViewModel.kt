package com.example.rendinxr.feature.review.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rendinxr.core.data.ImageStorage
import com.example.rendinxr.feature.scan.domain.repository.DefectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val defectRepository: DefectRepository,
    private val imageStorage: ImageStorage
): ViewModel() {

    private val _state = MutableStateFlow(ReviewState())
    val state: StateFlow<ReviewState> = _state.asStateFlow()

    init {
        loadDefects()
    }

    private fun loadDefects() {
        defectRepository.getAllDefects()
            .onEach { defects ->
                _state.update {
                    it.copy(
                        defects = defects,
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ReviewEvent) {
        when (event) {
            is ReviewEvent.SelectDefect -> {
                _state.update { it.copy(selectedDefect = event.defect) }
            }

            is ReviewEvent.ClearSelection -> {
                _state.update { it.copy(selectedDefect = null) }
            }

            is ReviewEvent.RequestDelete -> {
                _state.update {
                    it.copy(
                        showDeleteConfirmation = true,
                        isDeleteAllConfirmation = false,
                        defectToDelete = event.defect
                    )
                }
            }

            is ReviewEvent.ConfirmDelete -> {
                if (_state.value.isDeleteAllConfirmation) {
                    deleteAllDefects()
                } else {
                    deleteDefect()
                }
            }

            is ReviewEvent.CancelDelete -> {
                _state.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        isDeleteAllConfirmation = false,
                        defectToDelete = null
                    )
                }
            }

            is ReviewEvent.DeleteAll -> {
                _state.update {
                    it.copy(
                        showDeleteConfirmation = true,
                        isDeleteAllConfirmation = true,
                        defectToDelete = null
                    )
                }
            }

            is ReviewEvent.ConfirmDeleteAll -> {
                deleteAllDefects()
            }

            is ReviewEvent.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun deleteDefect() {
        val defect = _state.value.defectToDelete ?: return

        viewModelScope.launch {
            try {
                imageStorage.deleteImage(defect.imagePath, defect.thumbnailPath)
                defectRepository.deleteDefect(defect.id)

                _state.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        isDeleteAllConfirmation = false,
                        defectToDelete = null,
                        selectedDefect = if (it.selectedDefect?.id == defect.id) null else it.selectedDefect
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = "Failed to delete: ${e.message}",
                        showDeleteConfirmation = false,
                        isDeleteAllConfirmation = false,
                        defectToDelete = null
                    )
                }
            }
        }
    }

    private fun deleteAllDefects() {
        viewModelScope.launch {
            try {
                imageStorage.deleteAllImages()
                defectRepository.deleteAllDefects()
                _state.update {
                    it.copy(
                        selectedDefect = null,
                        showDeleteConfirmation = false,
                        isDeleteAllConfirmation = false,
                        defectToDelete = null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        errorMessage = "Failed to delete all: ${e.message}",
                        showDeleteConfirmation = false,
                        isDeleteAllConfirmation = false
                    )
                }
            }
        }
    }
}