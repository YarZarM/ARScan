package com.example.rendinxr.di

import android.content.Context
import androidx.room.Room
import com.example.rendinxr.core.data.DefectDatabase
import com.example.rendinxr.feature.scan.data.local.DefectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDefectDatabase(
        @ApplicationContext context: Context
    ): DefectDatabase {
        return Room.databaseBuilder(
            context,
            DefectDatabase::class.java,
            "defect_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideDefectDao(database: DefectDatabase): DefectDao {
        return database.defectDao()
    }
}