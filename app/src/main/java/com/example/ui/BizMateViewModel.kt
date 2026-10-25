package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BizMateDatabase
import com.example.data.local.dao.SaleWithItems
import com.example.data.local.entity.CustomerCreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.BizMateRepository
import com.example.data.repository.CartItem
import com.example.util.Localization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object LanguageSelection : Screen()
    object ShopNameSetup : Screen()
    object Dashboard : Screen()
    object Pos : Screen()
    object Products : Screen()
    object Stock : Screen()
    object Expenses : Screen()
    object CreditLedger : Screen()
    object Reports : Screen()
    object SalesHistory : Screen()
    object Settings : Screen()
    object About : Screen()
}

class BizMateViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BizMateDatabase.getDatabase(application)
    val repository = BizMateRepository(database)
    val preferencesManager = PreferencesManager(application)

    val language: StateFlow<String> = preferencesManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "si")

    val shopName: StateFlow<String> = preferencesManager.shopNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "BizMate Store")

    val isSetupCompleted: StateFlow<Boolean> = preferencesManager.isSetupCompletedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenBackStack = mutableListOf<Screen>()

    // Products
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockCount: StateFlow<Int> = repository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sales
    val allSalesWithItems: StateFlow<List<SaleWithItems>> = repository.allSalesWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSales: StateFlow<List<SaleEntity>> = repository.recentSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses
    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customers
    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stock Movements
    val allStockMovements: StateFlow<List<StockMovementEntity>> = repository.allStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cartDiscountCents = MutableStateFlow(0L)
    val cartDiscountCents: StateFlow<Long> = _cartDiscountCents.asStateFlow()

    private val _cartPaymentMethod = MutableStateFlow("CASH") // CASH, CARD, CREDIT, OTHER
    val cartPaymentMethod: StateFlow<String> = _cartPaymentMethod.asStateFlow()

    private val _cartAmountPaidCents = MutableStateFlow(0L)
    val cartAmountPaidCents: StateFlow<Long> = _cartAmountPaidCents.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    // Active Receipt Dialog
    private val _activeReceipt = MutableStateFlow<SaleWithItems?>(null)
    val activeReceipt: StateFlow<SaleWithItems?> = _activeReceipt.asStateFlow()

    // Notification toast/message
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialSampleProductsIfNeeded()
        }
    }

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenBackStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        return if (screenBackStack.isNotEmpty()) {
            _currentScreen.value = screenBackStack.removeAt(screenBackStack.size - 1)
            true
        } else if (_currentScreen.value != Screen.Dashboard && isSetupCompleted.value) {
            _currentScreen.value = Screen.Dashboard
            true
        } else {
            false
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
        }
    }

    fun setShopName(name: String) {
        viewModelScope.launch {
            preferencesManager.setShopName(name)
        }
    }

    fun completeOnboarding(shopName: String, lang: String) {
        viewModelScope.launch {
            preferencesManager.completeSetup(shopName, lang)
            _currentScreen.value = Screen.Dashboard
        }
    }

    // POS / Cart operations
    fun addToCart(product: ProductEntity, quantity: Double = 1.0) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = currentList[index]
            val newQty = existing.quantity + quantity
            if (newQty <= product.currentStock) {
                currentList[index] = existing.copy(quantity = newQty)
                _cart.value = currentList
            } else {
                showFeedback(Localization.get(Localization.Key.INSUFFICIENT_STOCK, language.value))
            }
        } else {
            if (quantity <= product.currentStock) {
                currentList.add(
                    CartItem(
                        product = product,
                        quantity = quantity,
                        unitPriceCents = product.sellingPriceCents
                    )
                )
                _cart.value = currentList
            } else {
                showFeedback(Localization.get(Localization.Key.INSUFFICIENT_STOCK, language.value))
            }
        }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0.0) {
                currentList.removeAt(index)
            } else {
                val item = currentList[index]
                if (newQuantity <= item.product.currentStock) {
                    currentList[index] = item.copy(quantity = newQuantity)
                } else {
                    showFeedback(Localization.get(Localization.Key.INSUFFICIENT_STOCK, language.value))
                }
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _cartDiscountCents.value = 0L
        _cartPaymentMethod.value = "CASH"
        _cartAmountPaidCents.value = 0L
        _selectedCustomer.value = null
    }

    fun setDiscount(discountCents: Long) {
        _cartDiscountCents.value = discountCents.coerceAtLeast(0L)
    }

    fun setPaymentMethod(method: String) {
        _cartPaymentMethod.value = method
    }

    fun setAmountPaid(cents: Long) {
        _cartAmountPaidCents.value = cents
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun completeCheckout(onSuccess: (SaleWithItems) -> Unit) {
        val items = _cart.value
        if (items.isEmpty()) return

        val subtotal = items.sumOf { it.totalCents }
        val discount = _cartDiscountCents.value.coerceIn(0L, subtotal)
        val total = (subtotal - discount).coerceAtLeast(0L)
        val method = _cartPaymentMethod.value
        val amountPaid = _cartAmountPaidCents.value

        if (method == "CASH" && amountPaid < total) {
            showFeedback(Localization.get(Localization.Key.INVALID_AMOUNT, language.value))
            return
        }

        if (method == "CREDIT" && _selectedCustomer.value == null) {
            showFeedback(Localization.get(Localization.Key.SELECT_CUSTOMER, language.value))
            return
        }

        viewModelScope.launch {
            val result = repository.checkoutSale(
                items = items,
                discountCents = discount,
                paymentMethod = method,
                amountPaidCents = if (method == "CASH") amountPaid else total,
                customerId = _selectedCustomer.value?.id,
                customerName = _selectedCustomer.value?.name
            )

            result.onSuccess { sale ->
                clearCart()
                // Fetch complete sale with items to show receipt
                val saleWithItems = database.saleDao().getSaleWithItemsById(sale.id)
                if (saleWithItems != null) {
                    _activeReceipt.value = saleWithItems
                    onSuccess(saleWithItems)
                }
                showFeedback(Localization.get(Localization.Key.SALE_SUCCESS, language.value))
            }.onFailure { err ->
                showFeedback(err.message ?: "Failed to complete sale")
            }
        }
    }

    fun showReceipt(saleWithItems: SaleWithItems) {
        _activeReceipt.value = saleWithItems
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    fun voidSale(saleId: Long, reason: String) {
        viewModelScope.launch {
            val result = repository.voidSale(saleId, reason)
            result.onSuccess {
                val updated = database.saleDao().getSaleWithItemsById(saleId)
                if (_activeReceipt.value?.sale?.id == saleId) {
                    _activeReceipt.value = updated
                }
                showFeedback("Sale voided successfully")
            }.onFailure { err ->
                showFeedback(err.message ?: "Failed to void sale")
            }
        }
    }

    // Product actions
    fun saveProduct(product: ProductEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                repository.updateProduct(product)
            } else {
                repository.insertProduct(product)
            }
            showFeedback("Product saved successfully")
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showFeedback("Product deleted")
        }
    }

    fun adjustStock(productId: Long, delta: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(productId, delta, reason)
            showFeedback("Stock updated")
        }
    }

    // Expense actions
    fun saveExpense(expense: ExpenseEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                repository.updateExpense(expense)
            } else {
                repository.insertExpense(expense)
            }
            showFeedback("Expense saved")
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            showFeedback("Expense deleted")
        }
    }

    // Customer & Credit actions
    fun saveCustomer(customer: CustomerEntity, isEdit: Boolean) {
        viewModelScope.launch {
            if (isEdit) {
                repository.updateCustomer(customer)
            } else {
                repository.insertCustomer(customer)
            }
            showFeedback("Customer saved")
        }
    }

    fun recordCustomerRepayment(customerId: Long, amountCents: Long, note: String?) {
        viewModelScope.launch {
            val result = repository.recordCustomerRepayment(customerId, amountCents, note)
            result.onSuccess {
                showFeedback("Repayment recorded")
            }.onFailure { err ->
                showFeedback(err.message ?: "Failed to record repayment")
            }
        }
    }

    // Backup & Restore
    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportBackupJson(shopName.value)
                onExportReady(json)
                showFeedback(Localization.get(Localization.Key.BACKUP_SUCCESS, language.value))
            } catch (e: Exception) {
                showFeedback("Export failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            val result = repository.restoreBackupJson(jsonString)
            result.onSuccess {
                showFeedback(Localization.get(Localization.Key.RESTORE_SUCCESS, language.value))
            }.onFailure { err ->
                showFeedback("Restore failed: ${err.message}")
            }
        }
    }

    // Call and Copy Support actions
    fun callSupport(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:0704442112")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            showFeedback("Cannot open phone dialer")
        }
    }

    fun copySupportNumber(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Support Number", "070 444 2112")
            clipboard.setPrimaryClip(clip)
            showFeedback(Localization.get(Localization.Key.NUMBER_COPIED, language.value))
            Toast.makeText(context, Localization.get(Localization.Key.NUMBER_COPIED, language.value), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            showFeedback("Failed to copy number")
        }
    }

    fun shareReceipt(context: Context, textReceipt: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, textReceipt)
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Receipt")
            shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            showFeedback("Unable to share receipt")
        }
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
