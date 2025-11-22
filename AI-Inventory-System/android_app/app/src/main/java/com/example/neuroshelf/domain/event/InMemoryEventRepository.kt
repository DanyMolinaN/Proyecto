package com.example.neuroshelf.domain.event

import com.example.neuroshelf.data.db.entities.Event
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryEventRepository : EventRepository {

    private val events = mutableListOf<Event>()
    private val mutex = Mutex()

    override suspend fun saveEvent(event: Event) {
        mutex.withLock {
            events.add(event)
        }
    }

    override suspend fun getAllEvents(): List<Event> =
        mutex.withLock { events.toList() }
}
