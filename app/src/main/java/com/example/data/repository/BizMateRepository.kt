package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.BizMateDatabase
import com.example.data.local.dao.SaleWithItems
import com.example.data.local.entity.CustomerCreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CartItem(
    val product: ProductEntity,
    val quantity: Double,
    val unitPriceCents: Long
) {
    val totalCents: Long
        get() = (quantity * unitPriceCents).toLong()
}

class BizMateRepository(private val database: BizMateDatabase) {
    private val productDao = database.productDao()
    private val saleDao = database.saleDao()
    private val expenseDao = database.expenseDao()
    private val customerDao = database.customerDao()
    private val stockMovementDao = database.stockMovementDao()

    // Products
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val totalProductCount: Flow<Int> = productDao.getTotalProductCount()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()

    suspend fun insertProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        val id = productDao.insertProduct(product)
        if (product.currentStock > 0) {
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = id,
                    productName = product.name,
                    quantityChange = product.currentStock,
                    newStockLevel = product.currentStock,
                    reason = "INITIAL_STOCK",
                    reference = "PROD-$id"
                )
            )
        }
        id
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun adjustStock(productId: Long, delta: Double, reason: String) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val prod = productDao.getProductById(productId) ?: return@withTransaction
            val newStock = (prod.currentStock + delta).coerceAtLeast(0.0)
            productDao.updateStock(productId, newStock)
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = productId,
                    productName = prod.name,
                    quantityChange = delta,
                    newStockLevel = newStock,
                    reason = reason,
                    reference = "MANUAL_ADJUST"
                )
            )
        }
    }

    // Sales
    val allSalesWithItems: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()
    val recentSales: Flow<List<SaleEntity>> = saleDao.getRecentSales(15)

    fun getSalesBetween(startTime: Long, endTime: Long): Flow<List<SaleWithItems>> {
        return saleDao.getSalesBetween(startTime, endTime)
    }

    suspend fun checkoutSale(
        items: List<CartItem>,
        discountCents: Long,
        paymentMethod: String,
        amountPaidCents: Long,
        customerId: Long?,
        customerName: String?
    ): Result<SaleEntity> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cart is empty"))
        }

        try {
            val sale = database.withTransaction {
                val subtotalCents = items.sumOf { it.totalCents }
                val validDiscount = discountCents.coerceIn(0L, subtotalCents)
                val totalCents = (subtotalCents - validDiscount).coerceAtLeast(0L)
                val changeCents = if (paymentMethod == "CASH") {
                    (amountPaidCents - totalCents).coerceAtLeast(0L)
                } else {
                    0L
                }

                val timestamp = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.US)
                val invoiceNumber = "INV-" + dateFormat.format(Date(timestamp))

                val saleEntity = SaleEntity(
                    invoiceNumber = invoiceNumber,
                    timestamp = timestamp,
                    subtotalCents = subtotalCents,
                    discountCents = validDiscount,
                    totalCents = totalCents,
                    paymentMethod = paymentMethod,
                    amountPaidCents = if (paymentMethod == "CREDIT") 0L else amountPaidCents,
                    changeCents = changeCents,
                    customerId = customerId,
                    customerName = customerName,
                    isVoided = false
                )

                val saleId = saleDao.insertSale(saleEntity)

                val saleItemsToInsert = mutableListOf<SaleItemEntity>()
                for (item in items) {
                    val currentProd = productDao.getProductById(item.product.id)
                    val costPrice = currentProd?.costPriceCents ?: item.product.costPriceCents
                    val currentStock = currentProd?.currentStock ?: item.product.currentStock
                    val newStock = (currentStock - item.quantity).coerceAtLeast(0.0)

                    productDao.updateStock(item.product.id, newStock)

                    saleItemsToInsert.add(
                        SaleItemEntity(
                            saleId = saleId,
                            productId = item.product.id,
                            productName = item.product.name,
                            quantity = item.quantity,
                            unitPriceCents = item.unitPriceCents,
                            costPriceCents = costPrice,
                            totalCents = item.totalCents
                        )
                    )

                    stockMovementDao.insertMovement(
                        StockMovementEntity(
                            productId = item.product.id,
                            productName = item.product.name,
                            quantityChange = -item.quantity,
                            newStockLevel = newStock,
                            reason = "SALE",
                            reference = invoiceNumber
                        )
                    )
                }

                saleDao.insertSaleItems(saleItemsToInsert)

                if (paymentMethod == "CREDIT" && customerId != null) {
                    val latestTx = customerDao.getLatestTransaction(customerId)
                    val prevBalance = latestTx?.balanceAfterCents ?: 0L
                    val newBalance = prevBalance + totalCents

                    customerDao.insertCreditTransaction(
                        CustomerCreditTransactionEntity(
                            customerId = customerId,
                            type = "CREDIT_SALE",
                            amountCents = totalCents,
                            balanceAfterCents = newBalance,
                            note = "Sale $invoiceNumber",
                            saleId = saleId
                        )
                    )
                }

                saleEntity.copy(id = saleId)
            }
            Result.success(sale)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun voidSale(saleId: Long, reason: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                val saleWithItems = saleDao.getSaleWithItemsById(saleId)
                    ?: throw IllegalArgumentException("Sale not found")
                val sale = saleWithItems.sale
                if (sale.isVoided) {
                    throw IllegalStateException("Sale is already voided")
                }

                saleDao.markSaleAsVoided(saleId, reason)

                // Restore stock for all items
                for (item in saleWithItems.items) {
                    val prod = productDao.getProductById(item.productId)
                    if (prod != null) {
                        val newStock = prod.currentStock + item.quantity
                        productDao.updateStock(item.productId, newStock)

                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = item.productId,
                                productName = item.productName,
                                quantityChange = item.quantity,
                                newStockLevel = newStock,
                                reason = "SALE_VOID",
                                reference = sale.invoiceNumber
                            )
                        )
                    }
                }

                // If credit sale, reverse credit ledger
                if (sale.paymentMethod == "CREDIT" && sale.customerId != null) {
                    val latestTx = customerDao.getLatestTransaction(sale.customerId)
                    val prevBalance = latestTx?.balanceAfterCents ?: 0L
                    val newBalance = (prevBalance - sale.totalCents).coerceAtLeast(0L)

                    customerDao.insertCreditTransaction(
                        CustomerCreditTransactionEntity(
                            customerId = sale.customerId,
                            type = "VOID_REVERSAL",
                            amountCents = sale.totalCents,
                            balanceAfterCents = newBalance,
                            note = "Void of ${sale.invoiceNumber} ($reason)",
                            saleId = saleId
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getExpensesBetween(startTime: Long, endTime: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesBetween(startTime, endTime)
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    // Customers & Credit
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun insertCustomer(customer: CustomerEntity): Long = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    fun getTransactionsForCustomer(customerId: Long): Flow<List<CustomerCreditTransactionEntity>> {
        return customerDao.getTransactionsForCustomer(customerId)
    }

    suspend fun getCustomerBalance(customerId: Long): Long = withContext(Dispatchers.IO) {
        customerDao.getLatestTransaction(customerId)?.balanceAfterCents ?: 0L
    }

    suspend fun recordCustomerRepayment(
        customerId: Long,
        amountCents: Long,
        note: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (amountCents <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        try {
            database.withTransaction {
                val latestTx = customerDao.getLatestTransaction(customerId)
                val prevBalance = latestTx?.balanceAfterCents ?: 0L
                val newBalance = (prevBalance - amountCents).coerceAtLeast(0L)

                customerDao.insertCreditTransaction(
                    CustomerCreditTransactionEntity(
                        customerId = customerId,
                        type = "PAYMENT_RECEIVED",
                        amountCents = amountCents,
                        balanceAfterCents = newBalance,
                        note = note ?: "Payment received"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Stock movements
    val allStockMovements: Flow<List<StockMovementEntity>> = stockMovementDao.getAllMovements()

    // Backup & Restore
    suspend fun exportBackupJson(shopName: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "BizMate LK")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("shopName", shopName)

        val products = productDao.getAllProductsDirect()
        val productsArray = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("barcode", p.barcode ?: "")
            obj.put("category", p.category)
            obj.put("costPriceCents", p.costPriceCents)
            obj.put("sellingPriceCents", p.sellingPriceCents)
            obj.put("currentStock", p.currentStock)
            obj.put("lowStockThreshold", p.lowStockThreshold)
            obj.put("unit", p.unit)
            obj.put("createdAt", p.createdAt)
            productsArray.put(obj)
        }
        root.put("products", productsArray)

        val sales = saleDao.getAllSalesDirect()
        val salesArray = JSONArray()
        for (s in sales) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("invoiceNumber", s.invoiceNumber)
            obj.put("timestamp", s.timestamp)
            obj.put("subtotalCents", s.subtotalCents)
            obj.put("discountCents", s.discountCents)
            obj.put("totalCents", s.totalCents)
            obj.put("paymentMethod", s.paymentMethod)
            obj.put("amountPaidCents", s.amountPaidCents)
            obj.put("changeCents", s.changeCents)
            obj.put("customerId", s.customerId ?: -1L)
            obj.put("customerName", s.customerName ?: "")
            obj.put("isVoided", s.isVoided)
            obj.put("voidReason", s.voidReason ?: "")
            obj.put("voidTimestamp", s.voidTimestamp ?: -1L)
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        val saleItems = saleDao.getAllSaleItemsDirect()
        val itemsArray = JSONArray()
        for (si in saleItems) {
            val obj = JSONObject()
            obj.put("id", si.id)
            obj.put("saleId", si.saleId)
            obj.put("productId", si.productId)
            obj.put("productName", si.productName)
            obj.put("quantity", si.quantity)
            obj.put("unitPriceCents", si.unitPriceCents)
            obj.put("costPriceCents", si.costPriceCents)
            obj.put("totalCents", si.totalCents)
            itemsArray.put(obj)
        }
        root.put("saleItems", itemsArray)

        val expenses = expenseDao.getAllExpensesDirect()
        val expensesArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("amountCents", e.amountCents)
            obj.put("category", e.category)
            obj.put("dateTimestamp", e.dateTimestamp)
            obj.put("note", e.note ?: "")
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        val customers = customerDao.getAllCustomersDirect()
        val customersArray = JSONArray()
        for (c in customers) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("phone", c.phone ?: "")
            obj.put("notes", c.notes ?: "")
            obj.put("createdAt", c.createdAt)
            customersArray.put(obj)
        }
        root.put("customers", customersArray)

        val creditTx = customerDao.getAllCreditTransactionsDirect()
        val creditArray = JSONArray()
        for (tx in creditTx) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("customerId", tx.customerId)
            obj.put("type", tx.type)
            obj.put("amountCents", tx.amountCents)
            obj.put("balanceAfterCents", tx.balanceAfterCents)
            obj.put("timestamp", tx.timestamp)
            obj.put("note", tx.note ?: "")
            obj.put("saleId", tx.saleId ?: -1L)
            creditArray.put(obj)
        }
        root.put("creditTransactions", creditArray)

        val movements = stockMovementDao.getAllMovementsDirect()
        val movementsArray = JSONArray()
        for (m in movements) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("productId", m.productId)
            obj.put("productName", m.productName)
            obj.put("quantityChange", m.quantityChange)
            obj.put("newStockLevel", m.newStockLevel)
            obj.put("reason", m.reason)
            obj.put("timestamp", m.timestamp)
            obj.put("reference", m.reference ?: "")
            movementsArray.put(obj)
        }
        root.put("stockMovements", movementsArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("products") || !root.has("sales")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid BizMate backup format"))
            }

            database.withTransaction {
                // Clear tables safely
                productDao.clearAll()
                saleDao.clearSales()
                saleDao.clearSaleItems()
                expenseDao.clearAll()
                customerDao.clearCustomers()
                customerDao.clearCreditTransactions()
                stockMovementDao.clearAll()

                val productsArray = root.getJSONArray("products")
                val productsList = mutableListOf<ProductEntity>()
                for (i in 0 until productsArray.length()) {
                    val obj = productsArray.getJSONObject(i)
                    productsList.add(
                        ProductEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            barcode = obj.optString("barcode").takeIf { it.isNotBlank() },
                            category = obj.optString("category", "General"),
                            costPriceCents = obj.optLong("costPriceCents", 0L),
                            sellingPriceCents = obj.optLong("sellingPriceCents", 0L),
                            currentStock = obj.optDouble("currentStock", 0.0),
                            lowStockThreshold = obj.optDouble("lowStockThreshold", 5.0),
                            unit = obj.optString("unit", "pcs"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                productDao.insertAll(productsList)

                val salesArray = root.getJSONArray("sales")
                for (i in 0 until salesArray.length()) {
                    val obj = salesArray.getJSONObject(i)
                    val sId = obj.optLong("id", 0L)
                    val custId = obj.optLong("customerId", -1L).takeIf { it > 0 }
                    val voidTs = obj.optLong("voidTimestamp", -1L).takeIf { it > 0 }
                    saleDao.insertSale(
                        SaleEntity(
                            id = sId,
                            invoiceNumber = obj.getString("invoiceNumber"),
                            timestamp = obj.getLong("timestamp"),
                            subtotalCents = obj.getLong("subtotalCents"),
                            discountCents = obj.optLong("discountCents", 0L),
                            totalCents = obj.getLong("totalCents"),
                            paymentMethod = obj.getString("paymentMethod"),
                            amountPaidCents = obj.optLong("amountPaidCents", 0L),
                            changeCents = obj.optLong("changeCents", 0L),
                            customerId = custId,
                            customerName = obj.optString("customerName").takeIf { it.isNotBlank() },
                            isVoided = obj.optBoolean("isVoided", false),
                            voidReason = obj.optString("voidReason").takeIf { it.isNotBlank() },
                            voidTimestamp = voidTs
                        )
                    )
                }

                val itemsArray = root.getJSONArray("saleItems")
                val itemsList = mutableListOf<SaleItemEntity>()
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    itemsList.add(
                        SaleItemEntity(
                            id = obj.optLong("id", 0L),
                            saleId = obj.getLong("saleId"),
                            productId = obj.getLong("productId"),
                            productName = obj.getString("productName"),
                            quantity = obj.getDouble("quantity"),
                            unitPriceCents = obj.getLong("unitPriceCents"),
                            costPriceCents = obj.optLong("costPriceCents", 0L),
                            totalCents = obj.getLong("totalCents")
                        )
                    )
                }
                saleDao.insertSaleItems(itemsList)

                if (root.has("expenses")) {
                    val expensesArray = root.getJSONArray("expenses")
                    val expensesList = mutableListOf<ExpenseEntity>()
                    for (i in 0 until expensesArray.length()) {
                        val obj = expensesArray.getJSONObject(i)
                        expensesList.add(
                            ExpenseEntity(
                                id = obj.optLong("id", 0L),
                                title = obj.getString("title"),
                                amountCents = obj.getLong("amountCents"),
                                category = obj.optString("category", "Other"),
                                dateTimestamp = obj.getLong("dateTimestamp"),
                                note = obj.optString("note").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                    expenseDao.insertAll(expensesList)
                }

                if (root.has("customers")) {
                    val customersArray = root.getJSONArray("customers")
                    val customersList = mutableListOf<CustomerEntity>()
                    for (i in 0 until customersArray.length()) {
                        val obj = customersArray.getJSONObject(i)
                        customersList.add(
                            CustomerEntity(
                                id = obj.optLong("id", 0L),
                                name = obj.getString("name"),
                                phone = obj.optString("phone").takeIf { it.isNotBlank() },
                                notes = obj.optString("notes").takeIf { it.isNotBlank() },
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    customerDao.insertAllCustomers(customersList)
                }

                if (root.has("creditTransactions")) {
                    val creditArray = root.getJSONArray("creditTransactions")
                    val creditList = mutableListOf<CustomerCreditTransactionEntity>()
                    for (i in 0 until creditArray.length()) {
                        val obj = creditArray.getJSONObject(i)
                        val saleId = obj.optLong("saleId", -1L).takeIf { it > 0 }
                        creditList.add(
                            CustomerCreditTransactionEntity(
                                id = obj.optLong("id", 0L),
                                customerId = obj.getLong("customerId"),
                                type = obj.getString("type"),
                                amountCents = obj.getLong("amountCents"),
                                balanceAfterCents = obj.getLong("balanceAfterCents"),
                                timestamp = obj.getLong("timestamp"),
                                note = obj.optString("note").takeIf { it.isNotBlank() },
                                saleId = saleId
                            )
                        )
                    }
                    customerDao.insertAllCreditTransactions(creditList)
                }

                if (root.has("stockMovements")) {
                    val movementsArray = root.getJSONArray("stockMovements")
                    val movementsList = mutableListOf<StockMovementEntity>()
                    for (i in 0 until movementsArray.length()) {
                        val obj = movementsArray.getJSONObject(i)
                        movementsList.add(
                            StockMovementEntity(
                                id = obj.optLong("id", 0L),
                                productId = obj.getLong("productId"),
                                productName = obj.getString("productName"),
                                quantityChange = obj.getDouble("quantityChange"),
                                newStockLevel = obj.getDouble("newStockLevel"),
                                reason = obj.optString("reason", "ADJUSTMENT"),
                                timestamp = obj.getLong("timestamp"),
                                reference = obj.optString("reference").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                    stockMovementDao.insertAllMovements(movementsList)
                }
            }
            Result.success("Restored successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedInitialSampleProductsIfNeeded() = withContext(Dispatchers.IO) {
        val count = productDao.getAllProductsDirect().size
        if (count == 0) {
            val sampleProducts = listOf(
                ProductEntity(
                    name = "Anchor Milk Powder 400g",
                    barcode = "890123456001",
                    category = "Groceries",
                    costPriceCents = 98000L, // Rs. 980.00
                    sellingPriceCents = 112000L, // Rs. 1,120.00
                    currentStock = 24.0,
                    lowStockThreshold = 5.0,
                    unit = "pack"
                ),
                ProductEntity(
                    name = "Munchee Super Cream Cracker",
                    barcode = "890123456002",
                    category = "Snacks",
                    costPriceCents = 21000L, // Rs. 210.00
                    sellingPriceCents = 25000L, // Rs. 250.00
                    currentStock = 40.0,
                    lowStockThreshold = 10.0,
                    unit = "pack"
                ),
                ProductEntity(
                    name = "Watawala Tea 200g",
                    barcode = "890123456003",
                    category = "Beverages",
                    costPriceCents = 39000L, // Rs. 390.00
                    sellingPriceCents = 46000L, // Rs. 460.00
                    currentStock = 18.0,
                    lowStockThreshold = 4.0,
                    unit = "pack"
                ),
                ProductEntity(
                    name = "White Sugar 1kg",
                    barcode = "890123456004",
                    category = "Groceries",
                    costPriceCents = 23000L, // Rs. 230.00
                    sellingPriceCents = 27000L, // Rs. 270.00
                    currentStock = 50.0,
                    lowStockThreshold = 10.0,
                    unit = "kg"
                ),
                ProductEntity(
                    name = "Samba Rice 5kg",
                    barcode = "890123456005",
                    category = "Groceries",
                    costPriceCents = 125000L, // Rs. 1,250.00
                    sellingPriceCents = 145000L, // Rs. 1,450.00
                    currentStock = 15.0,
                    lowStockThreshold = 3.0,
                    unit = "bag"
                ),
                ProductEntity(
                    name = "Sunlight Soap 115g",
                    barcode = "890123456006",
                    category = "Household",
                    costPriceCents = 13000L, // Rs. 130.00
                    sellingPriceCents = 16000L, // Rs. 160.00
                    currentStock = 3.0, // Low stock on purpose for testing
                    lowStockThreshold = 5.0,
                    unit = "pcs"
                )
            )
            for (p in sampleProducts) {
                insertProduct(p)
            }

            // Also seed sample customer
            val custId = customerDao.insertCustomer(
                CustomerEntity(
                    name = "Kamal Perera",
                    phone = "0771234567",
                    notes = "Regular customer"
                )
            )
        }
    }
}
