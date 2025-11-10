package com.nervesparks.iris.core.llm.inference

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing inference session dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InferenceModule {
    
    @Binds
    @Singleton
    abstract fun bindInferenceSession(
        impl: InferenceSessionImpl
    ): InferenceSession
}
