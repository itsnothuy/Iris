package com.nervesparks.iris.core.llm.conversation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing conversation manager dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ConversationModule {
    
    @Binds
    @Singleton
    abstract fun bindConversationManager(
        impl: ConversationManagerImpl
    ): ConversationManager
}
