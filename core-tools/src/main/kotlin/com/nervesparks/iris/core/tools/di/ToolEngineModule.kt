package com.nervesparks.iris.core.tools.di

import com.nervesparks.iris.core.tools.ToolEngine
import com.nervesparks.iris.core.tools.ToolEngineImpl
import com.nervesparks.iris.core.tools.parser.FunctionCallParser
import com.nervesparks.iris.core.tools.parser.FunctionCallParserImpl
import com.nervesparks.iris.core.tools.registry.ToolRegistry
import com.nervesparks.iris.core.tools.registry.ToolRegistryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Tool Engine dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ToolEngineModule {
    
    @Binds
    @Singleton
    abstract fun bindToolEngine(impl: ToolEngineImpl): ToolEngine
    
    @Binds
    @Singleton
    abstract fun bindToolRegistry(impl: ToolRegistryImpl): ToolRegistry
    
    @Binds
    @Singleton
    abstract fun bindFunctionCallParser(impl: FunctionCallParserImpl): FunctionCallParser
}
