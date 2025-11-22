package com.example.neuroshelf.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.neuroshelf.data.db.entities.Employee

@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee)

    @Query("SELECT * FROM employees WHERE employeeId = :id LIMIT 1")
    suspend fun getEmployeeById(id: String): Employee?

    @Query("SELECT * FROM employees")
    suspend fun getAllEmployees(): List<Employee>

    @Query("DELETE FROM employees")
    suspend fun deleteAll()
}
