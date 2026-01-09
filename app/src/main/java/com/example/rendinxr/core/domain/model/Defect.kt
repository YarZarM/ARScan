package com.example.rendinxr.core.domain.model

import java.util.UUID

data class Defect(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val imagePath: String,
    val createdAt: Long = System.currentTimeMillis(),

    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,

    val surfaceType: SurfaceType = SurfaceType.UNKNOWN,

    val thumbnailPath: String? = null
)