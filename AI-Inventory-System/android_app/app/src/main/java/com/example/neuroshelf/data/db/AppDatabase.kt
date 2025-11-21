package com.example.neuroshelf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.neuroshelf.data.db.dao.EmployeeDao
import com.example.neuroshelf.data.db.dao.EventDao
import com.example.neuroshelf.data.db.dao.ProductDao
import com.example.neuroshelf.data.db.entities.Employee
import com.example.neuroshelf.data.db.entities.Event
import com.example.neuroshelf.data.db.entities.Product

@Database(entities = [Employee::class, Product::class, Event::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun productDao(): ProductDao
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "neuroshelf_db")
                    .fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}