package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.SaleWithItems
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.components.BizMateBackground
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
import java.util.Calendar

data class TopProductStat(
    val name: String,
    val totalQty: Double,
    val totalRevenueCents: Long
)

@Composable
fun ReportsScreen(
    language: String,
    salesWithItems: List<SaleWithItems>,
    expenses: List<ExpenseEntity>,
    outstandingCreditCents: Long,
    onBack: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("TODAY") } // TODAY, YESTERDAY, WEEK, MONTH, ALL

    val (startTime, endTime) = remember(selectedPeriod) {
        val now = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (selectedPeriod) {
            "YESTERDAY" -> {
                startCal.add(Calendar.DAY_OF_YEAR, -1)
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = startCal.timeInMillis
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                startCal.timeInMillis to endCal.timeInMillis
            }
            "WEEK" -> {
                startCal.add(Calendar.DAY_OF_YEAR, -6)
                startCal.timeInMillis to now.timeInMillis
            }
            "MONTH" -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.timeInMillis to now.timeInMillis
            }
            "ALL" -> 0L to Long.MAX_VALUE
            else -> { // TODAY
                startCal.timeInMillis to now.timeInMillis
            }
        }
    }

    val periodSales = remember(salesWithItems, startTime, endTime) {
        salesWithItems.filter { !it.sale.isVoided && it.sale.timestamp in startTime..endTime }
    }

    val periodExpenses = remember(expenses, startTime, endTime) {
        expenses.filter { it.dateTimestamp in startTime..endTime }
    }

    val totalRevenueCents = remember(periodSales) {
        periodSales.sumOf { it.sale.totalCents }
    }

    val totalCogsCents = remember(periodSales) {
        periodSales.sumOf { saleWithItems ->
            saleWithItems.items.sumOf { item ->
                (item.quantity * item.costPriceCents).toLong()
            }
        }
    }

    val grossProfitCents = remember(totalRevenueCents, totalCogsCents) {
        totalRevenueCents - totalCogsCents
    }

    val totalExpensesCents = remember(periodExpenses) {
        periodExpenses.sumOf { it.amountCents }
    }

    val netProfitEstimateCents = remember(grossProfitCents, totalExpensesCents) {
        grossProfitCents - totalExpensesCents
    }

    val topProducts = remember(periodSales) {
        val map = mutableMapOf<String, Pair<Double, Long>>()
        for (sale in periodSales) {
            for (item in sale.items) {
                val current = map[item.productName] ?: (0.0 to 0L)
                map[item.productName] = (current.first + item.quantity) to (current.second + item.totalCents)
            }
        }
        map.map { TopProductStat(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.totalRevenueCents }
            .take(6)
    }

    val periods = listOf(
        "TODAY" to Localization.get(Localization.Key.PERIOD_TODAY, language),
        "YESTERDAY" to Localization.get(Localization.Key.PERIOD_YESTERDAY, language),
        "WEEK" to Localization.get(Localization.Key.PERIOD_THIS_WEEK, language),
        "MONTH" to Localization.get(Localization.Key.PERIOD_THIS_MONTH, language),
        "ALL" to Localization.get(Localization.Key.PERIOD_ALL_TIME, language)
    )

    BizMateBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reports_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Localization.get(Localization.Key.REPORTS_TITLE, language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Period selector chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(periods) { (key, label) ->
                        val isSelected = selectedPeriod == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryViolet,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF241438),
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Main Profit & Loss Statement Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = Color(0xFF26123D),
                    borderColor = BrightLavender.copy(alpha = 0.5f),
                    borderWidth = 1.5.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Localization.get(Localization.Key.NET_PROFIT, language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = LightLavender
                            )
                            Surface(
                                color = if (netProfitEstimateCents >= 0) StatusSuccess.copy(alpha = 0.2f) else StatusError.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (netProfitEstimateCents >= 0) "Profit" else "Loss",
                                    color = if (netProfitEstimateCents >= 0) StatusSuccess else StatusError,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = LkrFormatter.format(netProfitEstimateCents, language),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netProfitEstimateCents >= 0) StatusSuccess else StatusError
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Breakdown lines
                        BreakdownRow(
                            label = Localization.get(Localization.Key.TOTAL_REVENUE, language),
                            value = LkrFormatter.format(totalRevenueCents, language),
                            valueColor = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownRow(
                            label = Localization.get(Localization.Key.COGS, language),
                            value = "-${LkrFormatter.format(totalCogsCents, language)}",
                            valueColor = TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownRow(
                            label = Localization.get(Localization.Key.GROSS_PROFIT, language),
                            value = LkrFormatter.format(grossProfitCents, language),
                            valueColor = AccentLavender
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownRow(
                            label = Localization.get(Localization.Key.RECORDED_EXPENSES, language),
                            value = "-${LkrFormatter.format(totalExpensesCents, language)}",
                            valueColor = StatusError
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Stats row (Orders count & Outstanding Credit)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(Localization.get(Localization.Key.TOTAL_ORDERS, language), fontSize = 11.sp, color = TextSecondary)
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = BrightLavender, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${periodSales.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                    }

                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(Localization.get(Localization.Key.CREDIT_SUMMARY, language), fontSize = 11.sp, color = TextSecondary)
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = LkrFormatter.format(outstandingCreditCents, language),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentLavender
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Top Selling Products
            item {
                Text(
                    text = Localization.get(Localization.Key.TOP_SELLING_PRODUCTS, language),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (topProducts.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No sales recorded for this period",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(topProducts) { item ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${LkrFormatter.formatQuantity(item.totalQty)} sold",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = LkrFormatter.format(item.totalRevenueCents, language),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AccentLavender
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
