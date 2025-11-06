package com.nervesparks.iris.app.di

import com.nervesparks.iris.core.hw.BackendRouter
import com.nervesparks.iris.core.hw.BackendRouterImpl
import com.nervesparks.iris.core.hw.DeviceProfileProvider
import com.nervesparks.iris.core.hw.DeviceProfileProviderImpl
import com.nervesparks.iris.core.hw.ThermalManager
import com.nervesparks.iris.core.hw.ThermalManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for hardware abstraction layer
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HardwareModule {
    
    @Binds
    @Singleton
    abstract fun bindDeviceProfileProvider(
        impl: DeviceProfileProviderImpl
    ): DeviceProfileProvider
    
    @Binds
    @Singleton
    abstract fun bindBackendRouter(
        impl: BackendRouterImpl
    ): BackendRouter
    
    @Binds
    @Singleton
    abstract fun bindThermalManager(
        impl: ThermalManagerImpl
    ): ThermalManager
}
