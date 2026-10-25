package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.SaleWithItems
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.repository.CartItem
import com.example.ui.components.BizMateBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.GradientButton
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.BrightLavender
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderActive
import com.example.ui.theme.GlassCardBg
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    language: String,
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    cart: List<CartItem>,
    discountCents: Long,
    paymentMethod: String,
    amountPaidCents: Long,
    selectedCustomer: CustomerEntity?,
    onAddToCart: (ProductEntity) -> Unit,
    onUpdateQty: (Long, Double) -> Unit,
    onRemoveFromCart: (Long) -> Unit,
    onClearCart: () -> Unit,
    onSetDiscount: (Long) -> Unit,
    onSetPaymentMethod: (String) -> Unit,
    onSetAmountPaid: (Long) -> Unit,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onCheckout: ((SaleWithItems) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }
    var isCheckingOut by remember { mutableStateOf(false) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesCategory = selectedCategory == null || selectedCategory == "All" || p.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    (p.barcode != null && p.barcode.contains(searchQuery, ignoreCase = true))
            matchesCategory && matchesSearch
        }
    }

    val cartItemCount = remember(cart) { cart.sumOf { it.quantity } }
    val subtotalCents = remember(cart) { cart.sumOf { it.totalCents } }
    val validDiscountCents = remember(discountCents, subtotalCents) {
        discountCents.coerceIn(0L, subtotalCents)
    }
    val finalTotalCents = remember(subtotalCents, validDiscountCents) {
        (subtotalCents - validDiscountCents).coerceAtLeast(0L)
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
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pos_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = Localization.get(Localization.Key.POS_TITLE, language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Search Bar
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
                    testTag = "pos_search_input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = (selectedCategory == null && cat == "All") || selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (cat == "All") null else cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryViolet,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF241438),
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BrightLavender else GlassBorder
                            )
                        )
                    }
                }

                // Product Catalog List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = if (cart.isNotEmpty()) 100.dp else 24.dp),
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
                            PosProductRow(
                                product = product,
                                language = language,
                                onAddToCart = { onAddToCart(product) }
                            )
                        }
                    }
                }
            }

            // Bottom Floating Cart Bar
            if (cart.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("floating_cart_bar"),
                        shape = RoundedCornerShape(20.dp),
                        backgroundColor = Color(0xFF2B1648),
                        borderColor = BrightLavender,
                        borderWidth = 1.5.dp,
                        onClick = { showCartSheet = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryViolet),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${LkrFormatter.formatQuantity(cartItemCount)} ${if (language == "si") "භාණ්ඩ" else "items"}",
                                        fontSize = 12.sp,
                                        color = LightLavender
                                    )
                                    Text(
                                        text = LkrFormatter.format(finalTotalCents, language),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            GradientButton(
                                text = if (language == "si") "බිල්පත බලන්න" else "View Cart",
                                onClick = { showCartSheet = true },
                                testTag = "view_cart_button",
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Modal Cart Sheet
            if (showCartSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCartSheet = false },
                    containerColor = Color(0xFF1E0E33),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .size(width = 44.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(BrightLavender.copy(alpha = 0.5f))
                        )
                    }
                ) {
                    CartSheetContent(
                        language = language,
                        cart = cart,
                        subtotalCents = subtotalCents,
                        discountCents = validDiscountCents,
                        finalTotalCents = finalTotalCents,
                        paymentMethod = paymentMethod,
                        amountPaidCents = amountPaidCents,
                        customers = customers,
                        selectedCustomer = selectedCustomer,
                        isCheckingOut = isCheckingOut,
                        onUpdateQty = onUpdateQty,
                        onRemoveItem = onRemoveFromCart,
                        onClearCart = {
                            onClearCart()
                            showCartSheet = false
                        },
                        onSetDiscount = onSetDiscount,
                        onSetPaymentMethod = onSetPaymentMethod,
                        onSetAmountPaid = onSetAmountPaid,
                        onSelectCustomer = onSelectCustomer,
                        onCheckout = {
                            isCheckingOut = true
                            onCheckout {
                                isCheckingOut = false
                                showCartSheet = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PosProductRow(
    product: ProductEntity,
    language: String,
    onAddToCart: () -> Unit
) {
    val isOutOfStock = product.currentStock <= 0.0
    val isLowStock = product.currentStock <= product.lowStockThreshold && !isOutOfStock

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        onClick = if (!isOutOfStock) onAddToCart else null,
        testTag = "product_item_${product.id}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.category,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Stock badge
                    val (badgeText, badgeColor) = when {
                        isOutOfStock -> Localization.get(Localization.Key.OUT_OF_STOCK, language) to StatusError
                        isLowStock -> "${Localization.get(Localization.Key.LOW_STOCK, language)} (${LkrFormatter.formatQuantity(product.currentStock, product.unit)})" to StatusWarning
                        else -> "${LkrFormatter.formatQuantity(product.currentStock, product.unit)}" to StatusSuccess
                    }
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LkrFormatter.format(product.sellingPriceCents, language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentLavender
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isOutOfStock) Color(0xFF38294D) else PrimaryViolet)
                        .clickable(enabled = !isOutOfStock, onClick = onAddToCart),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = if (isOutOfStock) TextMuted else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CartSheetContent(
    language: String,
    cart: List<CartItem>,
    subtotalCents: Long,
    discountCents: Long,
    finalTotalCents: Long,
    paymentMethod: String,
    amountPaidCents: Long,
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    isCheckingOut: Boolean,
    onUpdateQty: (Long, Double) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearCart: () -> Unit,
    onSetDiscount: (Long) -> Unit,
    onSetPaymentMethod: (String) -> Unit,
    onSetAmountPaid: (Long) -> Unit,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onCheckout: () -> Unit
) {
    var discountText by remember { mutableStateOf(if (discountCents > 0) (discountCents / 100.0).toString() else "") }
    var amountPaidText by remember { mutableStateOf(if (amountPaidCents > 0) (amountPaidCents / 100.0).toString() else "") }

    val changeCents = remember(amountPaidCents, finalTotalCents, paymentMethod) {
        if (paymentMethod == "CASH") {
            (amountPaidCents - finalTotalCents).coerceAtLeast(0L)
        } else 0L
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Title Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Localization.get(Localization.Key.CART, language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (language == "si") "සියල්ල ඉවත් කරන්න" else "Clear All",
                    fontSize = 12.sp,
                    color = StatusError,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onClearCart)
                        .padding(6.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Cart items
        items(cart, key = { it.product.id }) { item ->
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
                            text = item.product.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${LkrFormatter.format(item.unitPriceCents, language)} × ${LkrFormatter.formatQuantity(item.quantity, item.product.unit)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // - button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF382352))
                                .clickable { onUpdateQty(item.product.id, item.quantity - 1.0) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = LkrFormatter.formatQuantity(item.quantity),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        // + button
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryViolet)
                                .clickable { onUpdateQty(item.product.id, item.quantity + 1.0) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = LkrFormatter.format(item.totalCents, language),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentLavender
                        )
                        IconButton(
                            onClick = { onRemoveItem(item.product.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = StatusError, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Customer selection
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = Localization.get(Localization.Key.SELECT_CUSTOMER, language),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val isWalkIn = selectedCustomer == null
                    FilterChip(
                        selected = isWalkIn,
                        onClick = { onSelectCustomer(null) },
                        label = { Text(Localization.get(Localization.Key.WALK_IN_CUSTOMER, language), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF241438),
                            labelColor = TextSecondary
                        )
                    )
                }
                items(customers) { c ->
                    val isSelected = selectedCustomer?.id == c.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCustomer(c) },
                        label = { Text(c.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF241438),
                            labelColor = TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Payment Method Selector
        item {
            Text(
                text = Localization.get(Localization.Key.PAYMENT_METHOD, language),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaymentMethodChip(
                    label = Localization.get(Localization.Key.CASH, language),
                    isSelected = paymentMethod == "CASH",
                    modifier = Modifier.weight(1f),
                    onClick = { onSetPaymentMethod("CASH") }
                )
                PaymentMethodChip(
                    label = Localization.get(Localization.Key.CARD, language),
                    isSelected = paymentMethod == "CARD",
                    modifier = Modifier.weight(1f),
                    onClick = { onSetPaymentMethod("CARD") }
                )
                PaymentMethodChip(
                    label = Localization.get(Localization.Key.CREDIT_UDAV, language),
                    isSelected = paymentMethod == "CREDIT",
                    modifier = Modifier.weight(1f),
                    onClick = { onSetPaymentMethod("CREDIT") }
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Discount & Cash Payment Inputs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Discount field
                OutlinedTextField(
                    value = discountText,
                    onValueChange = {
                        discountText = it
                        onSetDiscount(LkrFormatter.parseToCents(it))
                    },
                    label = { Text(Localization.get(Localization.Key.DISCOUNT, language), fontSize = 12.sp) },
                    placeholder = { Text("0.00", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF241438),
                        unfocusedContainerColor = Color(0xFF1E1030),
                        focusedBorderColor = BrightLavender,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Cash Amount Received field (only if Cash)
                if (paymentMethod == "CASH") {
                    OutlinedTextField(
                        value = amountPaidText,
                        onValueChange = {
                            amountPaidText = it
                            onSetAmountPaid(LkrFormatter.parseToCents(it))
                        },
                        label = { Text(Localization.get(Localization.Key.AMOUNT_RECEIVED, language), fontSize = 12.sp) },
                        placeholder = { Text(LkrFormatter.format(finalTotalCents, language), fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF241438),
                            unfocusedContainerColor = Color(0xFF1E1030),
                            focusedBorderColor = BrightLavender,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Financial Summary Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF241438),
                borderColor = BrightLavender.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Localization.get(Localization.Key.SUBTOTAL, language), color = TextSecondary, fontSize = 13.sp)
                        Text(LkrFormatter.format(subtotalCents, language), color = TextPrimary, fontSize = 13.sp)
                    }
                    if (discountCents > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Localization.get(Localization.Key.DISCOUNT, language), color = StatusError, fontSize = 13.sp)
                            Text("-${LkrFormatter.format(discountCents, language)}", color = StatusError, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Localization.get(Localization.Key.TOTAL, language), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        Text(LkrFormatter.format(finalTotalCents, language), color = AccentLavender, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    if (paymentMethod == "CASH" && amountPaidCents > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Localization.get(Localization.Key.CHANGE, language), color = StatusSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(LkrFormatter.format(changeCents, language), color = StatusSuccess, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Complete Checkout Button
        item {
            GradientButton(
                text = Localization.get(Localization.Key.CHECKOUT, language),
                onClick = onCheckout,
                isLoading = isCheckingOut,
                modifier = Modifier.fillMaxWidth(),
                testTag = "complete_checkout_button"
            )
        }
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryViolet else Color(0xFF241438))
            .border(1.dp, if (isSelected) BrightLavender else GlassBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}
