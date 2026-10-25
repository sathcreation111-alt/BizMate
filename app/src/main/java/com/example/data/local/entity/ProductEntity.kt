package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val category: String = "General",
    val costPriceCents: Long = 0L,
    val sellingPriceCents: Long = 0L,
    val currentStock: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val unit: String = "pcs",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
