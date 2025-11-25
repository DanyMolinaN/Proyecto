package com.example.neuroshelf20.domain.event

import com.example.neuroshelf20.data.model.EventModel

interface EventRepository {
    suspend fun saveEvent(event: EventModel)
    suspend fun getAllEvents(): List<EventModel>
}
