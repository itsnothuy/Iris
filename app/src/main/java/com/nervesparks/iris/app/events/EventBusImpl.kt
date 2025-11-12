package com.nervesparks.iris.app.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of EventBus for application-wide events
 */
@Singleton
class EventBusImpl @Inject constructor() : EventBus {

    private val _events = MutableSharedFlow<IrisEvent>(
        extraBufferCapacity = 64,
        replay = 0,
    )

    override val events: SharedFlow<IrisEvent> = _events.asSharedFlow()

    override fun emit(event: IrisEvent) {
        _events.tryEmit(event)
    }

    /**
     * Subscribe to specific event type
     */
    inline fun <reified T : IrisEvent> subscribe(): Flow<T> {
        return events.filterIsInstance<T>()
    }
}
