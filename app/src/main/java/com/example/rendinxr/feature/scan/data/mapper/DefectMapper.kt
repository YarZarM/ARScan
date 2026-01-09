package com.example.rendinxr.feature.scan.data.mapper

import com.example.rendinxr.core.domain.model.Defect
import com.example.rendinxr.core.domain.model.SurfaceType
import com.example.rendinxr.feature.scan.data.local.DefectEntity

object DefectMapper {

    fun entityToDomain(entity: DefectEntity): Defect {
        return Defect(
            id = entity.id,
            description = entity.description,
            imagePath = entity.imagePath,
            thumbnailPath = entity.thumbnailPath,
            createdAt = entity.createdAt,
            worldX = entity.worldX,
            worldY = entity.worldY,
            worldZ = entity.worldZ,
            surfaceType = try {
                SurfaceType.valueOf(entity.surfaceType)
            } catch (e: IllegalArgumentException) {
                SurfaceType.UNKNOWN
            }
        )
    }

    fun domainToEntity(defect: Defect): DefectEntity {
        return DefectEntity(
            id = defect.id,
            description = defect.description,
            imagePath = defect.imagePath,
            thumbnailPath = defect.thumbnailPath,
            createdAt = defect.createdAt,
            worldX = defect.worldX,
            worldY = defect.worldY,
            worldZ = defect.worldZ,
            surfaceType = defect.surfaceType.name
        )
    }
}