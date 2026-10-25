package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateTimestamp DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateTimestamp >= :startTime AND dateTimestamp <= :endTime ORDER BY dateTimestamp DESC")
    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE dateTimestamp >= :startTime AND dateTimestamp <= :endTime")
    suspend fun getExpensesBetweenDirect(startTime: Long, endTime: Long): List<ExpenseEntity>

    @Query("SELECT SUM(amountCents) FROM expenses WHERE dateTimestamp >= :startTime AND dateTimestamp <= :endTime")
    fun getTotalExpensesBetween(startTime: Long, endTime: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesDirect(): List<ExpenseEntity>
}
