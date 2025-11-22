package com.example.neuroshelf.data.repository

import com.example.neuroshelf.data.db.dao.EventDao
import com.example.neuroshelf.data.db.entities.Event
import com.example.neuroshelf.domain.event.EventRepository

class EventRepositoryImpl(
    private val eventDao: EventDao
) : EventRepository {

    override suspend fun saveEvent(event: Event) {
        eventDao.insert(event)
    }

    override suspend fun getAllEvents(): List<Event> {
        return eventDao.getAll()
    }
}
