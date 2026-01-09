package com.example.rendinxr.feature.scan.presentation

import android.graphics.Bitmap
import com.example.rendinxr.core.domain.model.SurfaceType

sealed class ScanEvent {
    object ARSessionReady : ScanEvent()
    object PlaneDetected : ScanEvent()
    data class TrackingStateChanged(val state: TrackingState): ScanEvent()

    data class TapToPlace(
        val defectId: String,
        val worldX: Float,
        val worldY: Float,
        val worldZ: Float,
        val surfaceType: SurfaceType
    ): ScanEvent()

    data class ImageCaptured(
        val defectId: String,
        val bitmap: Bitmap
    ): ScanEvent()

    data class DescriptionChanged(val text: String): ScanEvent()
    object SaveDefect: ScanEvent()
    object DismissDialog: ScanEvent()
    data class DeletePendingDefect(val id: String): ScanEvent()
    object ClearError: ScanEvent()
    object NavigateToReview: ScanEvent()
}