package com.example.rendinxr.feature.review.presentation

import com.example.rendinxr.core.domain.model.Defect

sealed class ReviewEvent {
    data class SelectDefect(val defect: Defect) : ReviewEvent()
    object ClearSelection : ReviewEvent()
    data class RequestDelete(val defect: Defect) : ReviewEvent()
    object ConfirmDelete : ReviewEvent()
    object CancelDelete : ReviewEvent()
    object DeleteAll : ReviewEvent()
    object ConfirmDeleteAll : ReviewEvent()
    object ClearError : ReviewEvent()
}