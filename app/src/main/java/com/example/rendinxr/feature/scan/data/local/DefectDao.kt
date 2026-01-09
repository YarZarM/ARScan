package com.example.rendinxr.feature.scan.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DefectDao {
    @Query("Select * FROM defects ORDER BY createdAt DESC")
    fun getAllDefects(): Flow<List<DefectEntity>>

    @Query("SELECT * FROM defects WHERE id = :id")
    suspend fun getDefectById(id: String): DefectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefect(defect: DefectEntity)

    @Query("DELETE FROM defects WHERE id = :id")
    suspend fun deleteDefect(id: String)

    @Query("DELETE FROM defects")
    suspend fun deleteAllDefects()

    @Query("SELECT COUNT(*) FROM defects")
    fun getDefectCount(): Flow<Int>

    @Query("SELECT imagePath FROM defects WHERE id = :id")
    suspend fun getImagePath(id: String): String?

    @Query("SELECT thumbnailPath FROM defects WHERE id = :id")
    suspend fun getThumbnailPath(id: String): String?
}