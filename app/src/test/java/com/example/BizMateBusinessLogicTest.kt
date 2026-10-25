package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BizMateDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.BizMateRepository
import com.example.data.repository.CartItem
import com.example.util.LkrFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BizMateBusinessLogicTest {

    private lateinit var database: BizMateDatabase
    private lateinit var repository: BizMateRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BizMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BizMateRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testLkrFormatter() {
        val formattedEn = LkrFormatter.format(125000L, "en")
        assertEquals("Rs. 1,250.00", formattedEn)

        val formattedSi = LkrFormatter.format(125000L, "si")
        assertEquals("රු. 1,250.00", formattedSi)

        val parsedCents = LkrFormatter.parseToCents("1,250.50")
        assertEquals(125050L, parsedCents)
    }

    @Test
    fun testSaleCheckoutAndStockDeduction() = runBlocking {
        // Insert product
        val prodId = repository.insertProduct(
            ProductEntity(
                name = "Test Milk",
                sellingPriceCents = 50000L, // Rs. 500.00
                costPriceCents = 40000L,    // Rs. 400.00
                currentStock = 10.0,
                unit = "pack"
            )
        )

        val products = repository.allProducts.first()
        val prod = products.first { it.id == prodId }
        assertEquals(10.0, prod.currentStock, 0.001)

        // Checkout 2 packs
        val cartItems = listOf(
            CartItem(
                product = prod,
                quantity = 2.0,
                unitPriceCents = prod.sellingPriceCents
            )
        )

        val result = repository.checkoutSale(
            items = cartItems,
            discountCents = 5000L, // Rs. 50.00 discount
            paymentMethod = "CASH",
            amountPaidCents = 100000L, // Rs. 1000.00
            customerId = null,
            customerName = null
        )

        assertTrue(result.isSuccess)
        val sale = result.getOrNull()
        assertNotNull(sale)
        assertEquals(100000L, sale!!.subtotalCents) // 2 * 500 = 1000
        assertEquals(5000L, sale.discountCents)
        assertEquals(95000L, sale.totalCents) // 950.00
        assertEquals(5000L, sale.changeCents) // 1000 - 950 = 50

        // Verify updated stock
        val updatedProd = database.productDao().getProductById(prodId)
        assertNotNull(updatedProd)
        assertEquals(8.0, updatedProd!!.currentStock, 0.001) // 10 - 2 = 8

        // Verify stock movement logged
        val movements = repository.allStockMovements.first()
        assertTrue(movements.any { it.productId == prodId && it.reason == "SALE" && it.quantityChange == -2.0 })
    }

    @Test
    fun testVoidSaleRestoresStock() = runBlocking {
        val prodId = repository.insertProduct(
            ProductEntity(
                name = "Test Biscuit",
                sellingPriceCents = 10000L,
                costPriceCents = 8000L,
                currentStock = 20.0
            )
        )
        val prod = database.productDao().getProductById(prodId)!!

        val cart = listOf(CartItem(product = prod, quantity = 5.0, unitPriceCents = prod.sellingPriceCents))
        val saleResult = repository.checkoutSale(
            items = cart,
            discountCents = 0L,
            paymentMethod = "CASH",
            amountPaidCents = 50000L,
            customerId = null,
            customerName = null
        )
        val sale = saleResult.getOrThrow()

        // Stock is now 15
        assertEquals(15.0, database.productDao().getProductById(prodId)!!.currentStock, 0.001)

        // Void the sale
        val voidResult = repository.voidSale(sale.id, "Customer return")
        assertTrue(voidResult.isSuccess)

        // Stock restored back to 20
        assertEquals(20.0, database.productDao().getProductById(prodId)!!.currentStock, 0.001)
        val voidedSale = database.saleDao().getSaleWithItemsById(sale.id)!!.sale
        assertTrue(voidedSale.isVoided)
    }

    @Test
    fun testCustomerCreditSaleAndRepayment() = runBlocking {
        val custId = repository.insertCustomer(
            CustomerEntity(name = "Sunil Silva", phone = "0711112233")
        )

        val prodId = repository.insertProduct(
            ProductEntity(
                name = "Rice 5kg",
                sellingPriceCents = 120000L,
                costPriceCents = 100000L,
                currentStock = 10.0
            )
        )
        val prod = database.productDao().getProductById(prodId)!!

        // Credit sale
        val cart = listOf(CartItem(product = prod, quantity = 1.0, unitPriceCents = prod.sellingPriceCents))
        val saleResult = repository.checkoutSale(
            items = cart,
            discountCents = 0L,
            paymentMethod = "CREDIT",
            amountPaidCents = 0L,
            customerId = custId,
            customerName = "Sunil Silva"
        )
        assertTrue(saleResult.isSuccess)

        // Customer balance should be 1200.00 (120000 cents)
        val balance = repository.getCustomerBalance(custId)
        assertEquals(120000L, balance)

        // Customer repays 500.00
        val repayResult = repository.recordCustomerRepayment(custId, 50000L, "Partial cash repayment")
        assertTrue(repayResult.isSuccess)

        // New balance is 700.00 (70000 cents)
        val newBalance = repository.getCustomerBalance(custId)
        assertEquals(70000L, newBalance)
    }

    @Test
    fun testBackupAndRestore() = runBlocking {
        repository.insertProduct(
            ProductEntity(
                name = "Sugar 1kg",
                sellingPriceCents = 25000L,
                costPriceCents = 20000L,
                currentStock = 30.0
            )
        )

        val jsonBackup = repository.exportBackupJson("My Test Shop")
        assertTrue(jsonBackup.contains("Sugar 1kg"))
        assertTrue(jsonBackup.contains("BizMate LK"))

        val restoreResult = repository.restoreBackupJson(jsonBackup)
        assertTrue(restoreResult.isSuccess)

        val restoredProducts = repository.allProducts.first()
        assertEquals(1, restoredProducts.size)
        assertEquals("Sugar 1kg", restoredProducts[0].name)
    }
}
