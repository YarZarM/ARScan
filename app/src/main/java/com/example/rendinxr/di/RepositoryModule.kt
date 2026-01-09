package com.example.rendinxr.di

import com.example.rendinxr.feature.scan.data.repository.DefectRepositoryImpl
import com.example.rendinxr.feature.scan.domain.repository.DefectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDefectRepository(
        impl: DefectRepositoryImpl
    ): DefectRepository
}