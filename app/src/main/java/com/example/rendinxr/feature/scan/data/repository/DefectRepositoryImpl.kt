package com.example.rendinxr.feature.scan.data.repository

import com.example.rendinxr.core.data.ImageStorage
import com.example.rendinxr.core.domain.model.Defect
import com.example.rendinxr.feature.scan.data.local.DefectDao
import com.example.rendinxr.feature.scan.data.mapper.DefectMapper
import com.example.rendinxr.feature.scan.domain.repository.DefectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefectRepositoryImpl @Inject constructor(
    private val defectDao: DefectDao,
    private val imageStorage: ImageStorage
) : DefectRepository {

    override fun getAllDefects(): Flow<List<Defect>> {
        return defectDao.getAllDefects().map { entities ->
            entities.map { DefectMapper.entityToDomain(it) }
        }
    }

    override suspend fun getDefectById(id: String): Defect? {
        return defectDao.getDefectById(id)?.let {
            DefectMapper.entityToDomain(it)
        }
    }

    override suspend fun saveDefect(defect: Defect) {
        val entity = DefectMapper.domainToEntity(defect)
        defectDao.insertDefect(entity)
    }

    override suspend fun deleteDefect(id: String) {
        val imagePath = defectDao.getImagePath(id)
        val thumbnailPath = defectDao.getThumbnailPath(id)

        defectDao.deleteDefect(id)
        imagePath?.let { imageStorage.deleteImage(it, thumbnailPath) }
    }

    override suspend fun deleteAllDefects() {
        imageStorage.deleteAllImages()
        defectDao.deleteAllDefects()
    }

    override fun getDefectCount(): Flow<Int> {
        return defectDao.getDefectCount()
    }
}