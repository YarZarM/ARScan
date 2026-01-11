package com.example.rendinxr.feature.review.presentation

import com.example.rendinxr.core.domain.model.Defect

data class ReviewState(
    val defects: List<Defect> = emptyList(),
    val isLoading: Boolean = true,
    val selectedDefect: Defect? = null,
    val showDeleteConfirmation: Boolean = false,
    val isDeleteAllConfirmation: Boolean = false,
    val defectToDelete: Defect? = null,
    val errorMessage: String? = null,
)