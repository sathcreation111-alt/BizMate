package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.SaleWithItems
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.ui.Screen
import com.example.ui.components.BizMateBackground
import com.example.ui.components.BrandHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.LightLavender
import com.example.ui.theme.PrimaryViolet
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LkrFormatter
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    shopName: String,
    language: String,
    salesWithItems: List<SaleWithItems>,
    expenses: List<ExpenseEntity>,
    lowStockCount: Int,
    outstandingCreditCents: Long,
    onNavigate: (Screen) -> Unit,
    onViewReceipt: (SaleWithItems) -> Unit
) {
    // Calculate today's time range
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis

    val todaySalesWithItems = remember(salesWithItems, startOfToday) {
        salesWithItems.filter { !it.sale.isVoided && it.sale.timestamp >= startOfToday }
    }

    val todaySalesCents = remember(todaySalesWithItems) {
        todaySalesWithItems.sumOf { it.sale.totalCents }
    }

    val todayCogsCents = remember(todaySalesWithItems) {
        todaySalesWithItems.sumOf { saleWithItems ->
            saleWithItems.items.sumOf { item ->
                (item.quantity * item.costPriceCents).toLong()
            }
        }
    }

    val todayGrossProfitCents = remember(todaySalesCents, todayCogsCents) {
        todaySalesCents - todayCogsCents
    }

    val todayExpensesCents = remember(expenses, startOfToday) {
        expenses.filter { it.dateTimestamp >= startOfToday }.sumOf { it.amountCents }
    }

    val todayNetEstimateCents = remember(todayGrossProfitCents, todayExpensesCents) {
        todayGrossProfitCents - todayExpensesCents
    }

    val recentSalesList = remember(salesWithItems) {
        salesWithItems.take(5)
    }

    BizMateBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // Header bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = shopName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = Localization.get(Localization.Key.NAV_DASHBOARD, language),
                            fontSize = 13.sp,
                            color = AccentLavender
                        )
                    }
                    BrandHeader()
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Primary KPI Hero Cards (Grid of Today's Sales and Profit)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Today's Sales Card
                    KpiStatCard(
                        title = Localization.get(Localization.Key.TODAY_SALES, language),
                        amountFormatted = LkrFormatter.format(todaySalesCents, language),
                        subtitle = "${todaySalesWithItems.size} ${if (language == "si") "ගනුදෙනු" else "Orders"}",
                        icon = Icons.Default.TrendingUp,
                        iconTint = StatusSuccess,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_today_sales"
                    )

                    // Estimated Gross Profit Card
                    KpiStatCard(
                        title = Localization.get(Localization.Key.GROSS_PROFIT, language),
                        amountFormatted = LkrFormatter.format(todayGrossProfitCents, language),
                        subtitle = "${Localization.get(Localization.Key.NET_ESTIMATE, language)}: ${LkrFormatter.format(todayNetEstimateCents, language)}",
                        icon = Icons.Default.MonetizationOn,
                        iconTint = AccentLavender,
                        modifier = Modifier.weight(1f),
                        testTag = "kpi_gross_profit"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Secondary KPI Row: Expenses, Low Stock, Outstanding Credit
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Today Expenses
                    MiniKpiCard(
                        title = Localization.get(Localization.Key.TODAY_EXPENSES, language),
                        value = LkrFormatter.format(todayExpensesCents, language),
                        icon = Icons.Default.TrendingDown,
                        iconTint = StatusError,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(Screen.Expenses) },
                        testTag = "kpi_today_expenses"
                    )

                    // Low Stock Alert
                    MiniKpiCard(
                        title = Localization.get(Localization.Key.LOW_STOCK_ITEMS, language),
                        value = "$lowStockCount ${if (language == "si") "භාණ්ඩ" else "Items"}",
                        icon = Icons.Default.Warning,
                        iconTint = if (lowStockCount > 0) StatusWarning else StatusSuccess,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(Screen.Stock) },
                        testTag = "kpi_low_stock"
                    )

                    // Outstanding Credit
                    MiniKpiCard(
                        title = Localization.get(Localization.Key.OUTSTANDING_CREDIT, language),
                        value = LkrFormatter.format(outstandingCreditCents, language),
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = BrightLavender,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(Screen.CreditLedger) },
                        testTag = "kpi_outstanding_credit"
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick Actions Section
            item {
                Text(
                    text = Localization.get(Localization.Key.QUICK_ACTIONS, language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionItem(
                        icon = Icons.Default.PointOfSale,
                        label = Localization.get(Localization.Key.NEW_SALE, language),
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_pos",
                        onClick = { onNavigate(Screen.Pos) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.ShoppingBag,
                        label = Localization.get(Localization.Key.NAV_PRODUCTS, language),
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_products",
                        onClick = { onNavigate(Screen.Products) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Inventory,
                        label = Localization.get(Localization.Key.NAV_STOCK, language),
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_stock",
                        onClick = { onNavigate(Screen.Stock) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionItem(
                        icon = Icons.Default.AttachMoney,
                        label = Localization.get(Localization.Key.NAV_EXPENSES, language),
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_expenses",
                        onClick = { onNavigate(Screen.Expenses) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.People,
                        label = Localization.get(Localization.Key.NAV_CREDIT, language),
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_credit",
                        onClick = { onNavigate(Screen.CreditLedger) }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Assessment,
                        label = Localization.get(Localization.Key.NAV_REPORTS, language),
                        modifier = Modifier.weight(1f),
                        testTag = "quick_action_reports",
                        onClick = { onNavigate(Screen.Reports) }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Recent Transactions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Localization.get(Localization.Key.RECENT_TRANSACTIONS, language),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = Localization.get(Localization.Key.VIEW_ALL, language),
                        fontSize = 13.sp,
                        color = AccentLavender,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigate(Screen.SalesHistory) }
                            .padding(4.dp)
                            .testTag("view_all_sales_button")
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (recentSalesList.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Localization.get(Localization.Key.NO_RECENT_TRANSACTIONS, language),
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(recentSalesList, key = { it.sale.id }) { saleWithItems ->
                    RecentSaleRow(
                        saleWithItems = saleWithItems,
                        language = language,
                        onClick = { onViewReceipt(saleWithItems) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    amountFormatted: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    GlassCard(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        borderWidth = 1.dp,
        borderColor = BrightLavender.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = amountFormatted,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun MiniKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    testTag: String
) {
    GlassCard(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = if (isPrimary) Color(0xFF371D5C) else Color(0xFF241438),
        borderColor = if (isPrimary) BrightLavender.copy(alpha = 0.7f) else GlassBorder,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPrimary) PrimaryViolet else Color(0xFF2F1A48)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isPrimary) Color.White else BrightLavender,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentSaleRow(
    saleWithItems: SaleWithItems,
    language: String,
    onClick: () -> Unit
) {
    val sale = saleWithItems.sale
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    val formattedTime = timeFormat.format(Date(sale.timestamp))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (sale.isVoided) StatusError.copy(alpha = 0.2f)
                            else PrimaryViolet.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = if (sale.isVoided) StatusError else BrightLavender,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sale.invoiceNumber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (sale.isVoided) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VOID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusError
                            )
                        }
                    }
                    Text(
                        text = "$formattedTime • ${sale.paymentMethod} • ${saleWithItems.items.size} items",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LkrFormatter.format(sale.totalCents, language),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sale.isVoided) StatusError else TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
