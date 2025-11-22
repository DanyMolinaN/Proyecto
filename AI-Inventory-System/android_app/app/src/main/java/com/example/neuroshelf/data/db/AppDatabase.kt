package com.example.neuroshelf.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.neuroshelf.data.db.dao.EmployeeDao
import com.example.neuroshelf.data.db.dao.ProductDao
import com.example.neuroshelf.data.db.dao.EventDao
import com.example.neuroshelf.data.db.entities.Employee
import com.example.neuroshelf.data.db.entities.Product
import com.example.neuroshelf.data.db.entities.Event

@Database(
    entities = [
        Employee::class,
        Product::class,
        Event::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun employeeDao(): EmployeeDao
    abstract fun productDao(): ProductDao
    abstract fun eventDao(): EventDao
}
