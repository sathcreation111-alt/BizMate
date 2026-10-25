package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BizMateViewModel
import com.example.ui.Screen
import com.example.ui.components.BizMateBackground
import com.example.ui.components.GlassCard
import com.example.ui.screens.CustomerCreditScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.LanguageSelectionScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReceiptDialog
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopNameSetupScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.BizMateTheme
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassCardBg
import com.example.ui.theme.LightLavender
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.Localization

class MainActivity : ComponentActivity() {
    private val viewModel: BizMateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BizMateTheme {
                BizMateApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BizMateApp(viewModel: BizMateViewModel) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsStateWithLifecycle()
    val shopName by viewModel.shopName.collectAsStateWithLifecycle()
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val lowStockCount by viewModel.lowStockCount.collectAsStateWithLifecycle()
    val salesWithItems by viewModel.allSalesWithItems.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val stockMovements by viewModel.allStockMovements.collectAsStateWithLifecycle()

    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val discountCents by viewModel.cartDiscountCents.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.cartPaymentMethod.collectAsStateWithLifecycle()
    val amountPaidCents by viewModel.cartAmountPaidCents.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val activeReceipt by viewModel.activeReceipt.collectAsStateWithLifecycle()

    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    // Calculate total outstanding credit
    var outstandingCreditCents by remember { mutableStateOf(0L) }
    LaunchedEffect(customers, salesWithItems) {
        var total = 0L
        for (c in customers) {
            total += viewModel.repository.getCustomerBalance(c.id)
        }
        outstandingCreditCents = total
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    // First-launch state control
    var onboardingStep by remember { mutableStateOf<String>("LANG") } // LANG or SHOP

    if (!isSetupCompleted) {
        if (onboardingStep == "LANG") {
            LanguageSelectionScreen(
                currentLang = language,
                onLanguageSelected = { viewModel.setLanguage(it) },
                onContinue = { onboardingStep = "SHOP" }
            )
        } else {
            ShopNameSetupScreen(
                language = language,
                onComplete = { enteredShopName ->
                    viewModel.completeOnboarding(enteredShopName, language)
                }
            )
        }
        return
    }

    // Back button handling
    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        viewModel.navigateBack()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color.Transparent,
        bottomBar = {
            // Show bottom navigation bar on primary tabs
            if (shouldShowBottomBar(currentScreen)) {
                GlassBottomNavBar(
                    currentScreen = currentScreen,
                    language = language,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(
                        shopName = shopName,
                        language = language,
                        salesWithItems = salesWithItems,
                        expenses = expenses,
                        lowStockCount = lowStockCount,
                        outstandingCreditCents = outstandingCreditCents,
                        onNavigate = { viewModel.navigateTo(it) },
                        onViewReceipt = { viewModel.showReceipt(it) }
                    )
                    Screen.Pos -> PosScreen(
                        language = language,
                        products = products,
                        customers = customers,
                        cart = cart,
                        discountCents = discountCents,
                        paymentMethod = paymentMethod,
                        amountPaidCents = amountPaidCents,
                        selectedCustomer = selectedCustomer,
                        onAddToCart = { viewModel.addToCart(it) },
                        onUpdateQty = { id, qty -> viewModel.updateCartItemQuantity(id, qty) },
                        onRemoveFromCart = { viewModel.removeFromCart(it) },
                        onClearCart = { viewModel.clearCart() },
                        onSetDiscount = { viewModel.setDiscount(it) },
                        onSetPaymentMethod = { viewModel.setPaymentMethod(it) },
                        onSetAmountPaid = { viewModel.setAmountPaid(it) },
                        onSelectCustomer = { viewModel.selectCustomer(it) },
                        onCheckout = { callback ->
                            viewModel.completeCheckout { saleWithItems ->
                                callback(saleWithItems)
                            }
                        },
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.Products -> ProductsScreen(
                        language = language,
                        products = products,
                        onSaveProduct = { prod, isEdit -> viewModel.saveProduct(prod, isEdit) },
                        onDeleteProduct = { viewModel.deleteProduct(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.Stock -> StockScreen(
                        language = language,
                        products = products,
                        movements = stockMovements,
                        onAdjustStock = { id: Long, delta: Double, reason: String -> viewModel.adjustStock(id, delta, reason) },
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.Expenses -> ExpensesScreen(
                        language = language,
                        expenses = expenses,
                        onSaveExpense = { exp, isEdit -> viewModel.saveExpense(exp, isEdit) },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.CreditLedger -> CustomerCreditScreen(
                        language = language,
                        customers = customers,
                        onGetBalance = { viewModel.repository.getCustomerBalance(it) },
                        onGetTransactions = { viewModel.repository.getTransactionsForCustomer(it) },
                        onSaveCustomer = { cust, isEdit -> viewModel.saveCustomer(cust, isEdit) },
                        onRecordRepayment = { id, amt, note -> viewModel.recordCustomerRepayment(id, amt, note) },
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.Reports -> ReportsScreen(
                        language = language,
                        salesWithItems = salesWithItems,
                        expenses = expenses,
                        outstandingCreditCents = outstandingCreditCents,
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.SalesHistory -> SalesHistoryScreen(
                        language = language,
                        salesWithItems = salesWithItems,
                        onSelectSale = { viewModel.showReceipt(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                    Screen.Settings, Screen.About -> SettingsScreen(
                        language = language,
                        shopName = shopName,
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onShopNameChange = { viewModel.setShopName(it) },
                        onExportBackup = { viewModel.exportBackup(it) },
                        onRestoreBackup = { viewModel.restoreBackup(it) },
                        onCallSupport = { viewModel.callSupport(it) },
                        onCopyNumber = { viewModel.copySupportNumber(it) },
                        onBack = { viewModel.navigateBack() }
                    )
                    else -> DashboardScreen(
                        shopName = shopName,
                        language = language,
                        salesWithItems = salesWithItems,
                        expenses = expenses,
                        lowStockCount = lowStockCount,
                        outstandingCreditCents = outstandingCreditCents,
                        onNavigate = { viewModel.navigateTo(it) },
                        onViewReceipt = { viewModel.showReceipt(it) }
                    )
                }
            }

            // Global Itemized Receipt Dialog
            if (activeReceipt != null) {
                ReceiptDialog(
                    saleWithItems = activeReceipt!!,
                    shopName = shopName,
                    language = language,
                    onDismiss = { viewModel.closeReceipt() },
                    onVoidSale = { id, reason -> viewModel.voidSale(id, reason) },
                    onShare = { receiptText -> viewModel.shareReceipt(context, receiptText) }
                )
            }
        }
    }
}

private fun shouldShowBottomBar(screen: Screen): Boolean {
    return when (screen) {
        Screen.Dashboard, Screen.Pos, Screen.Products, Screen.Reports, Screen.Settings -> true
        else -> false
    }
}

@Composable
private fun GlassBottomNavBar(
    currentScreen: Screen,
    language: String,
    onNavigate: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            backgroundColor = Color(0xF21F1035),
            borderColor = BrightLavender.copy(alpha = 0.3f),
            borderWidth = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    icon = Icons.Default.Home,
                    label = Localization.get(Localization.Key.NAV_DASHBOARD, language),
                    isSelected = currentScreen == Screen.Dashboard,
                    testTag = "nav_dashboard",
                    onClick = { onNavigate(Screen.Dashboard) }
                )
                NavBarItem(
                    icon = Icons.Default.PointOfSale,
                    label = Localization.get(Localization.Key.NAV_POS, language),
                    isSelected = currentScreen == Screen.Pos,
                    testTag = "nav_pos",
                    onClick = { onNavigate(Screen.Pos) }
                )
                NavBarItem(
                    icon = Icons.Default.ShoppingBag,
                    label = Localization.get(Localization.Key.NAV_PRODUCTS, language),
                    isSelected = currentScreen == Screen.Products,
                    testTag = "nav_products",
                    onClick = { onNavigate(Screen.Products) }
                )
                NavBarItem(
                    icon = Icons.Default.Assessment,
                    label = Localization.get(Localization.Key.NAV_REPORTS, language),
                    isSelected = currentScreen == Screen.Reports,
                    testTag = "nav_reports",
                    onClick = { onNavigate(Screen.Reports) }
                )
                NavBarItem(
                    icon = Icons.Default.Settings,
                    label = Localization.get(Localization.Key.NAV_SETTINGS, language),
                    isSelected = currentScreen == Screen.Settings,
                    testTag = "nav_settings",
                    onClick = { onNavigate(Screen.Settings) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) PrimaryViolet else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) LightLavender else TextMuted
        )
    }
}
