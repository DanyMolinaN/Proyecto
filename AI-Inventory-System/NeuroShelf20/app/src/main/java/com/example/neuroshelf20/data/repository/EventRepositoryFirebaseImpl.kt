package com.example.neuroshelf20.data.repository

import com.example.neuroshelf20.data.model.EventModel
import com.example.neuroshelf20.domain.event.EventRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EventRepositoryFirebaseImpl : EventRepository {
    private val db = FirebaseFirestore.getInstance()
    private val col = db.collection("events")

    override suspend fun saveEvent(event: EventModel) {
        val id = if (event.id.isBlank()) col.document().id else event.id
        val toSave = event.copy(id = id)
        col.document(id).set(toSave).await()
    }

    override suspend fun getAllEvents(): List<EventModel> {
        return col.get().await().toObjects(EventModel::class.java)
    }
}
