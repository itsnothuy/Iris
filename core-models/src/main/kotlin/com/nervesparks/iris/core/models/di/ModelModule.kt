package com.nervesparks.iris.core.models.di

import com.nervesparks.iris.core.models.downloader.ModelDownloader
import com.nervesparks.iris.core.models.downloader.ModelDownloaderImpl
import com.nervesparks.iris.core.models.registry.ModelRegistry
import com.nervesparks.iris.core.models.registry.ModelRegistryImpl
import com.nervesparks.iris.core.models.storage.ModelStorage
import com.nervesparks.iris.core.models.storage.ModelStorageImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for model management dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModelModule {
    
    @Binds
    @Singleton
    abstract fun bindModelRegistry(
        impl: ModelRegistryImpl
    ): ModelRegistry
    
    @Binds
    @Singleton
    abstract fun bindModelStorage(
        impl: ModelStorageImpl
    ): ModelStorage
    
    @Binds
    @Singleton
    abstract fun bindModelDownloader(
        impl: ModelDownloaderImpl
    ): ModelDownloader
}
