package com.example.neuroshelf20.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EmployeeRepositoryFirebase {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val col = db.collection("employees")

    suspend fun getAllEmbeddings(): List<Pair<String, FloatArray>> {
        val snap = col.get().await()
        return snap.documents.mapNotNull { doc ->
            val id: String = doc.getString("employeeId") ?: return@mapNotNull null
            val list = doc.get("embeddingVector") as? List<Double> ?: return@mapNotNull null
            id to list.map { it.toFloat() }.toFloatArray()
        }
    }

    suspend fun getEmployeeById(id: String): Map<String, Any>? {
        val doc = col.document(id).get().await()
        return doc.data // Regresa un Map<String, Any>
    }
}
