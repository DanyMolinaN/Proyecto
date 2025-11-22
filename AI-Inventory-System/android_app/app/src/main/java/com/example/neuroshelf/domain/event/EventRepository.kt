package com.example.neuroshelf.domain.event

import com.example.neuroshelf.data.db.entities.Event

interface EventRepository {
    suspend fun saveEvent(event: Event)
    suspend fun getAllEvents(): List<Event>
}
