package com.example.rendinxr.feature.scan.presentation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rendinxr.core.data.ImageStorage
import com.example.rendinxr.core.domain.model.Defect
import com.example.rendinxr.feature.scan.domain.repository.DefectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val defectRepository: DefectRepository,
    private val imageStorage: ImageStorage
): ViewModel() {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        observeDefectCount()
    }

    private fun observeDefectCount() {
        defectRepository.getDefectCount()
            .onEach { count ->
                _state.update { it.copy(savedDefectsCount = count) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.ARSessionReady -> {
                _state.update { it.copy(isARSessionReady = true) }
            }

            is ScanEvent.PlaneDetected -> {
                _state.update { it.copy(isPlanDetected = true) }
            }

            is ScanEvent.TrackingStateChanged -> {
                _state.update { it.copy(trackingState = event.state) }
            }

            is ScanEvent.TapToPlace -> {
                handleTapToPlace(event)
            }

            is ScanEvent.ImageCaptured -> {
                handleImageCaptured(event.defectId, event.bitmap)
            }

            is ScanEvent.DescriptionChanged -> {
                _state.update { it.copy(descriptionText = event.text) }
            }

            is ScanEvent.SaveDefect -> {
                saveCurrentDefect()
            }

            is ScanEvent.DismissDialog -> {
                dismissDialogAndCleanup()
            }

            is ScanEvent.DeletePendingDefect -> {
                deletePendingDefect(event.id)
            }

            is ScanEvent.ClearError -> {
                _state.update { it.copy(errorMessage = null) }
            }

            is ScanEvent.NavigateToReview -> {
                viewModelScope.launch {
                    _navigationEvent.emit(NavigationEvent.ToReview)
                }
            }
        }
    }

    private fun handleTapToPlace(event: ScanEvent.TapToPlace) {
        val pendingDefect = PendingDefect(
            id = event.defectId,
            worldX = event.worldX,
            worldY = event.worldY,
            worldZ = event.worldZ,
            surfaceType = event.surfaceType
        )

        _state.update { currentState ->
            currentState.copy(
                pendingDefects = currentState.pendingDefects + pendingDefect,
                currentPendingDefect = pendingDefect,
                isCapturingImage = true,
            )
        }

    }

    private fun handleImageCaptured(defectId: String, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                val saveResult = imageStorage.saveImage(bitmap)

                _state.update { currentState ->
                    val updatePending = currentState.pendingDefects.map { pending ->
                        if (pending.id == defectId) {
                            pending.copy(
                                imagePath = saveResult.imagePath,
                                thumbnailPath = saveResult.thumbnailPath
                            )
                        } else pending
                    }

                    val updatedCurrent = currentState.currentPendingDefect?.let { current ->
                        if (current.id == defectId) {
                            current.copy(
                                imagePath = saveResult.imagePath,
                                thumbnailPath = saveResult.thumbnailPath
                            )
                        } else current
                    }

                    currentState.copy(
                        pendingDefects = updatePending,
                        currentPendingDefect = updatedCurrent,
                        isCapturingImage = false,
                        showDescriptionDialog = true
                    )
                }
                bitmap.recycle()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCapturingImage = false,
                        errorMessage = "Failed to save image: ${e.message}"
                    )
                }
            }
        }
    }

    private fun saveCurrentDefect() {
        val currentPending = _state.value.currentPendingDefect ?: return
        val description = _state.value.descriptionText.trim()

        if (description.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter a description") }
            return
        }

        val imagePath = currentPending.imagePath
        if (imagePath == null) {
            _state.update { it.copy(errorMessage = "Image not captured") }
            return
        }

        viewModelScope.launch {
            try {
                val defect = Defect(
                    id = currentPending.id,
                    description = description,
                    imagePath = imagePath,
                    thumbnailPath = currentPending.thumbnailPath,
                    worldX = currentPending.worldX,
                    worldY = currentPending.worldY,
                    worldZ = currentPending.worldZ,
                    surfaceType = currentPending.surfaceType
                )

                defectRepository.saveDefect(defect)

                _state.update { currentState ->
                    currentState.copy(
                        pendingDefects = currentState.pendingDefects.filter { it.id != currentPending.id },
                        currentPendingDefect = null,
                        showDescriptionDialog = false,
                        descriptionText = ""
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(errorMessage = "Failed to save defect: ${e.message}")
                }
            }
        }
    }

    private fun dismissDialogAndCleanup() {
        val currentPending = _state.value.currentPendingDefect

        viewModelScope.launch {
            currentPending?.let { pending ->
                pending.imagePath?.let { path ->
                    imageStorage.deleteImage(path, pending.thumbnailPath)
                }
            }

            _state.update { currentState ->
                currentState.copy(
                    pendingDefects = currentState.pendingDefects.filter {
                        it.id != currentPending?.id
                    },
                    currentPendingDefect = null,
                    showDescriptionDialog = false,
                    descriptionText = ""
                )
            }
        }
    }

    private fun deletePendingDefect(id: String) {
        val pending = _state.value.pendingDefects.find { it.id == id } ?: return

        viewModelScope.launch {
            pending.imagePath?.let { path ->
                imageStorage.deleteImage(path, pending.thumbnailPath)
            }

            _state.update { currentState ->
                currentState.copy(
                    pendingDefects = currentState.pendingDefects.filter { it.id != id }
                )
            }
        }
    }
}

sealed class NavigationEvent {
    object ToReview: NavigationEvent()
}