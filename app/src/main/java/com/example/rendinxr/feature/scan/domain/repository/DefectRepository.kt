package com.example.rendinxr.feature.scan.domain.repository

import com.example.rendinxr.core.domain.model.Defect
import kotlinx.coroutines.flow.Flow

interface DefectRepository {
    fun getAllDefects(): Flow<List<Defect>>
    suspend fun getDefectById(id: String): Defect?
    suspend fun saveDefect(defect: Defect)
    suspend fun deleteDefect(id: String)
    suspend fun deleteAllDefects()
    fun getDefectCount(): Flow<Int>
}