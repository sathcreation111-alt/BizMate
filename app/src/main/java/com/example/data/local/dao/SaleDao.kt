package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)

@Dao
interface SaleDao {
    @androidx.room.Transaction
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @androidx.room.Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleWithItemsById(saleId: Long): SaleWithItems?

    @androidx.room.Transaction
    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleWithItems>>

    @androidx.room.Transaction
    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getSalesBetweenDirect(startTime: Long, endTime: Long): List<SaleWithItems>

    @Query("SELECT * FROM sales WHERE isVoided = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSales(limit: Int = 10): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Query("UPDATE sales SET isVoided = 1, voidReason = :reason, voidTimestamp = :timestamp WHERE id = :saleId")
    suspend fun markSaleAsVoided(saleId: Long, reason: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItemEntity>

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM sale_items")
    suspend fun clearSaleItems()

    @Query("SELECT * FROM sales")
    suspend fun getAllSalesDirect(): List<SaleEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsDirect(): List<SaleItemEntity>
}
