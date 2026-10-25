package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val subtotalCents: Long,
    val discountCents: Long = 0L,
    val totalCents: Long,
    val paymentMethod: String, // CASH, CARD, CREDIT, OTHER
    val amountPaidCents: Long,
    val changeCents: Long = 0L,
    val customerId: Long? = null,
    val customerName: String? = null,
    val isVoided: Boolean = false,
    val voidReason: String? = null,
    val voidTimestamp: Long? = null
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["saleId"]), Index(value = ["productId"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Double,
    val unitPriceCents: Long,
    val costPriceCents: Long, // Historical cost price snapshot
    val totalCents: Long
)
