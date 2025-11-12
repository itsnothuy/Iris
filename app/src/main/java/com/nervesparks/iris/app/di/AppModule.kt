package com.nervesparks.iris.app.di

import com.nervesparks.iris.app.events.EventBus
import com.nervesparks.iris.app.events.EventBusImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application layer components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindEventBus(impl: EventBusImpl): EventBus
}
