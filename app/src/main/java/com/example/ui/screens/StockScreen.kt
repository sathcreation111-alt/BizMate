package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.ui.components.BizMateBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDialog
import com.example.ui.components.GlassTextField
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
import java.util.Date
import java.util.Locale

@Composable
fun StockScreen(
    language: String,
    products: List<ProductEntity>,
    movements: List<StockMovementEntity>,
    onAdjustStock: (Long, Double, String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Inventory, 1: Movement History
    var searchQuery by remember { mutableStateOf("") }
    var adjustingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        products.filter { p ->
            searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
        }
    }

    BizMateBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("stock_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Localization.get(Localization.Key.STOCK_MANAGEMENT, language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Tabs: Current Stock vs Movement History
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BrightLavender,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BrightLavender
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = Localization.get(Localization.Key.NAV_STOCK, language),
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color.White else TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = Localization.get(Localization.Key.STOCK_HISTORY, language),
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color.White else TextSecondary
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Inventory Tab
                    GlassTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = Localization.get(Localization.Key.SEARCH_PRODUCTS, language),
                        leadingIcon = Icons.Default.Search,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            StockItemCard(
                                product = product,
                                language = language,
                                onAdjust = { adjustingProduct = product }
                            )
                        }
                    }
                } else {
                    // Movement History Tab
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (movements.isEmpty()) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No stock movements recorded yet",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(movements, key = { it.id }) { movement ->
                                StockMovementRow(movement = movement, language = language)
                            }
                        }
                    }
                }
            }

            // Stock Adjustment Dialog
            if (adjustingProduct != null) {
                StockAdjustmentDialog(
                    product = adjustingProduct!!,
                    language = language,
                    onDismiss = { adjustingProduct = null },
                    onConfirm = { delta, reason ->
                        onAdjustStock(adjustingProduct!!.id, delta, reason)
                        adjustingProduct = null
                    }
                )
            }
        }
    }
}

@Composable
private fun StockItemCard(
    product: ProductEntity,
    language: String,
    onAdjust: () -> Unit
) {
    val isOutOfStock = product.currentStock <= 0.0
    val isLowStock = product.currentStock <= product.lowStockThreshold && !isOutOfStock

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.category} • Alert below ${LkrFormatter.formatQuantity(product.lowStockThreshold, product.unit)}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Stock status
                val (badgeText, badgeColor) = when {
                    isOutOfStock -> Localization.get(Localization.Key.OUT_OF_STOCK, language) to StatusError
                    isLowStock -> Localization.get(Localization.Key.LOW_STOCK, language) to StatusWarning
                    else -> Localization.get(Localization.Key.IN_STOCK, language) to StatusSuccess
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${Localization.get(Localization.Key.CURRENT_STOCK, language)}: ${LkrFormatter.formatQuantity(product.currentStock, product.unit)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }
            }

            Button(
                onClick = onAdjust,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = Localization.get(Localization.Key.ADJUST_STOCK, language),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StockMovementRow(
    movement: StockMovementEntity,
    language: String
) {
    val isPositive = movement.quantityChange > 0
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(movement.timestamp))

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPositive) StatusSuccess.copy(alpha = 0.2f)
                            else StatusError.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.Add else Icons.Default.Remove,
                        contentDescription = null,
                        tint = if (isPositive) StatusSuccess else StatusError,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = movement.productName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${movement.reason} • $formattedDate",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}${LkrFormatter.formatQuantity(movement.quantityChange)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPositive) StatusSuccess else StatusError
                )
                Text(
                    text = "Bal: ${LkrFormatter.formatQuantity(movement.newStockLevel)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun StockAdjustmentDialog(
    product: ProductEntity,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var isAdd by remember { mutableStateOf(true) }
    var qtyText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf(Localization.get(Localization.Key.REASON_RESTOCK, language)) }

    val reasons = listOf(
        Localization.get(Localization.Key.REASON_RESTOCK, language),
        Localization.get(Localization.Key.REASON_DAMAGE, language),
        Localization.get(Localization.Key.REASON_COUNT_CORRECTION, language)
    )

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${Localization.get(Localization.Key.ADJUST_STOCK, language)}: ${product.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Localization.get(Localization.Key.CURRENT_STOCK, language)}: ${LkrFormatter.formatQuantity(product.currentStock, product.unit)}",
                fontSize = 13.sp,
                color = AccentLavender
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add or Deduct Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isAdd = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdd) StatusSuccess else Color(0xFF241438)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Restock / Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isAdd = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAdd) StatusError else Color(0xFF241438)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("- Deduct / Loss", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                label = Localization.get(Localization.Key.QUANTITY_CHANGE, language),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Reason chips
            Text(
                text = Localization.get(Localization.Key.ADJUSTMENT_REASON, language),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(reasons) { r ->
                    val isSelected = reason == r
                    FilterChip(
                        selected = isSelected,
                        onClick = { reason = r },
                        label = { Text(r, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF241438),
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightLavender),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Text(Localization.get(Localization.Key.CANCEL, language))
                }
                Button(
                    onClick = {
                        val qty = qtyText.toDoubleOrNull() ?: 0.0
                        if (qty > 0) {
                            val delta = if (isAdd) qty else -qty
                            onConfirm(delta, reason)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                ) {
                    Text(Localization.get(Localization.Key.SAVE, language), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
