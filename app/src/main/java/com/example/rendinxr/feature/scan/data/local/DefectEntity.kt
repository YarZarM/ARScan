package com.example.rendinxr.feature.scan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "defects")
data class DefectEntity(
    @PrimaryKey
    val id: String,
    val description: String,
    val imagePath: String,
    val thumbnailPath: String?,
    val createdAt: Long,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float,
    val surfaceType: String
)