package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.dao.SaleWithItems
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LkrFormatter
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesHistoryScreen(
    language: String,
    salesWithItems: List<SaleWithItems>,
    onSelectSale: (SaleWithItems) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSales = remember(salesWithItems, searchQuery) {
        if (searchQuery.isBlank()) salesWithItems
        else {
            salesWithItems.filter {
                it.sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                        (it.sale.customerName != null && it.sale.customerName.contains(searchQuery, ignoreCase = true))
            }
        }
    }

    BizMateBackground {
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
                IconButton(onClick = onBack, modifier = Modifier.testTag("sales_history_back_button")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = Localization.get(Localization.Key.NAV_HISTORY, language),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            // Search
            GlassTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search by invoice or customer...",
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
                if (filteredSales.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Localization.get(Localization.Key.NO_RECENT_TRANSACTIONS, language),
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredSales, key = { it.sale.id }) { saleWithItems ->
                        val sale = saleWithItems.sale
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
                        val formattedDate = dateFormat.format(Date(sale.timestamp))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            onClick = { onSelectSale(saleWithItems) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = sale.invoiceNumber,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        if (sale.isVoided) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = StatusError.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = Localization.get(Localization.Key.VOIDED_LABEL, language),
                                                    color = StatusError,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$formattedDate • ${sale.paymentMethod}${if (!sale.customerName.isNullOrBlank()) " • ${sale.customerName}" else ""}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${saleWithItems.items.size} items",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Text(
                                    text = LkrFormatter.format(sale.totalCents, language),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (sale.isVoided) StatusError else AccentLavender
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    saleWithItems: SaleWithItems,
    shopName: String,
    language: String,
    onDismiss: () -> Unit,
    onVoidSale: (Long, String) -> Unit,
    onShare: (String) -> Unit
) {
    val sale = saleWithItems.sale
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(sale.timestamp))
    var showVoidConfirm by remember { mutableStateOf(false) }

    val formattedReceiptText = remember(saleWithItems, shopName, language) {
        buildString {
            appendLine("================================")
            appendLine("           $shopName            ")
            appendLine("          BizMate LK            ")
            appendLine("================================")
            appendLine("Invoice: ${sale.invoiceNumber}")
            appendLine("Date: $formattedDate")
            if (!sale.customerName.isNullOrBlank()) {
                appendLine("Customer: ${sale.customerName}")
            }
            appendLine("Payment: ${sale.paymentMethod}")
            if (sale.isVoided) {
                appendLine("STATUS: *** VOIDED ***")
                if (!sale.voidReason.isNullOrBlank()) {
                    appendLine("Void Reason: ${sale.voidReason}")
                }
            }
            appendLine("--------------------------------")
            for (item in saleWithItems.items) {
                appendLine("${item.productName} x ${LkrFormatter.formatQuantity(item.quantity)}")
                appendLine("   @ ${LkrFormatter.format(item.unitPriceCents, language)} = ${LkrFormatter.format(item.totalCents, language)}")
            }
            appendLine("--------------------------------")
            appendLine("Subtotal: ${LkrFormatter.format(sale.subtotalCents, language)}")
            if (sale.discountCents > 0) {
                appendLine("Discount: -${LkrFormatter.format(sale.discountCents, language)}")
            }
            appendLine("TOTAL: ${LkrFormatter.format(sale.totalCents, language)}")
            if (sale.paymentMethod == "CASH") {
                appendLine("Paid: ${LkrFormatter.format(sale.amountPaidCents, language)}")
                appendLine("Change: ${LkrFormatter.format(sale.changeCents, language)}")
            }
            appendLine("================================")
            appendLine("  Thank You For Your Business!  ")
            appendLine("================================")
        }
    }

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Receipt Header
            Text(
                text = shopName,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = Localization.get(Localization.Key.RECEIPT_TITLE, language),
                fontSize = 12.sp,
                color = AccentLavender
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (sale.isVoided) {
                Surface(
                    color = StatusError.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StatusError)
                ) {
                    Text(
                        text = "*** ${Localization.get(Localization.Key.VOIDED_LABEL, language)} ***",
                        color = StatusError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
            Spacer(modifier = Modifier.height(10.dp))

            // Metadata
            ReceiptMetaRow(Localization.get(Localization.Key.INVOICE_NO, language), sale.invoiceNumber)
            ReceiptMetaRow(Localization.get(Localization.Key.DATE, language), formattedDate)
            if (!sale.customerName.isNullOrBlank()) {
                ReceiptMetaRow(Localization.get(Localization.Key.CUSTOMER_NAME, language), sale.customerName)
            }
            ReceiptMetaRow(Localization.get(Localization.Key.PAYMENT_METHOD, language), sale.paymentMethod)

            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
            Spacer(modifier = Modifier.height(10.dp))

            // Item list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for (item in saleWithItems.items) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "${LkrFormatter.formatQuantity(item.quantity)} × ${LkrFormatter.format(item.unitPriceCents, language)}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            LkrFormatter.format(item.totalCents, language),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLavender
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
            Spacer(modifier = Modifier.height(10.dp))

            // Totals
            ReceiptMetaRow(Localization.get(Localization.Key.SUBTOTAL, language), LkrFormatter.format(sale.subtotalCents, language))
            if (sale.discountCents > 0) {
                ReceiptMetaRow(Localization.get(Localization.Key.DISCOUNT, language), "-${LkrFormatter.format(sale.discountCents, language)}")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(Localization.get(Localization.Key.TOTAL, language), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(LkrFormatter.format(sale.totalCents, language), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AccentLavender)
            }

            if (sale.paymentMethod == "CASH") {
                Spacer(modifier = Modifier.height(6.dp))
                ReceiptMetaRow(Localization.get(Localization.Key.AMOUNT_RECEIVED, language), LkrFormatter.format(sale.amountPaidCents, language))
                ReceiptMetaRow(Localization.get(Localization.Key.CHANGE, language), LkrFormatter.format(sale.changeCents, language))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onShare(formattedReceiptText) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LightLavender),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(Localization.get(Localization.Key.PRINT_SHARE_RECEIPT, language), fontSize = 12.sp)
                }

                if (!sale.isVoided) {
                    Button(
                        onClick = { showVoidConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Localization.get(Localization.Key.VOID_SALE, language), fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Transparent)
            ) {
                Text("Close")
            }
        }
    }

    if (showVoidConfirm) {
        ConfirmationDialog(
            title = Localization.get(Localization.Key.VOID_CONFIRM_TITLE, language),
            message = Localization.get(Localization.Key.VOID_CONFIRM_MSG, language),
            confirmText = Localization.get(Localization.Key.VOID_SALE, language),
            cancelText = Localization.get(Localization.Key.CANCEL, language),
            onConfirm = {
                onVoidSale(sale.id, "User requested void")
                showVoidConfirm = false
            },
            onDismiss = { showVoidConfirm = false }
        )
    }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, color = TextPrimary)
    }
    Spacer(modifier = Modifier.height(4.dp))
}
