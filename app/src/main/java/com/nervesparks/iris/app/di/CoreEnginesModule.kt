package com.nervesparks.iris.app.di

import com.nervesparks.iris.core.llm.LLMEngine
import com.nervesparks.iris.core.llm.LLMEngineImpl
import com.nervesparks.iris.core.rag.RAGEngine
import com.nervesparks.iris.core.rag.RAGEngineImpl
import com.nervesparks.iris.core.safety.SafetyEngine
import com.nervesparks.iris.core.safety.SafetyEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for core AI engines
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreEnginesModule {

    @Binds
    @Singleton
    abstract fun bindLLMEngine(impl: LLMEngineImpl): LLMEngine

    @Binds
    @Singleton
    abstract fun bindRAGEngine(impl: RAGEngineImpl): RAGEngine

    @Binds
    @Singleton
    abstract fun bindSafetyEngine(impl: SafetyEngineImpl): SafetyEngine
}
