package com.example.neuroshelf.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val productId: String,
    val name: String,
    val category: String,
    val sku: String? = null
)
