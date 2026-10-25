package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerCreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customer_credit_transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<CustomerCreditTransactionEntity>>

    @Query("SELECT * FROM customer_credit_transactions WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTransaction(customerId: Long): CustomerCreditTransactionEntity?

    @Query("SELECT * FROM customer_credit_transactions ORDER BY timestamp DESC")
    fun getAllCreditTransactions(): Flow<List<CustomerCreditTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditTransaction(transaction: CustomerCreditTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCreditTransactions(transactions: List<CustomerCreditTransactionEntity>)

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM customer_credit_transactions")
    suspend fun clearCreditTransactions()

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersDirect(): List<CustomerEntity>

    @Query("SELECT * FROM customer_credit_transactions")
    suspend fun getAllCreditTransactionsDirect(): List<CustomerCreditTransactionEntity>
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMovements(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements")
    suspend fun clearAll()

    @Query("SELECT * FROM stock_movements")
    suspend fun getAllMovementsDirect(): List<StockMovementEntity>
}
