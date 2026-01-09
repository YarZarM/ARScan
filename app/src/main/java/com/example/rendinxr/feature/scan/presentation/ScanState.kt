package com.example.rendinxr.feature.scan.presentation

import com.example.rendinxr.core.domain.model.SurfaceType

data class ScanState(
    val isARSessionReady: Boolean = false,
    val isPlanDetected: Boolean = false,
    val pendingDefects: List<PendingDefect> = emptyList(),
    val savedDefectsCount: Int = 0,
    val showDescriptionDialog: Boolean = false,
    val currentPendingDefect: PendingDefect? = null,
    val descriptionText: String = "",
    val isCapturingImage: Boolean = false,
    val errorMessage: String? = null,
    val trackingState: TrackingState = TrackingState.INITIALIZING
)

data class PendingDefect(
    val id: String,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val surfaceType: SurfaceType,
    val imagePath: String? = null,
    val thumbnailPath: String? = null,
)

enum class TrackingState {
    INITIALIZING,
    TRACKING,
    LIMITED,
    STOPPED
}