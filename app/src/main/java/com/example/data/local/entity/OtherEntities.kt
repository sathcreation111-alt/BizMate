package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantityChange: Double,
    val newStockLevel: Double,
    val reason: String, // RESTOCK, SALE, LOSS, ADJUSTMENT, SALE_VOID
    val timestamp: Long = System.currentTimeMillis(),
    val reference: String? = null
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amountCents: Long,
    val category: String, // Utilities, Rent, Salary, Transport, Supplies, Other
    val dateTimestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_credit_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerCreditTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val type: String, // CREDIT_SALE, PAYMENT_RECEIVED, VOID_REVERSAL
    val amountCents: Long,
    val balanceAfterCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val saleId: Long? = null
)
