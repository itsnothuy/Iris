package com.nervesparks.iris.core.multimodal.di

import com.nervesparks.iris.core.multimodal.ImageProcessor
import com.nervesparks.iris.core.multimodal.MultimodalModelRegistry
import com.nervesparks.iris.core.multimodal.VisionProcessingEngine
import com.nervesparks.iris.core.multimodal.image.ImageProcessorImpl
import com.nervesparks.iris.core.multimodal.registry.MultimodalModelRegistryImpl
import com.nervesparks.iris.core.multimodal.vision.VisionProcessingEngineImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt module for multimodal components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MultimodalModule {
    
    @Binds
    @Singleton
    abstract fun bindMultimodalModelRegistry(
        impl: MultimodalModelRegistryImpl
    ): MultimodalModelRegistry
    
    @Binds
    @Singleton
    abstract fun bindImageProcessor(
        impl: ImageProcessorImpl
    ): ImageProcessor
    
    @Binds
    @Singleton
    abstract fun bindVisionProcessingEngine(
        impl: VisionProcessingEngineImpl
    ): VisionProcessingEngine
}

/**
 * Provides coroutine dispatchers
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

/**
 * Qualifier for IO dispatcher
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
