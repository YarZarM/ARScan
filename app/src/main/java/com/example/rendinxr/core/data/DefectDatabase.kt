package com.example.rendinxr.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.rendinxr.feature.scan.data.local.DefectDao
import com.example.rendinxr.feature.scan.data.local.DefectEntity

@Database(
    entities = [DefectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DefectDatabase: RoomDatabase() {
    abstract fun defectDao() : DefectDao
}