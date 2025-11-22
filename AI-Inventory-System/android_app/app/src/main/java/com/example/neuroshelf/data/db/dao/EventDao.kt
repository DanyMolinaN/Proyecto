package com.example.neuroshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neuroshelf.data.db.entities.Event

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event)

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    suspend fun getAllEvents(): List<Event>

    @Query("SELECT * FROM events WHERE employeeId = :employeeId ORDER BY timestamp DESC")
    suspend fun getEventsForEmployee(employeeId: String): List<Event>

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
