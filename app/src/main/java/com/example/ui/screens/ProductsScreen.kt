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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.ui.components.BizMateBackground
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDialog
import com.example.ui.components.GlassTextField
import com.example.ui.components.GradientButton
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

@Composable
fun ProductsScreen(
    language: String,
    products: List<ProductEntity>,
    onSaveProduct: (ProductEntity, Boolean) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf("ALL") } // ALL, LOW, OUT
    var sortBy by remember { mutableStateOf("NAME") } // NAME, PRICE_ASC, PRICE_DESC, STOCK
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(products, searchQuery, stockFilter, sortBy) {
        var list = products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    (p.barcode != null && p.barcode.contains(searchQuery, ignoreCase = true)) ||
                    p.category.contains(searchQuery, ignoreCase = true)

            val matchesStock = when (stockFilter) {
                "LOW" -> p.currentStock <= p.lowStockThreshold && p.currentStock > 0
                "OUT" -> p.currentStock <= 0
                else -> true
            }
            matchesSearch && matchesStock
        }

        list = when (sortBy) {
            "PRICE_ASC" -> list.sortedBy { it.sellingPriceCents }
            "PRICE_DESC" -> list.sortedByDescending { it.sellingPriceCents }
            "STOCK" -> list.sortedBy { it.currentStock }
            else -> list.sortedBy { it.name.lowercase() }
        }
        list
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("products_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.get(Localization.Key.PRODUCTS_TITLE, language),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    GradientButton(
                        text = Localization.get(Localization.Key.ADD_PRODUCT, language),
                        icon = Icons.Default.Add,
                        onClick = { isCreatingNew = true },
                        testTag = "add_product_button",
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Search
                GlassTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = Localization.get(Localization.Key.SEARCH_PRODUCTS, language),
                    leadingIcon = Icons.Default.Search,
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "products_search_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filters Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = stockFilter == "ALL",
                            onClick = { stockFilter = "ALL" },
                            label = { Text(Localization.get(Localization.Key.FILTER_ALL, language), fontSize = 11.sp) },
                            colors = filterColors(stockFilter == "ALL")
                        )
                    }
                    item {
                        FilterChip(
                            selected = stockFilter == "LOW",
                            onClick = { stockFilter = "LOW" },
                            label = { Text(Localization.get(Localization.Key.FILTER_LOW_STOCK, language), fontSize = 11.sp) },
                            colors = filterColors(stockFilter == "LOW")
                        )
                    }
                    item {
                        FilterChip(
                            selected = stockFilter == "OUT",
                            onClick = { stockFilter = "OUT" },
                            label = { Text(Localization.get(Localization.Key.FILTER_OUT_OF_STOCK, language), fontSize = 11.sp) },
                            colors = filterColors(stockFilter == "OUT")
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortBy == "NAME",
                            onClick = { sortBy = if (sortBy == "NAME") "PRICE_DESC" else "NAME" },
                            label = { Text("Sort: $sortBy", fontSize = 11.sp) },
                            colors = filterColors(false)
                        )
                    }
                }

                // Products List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredProducts.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = Localization.get(Localization.Key.NO_PRODUCTS_FOUND, language),
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredProducts, key = { it.id }) { product ->
                            ProductCardRow(
                                product = product,
                                language = language,
                                onEdit = { editingProduct = product },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            }

            // Edit / Add Dialog
            if (isCreatingNew || editingProduct != null) {
                ProductEditorDialog(
                    initialProduct = editingProduct,
                    language = language,
                    onDismiss = {
                        isCreatingNew = false
                        editingProduct = null
                    },
                    onSave = { prod, isEdit ->
                        onSaveProduct(prod, isEdit)
                        isCreatingNew = false
                        editingProduct = null
                    }
                )
            }

            // Delete Confirmation Dialog
            if (productToDelete != null) {
                ConfirmationDialog(
                    title = Localization.get(Localization.Key.CONFIRM_DELETE_PRODUCT, language),
                    message = "${Localization.get(Localization.Key.CONFIRM_DELETE_MSG, language)}\n\"${productToDelete?.name}\"",
                    confirmText = Localization.get(Localization.Key.DELETE_PRODUCT, language),
                    cancelText = Localization.get(Localization.Key.CANCEL, language),
                    onConfirm = {
                        productToDelete?.let { onDeleteProduct(it) }
                        productToDelete = null
                    },
                    onDismiss = { productToDelete = null }
                )
            }
        }
    }
}

@Composable
private fun filterColors(isSelected: Boolean) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = PrimaryViolet,
    selectedLabelColor = Color.White,
    containerColor = Color(0xFF241438),
    labelColor = TextSecondary
)

@Composable
private fun ProductCardRow(
    product: ProductEntity,
    language: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isOutOfStock = product.currentStock <= 0.0
    val isLowStock = product.currentStock <= product.lowStockThreshold && !isOutOfStock

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_row_${product.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "${product.category}${if (!product.barcode.isNullOrBlank()) " • ${product.barcode}" else ""}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrightLavender, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prices
                Column {
                    Text(
                        text = "${Localization.get(Localization.Key.SELLING_PRICE, language)}: ${LkrFormatter.format(product.sellingPriceCents, language)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentLavender
                    )
                    Text(
                        text = "${Localization.get(Localization.Key.COST_PRICE, language)}: ${LkrFormatter.format(product.costPriceCents, language)}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Stock Badge
                val (stockText, stockColor) = when {
                    isOutOfStock -> Localization.get(Localization.Key.OUT_OF_STOCK, language) to StatusError
                    isLowStock -> "${Localization.get(Localization.Key.LOW_STOCK, language)}: ${LkrFormatter.formatQuantity(product.currentStock, product.unit)}" to StatusWarning
                    else -> "${Localization.get(Localization.Key.IN_STOCK, language)}: ${LkrFormatter.formatQuantity(product.currentStock, product.unit)}" to StatusSuccess
                }

                Surface(
                    color = stockColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, stockColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stockText,
                        color = stockColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    initialProduct: ProductEntity?,
    language: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, Boolean) -> Unit
) {
    val isEdit = initialProduct != null
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "General") }
    var sellingPriceText by remember {
        mutableStateOf(if (initialProduct != null) (initialProduct.sellingPriceCents / 100.0).toString() else "")
    }
    var costPriceText by remember {
        mutableStateOf(if (initialProduct != null) (initialProduct.costPriceCents / 100.0).toString() else "")
    }
    var stockText by remember {
        mutableStateOf(if (initialProduct != null) initialProduct.currentStock.toString() else "10")
    }
    var thresholdText by remember {
        mutableStateOf(if (initialProduct != null) initialProduct.lowStockThreshold.toString() else "5")
    }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEdit) Localization.get(Localization.Key.EDIT_PRODUCT, language)
                else Localization.get(Localization.Key.ADD_PRODUCT, language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            GlassTextField(
                value = name,
                onValueChange = { name = it },
                label = Localization.get(Localization.Key.PRODUCT_NAME, language),
                isError = errorMessage != null && name.isBlank()
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassTextField(
                value = category,
                onValueChange = { category = it },
                label = Localization.get(Localization.Key.CATEGORY, language)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(
                    value = sellingPriceText,
                    onValueChange = { sellingPriceText = it },
                    label = Localization.get(Localization.Key.SELLING_PRICE, language),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GlassTextField(
                    value = costPriceText,
                    onValueChange = { costPriceText = it },
                    label = Localization.get(Localization.Key.COST_PRICE, language),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = Localization.get(Localization.Key.CURRENT_STOCK, language),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GlassTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = Localization.get(Localization.Key.UNIT, language),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(
                    value = thresholdText,
                    onValueChange = { thresholdText = it },
                    label = Localization.get(Localization.Key.LOW_STOCK_ALERT_LEVEL, language),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                GlassTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = Localization.get(Localization.Key.BARCODE_SKU, language),
                    modifier = Modifier.weight(1f)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage!!, color = StatusError, fontSize = 12.sp)
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
                        if (name.isBlank()) {
                            errorMessage = "Name is required"
                            return@Button
                        }
                        val sellingPriceCents = LkrFormatter.parseToCents(sellingPriceText)
                        val costPriceCents = LkrFormatter.parseToCents(costPriceText)
                        val stock = stockText.toDoubleOrNull() ?: 0.0
                        val threshold = thresholdText.toDoubleOrNull() ?: 5.0

                        val entity = ProductEntity(
                            id = initialProduct?.id ?: 0L,
                            name = name.trim(),
                            barcode = barcode.trim().takeIf { it.isNotBlank() },
                            category = category.trim().ifBlank { "General" },
                            costPriceCents = costPriceCents,
                            sellingPriceCents = sellingPriceCents,
                            currentStock = stock,
                            lowStockThreshold = threshold,
                            unit = unit.trim().ifBlank { "pcs" },
                            createdAt = initialProduct?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(entity, isEdit)
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
