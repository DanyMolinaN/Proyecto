package com.example.neuroshelf.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey val employeeID: String,
    val name: String,
    val embeddingJson: String // serializar FloatArray a JSON o Base64
)
