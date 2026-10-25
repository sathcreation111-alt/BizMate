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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CustomerCreditTransactionEntity
import com.example.data.local.entity.CustomerEntity
import com.example.ui.components.BizMateBackground
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
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomerCreditScreen(
    language: String,
    customers: List<CustomerEntity>,
    onGetBalance: suspend (Long) -> Long,
    onGetTransactions: (Long) -> Flow<List<CustomerCreditTransactionEntity>>,
    onSaveCustomer: (CustomerEntity, Boolean) -> Unit,
    onRecordRepayment: (Long, Long, String?) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isAddingCustomer by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForRepayment by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { c ->
            searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    (c.phone != null && c.phone.contains(searchQuery))
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("credit_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.get(Localization.Key.CUSTOMER_CREDIT_TITLE, language),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    GradientButton(
                        text = Localization.get(Localization.Key.ADD_NEW_CUSTOMER, language),
                        icon = Icons.Default.Add,
                        onClick = { isAddingCustomer = true },
                        testTag = "add_customer_button",
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Search
                GlassTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = Localization.get(Localization.Key.CUSTOMER_NAME, language),
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Customers List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredCustomers.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = Localization.get(Localization.Key.NO_CREDIT_CUSTOMERS, language),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            CustomerLedgerCard(
                                customer = customer,
                                language = language,
                                onGetBalance = onGetBalance,
                                onClick = { selectedCustomerForDetails = customer },
                                onRecordPayment = { customerForRepayment = customer }
                            )
                        }
                    }
                }
            }

            // Add Customer Dialog
            if (isAddingCustomer) {
                AddCustomerDialog(
                    language = language,
                    onDismiss = { isAddingCustomer = false },
                    onSave = { entity ->
                        onSaveCustomer(entity, false)
                        isAddingCustomer = false
                    }
                )
            }

            // Record Repayment Dialog
            if (customerForRepayment != null) {
                RecordRepaymentDialog(
                    customer = customerForRepayment!!,
                    language = language,
                    onGetBalance = onGetBalance,
                    onDismiss = { customerForRepayment = null },
                    onConfirm = { amountCents, note ->
                        onRecordRepayment(customerForRepayment!!.id, amountCents, note)
                        customerForRepayment = null
                    }
                )
            }

            // Customer Details Dialog (Ledger History)
            if (selectedCustomerForDetails != null) {
                CustomerHistoryDialog(
                    customer = selectedCustomerForDetails!!,
                    language = language,
                    onGetBalance = onGetBalance,
                    onGetTransactions = onGetTransactions,
                    onDismiss = { selectedCustomerForDetails = null },
                    onRepayment = {
                        customerForRepayment = selectedCustomerForDetails
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomerLedgerCard(
    customer: CustomerEntity,
    language: String,
    onGetBalance: suspend (Long) -> Long,
    onClick: () -> Unit,
    onRecordPayment: () -> Unit
) {
    var balanceCents by remember { mutableStateOf(0L) }

    LaunchedEffect(customer.id) {
        balanceCents = onGetBalance(customer.id)
    }

    val isSettled = balanceCents <= 0L

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick
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
                    text = customer.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (!customer.phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = customer.phone,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${Localization.get(Localization.Key.CURRENT_BALANCE, language)}: ",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = LkrFormatter.format(balanceCents, language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSettled) StatusSuccess else AccentLavender
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isSettled) {
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = Localization.get(Localization.Key.RECORD_PAYMENT, language),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddCustomerDialog(
    language: String,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Localization.get(Localization.Key.ADD_NEW_CUSTOMER, language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            GlassTextField(
                value = name,
                onValueChange = { name = it },
                label = Localization.get(Localization.Key.CUSTOMER_NAME, language)
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassTextField(
                value = phone,
                onValueChange = { phone = it },
                label = Localization.get(Localization.Key.CUSTOMER_PHONE, language),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassTextField(
                value = notes,
                onValueChange = { notes = it },
                label = Localization.get(Localization.Key.CUSTOMER_NOTES, language)
            )

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
                        onSave(
                            CustomerEntity(
                                name = name.trim(),
                                phone = phone.trim().takeIf { it.isNotBlank() },
                                notes = notes.trim().takeIf { it.isNotBlank() }
                            )
                        )
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

@Composable
private fun RecordRepaymentDialog(
    customer: CustomerEntity,
    language: String,
    onGetBalance: suspend (Long) -> Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, String?) -> Unit
) {
    var balanceCents by remember { mutableStateOf(0L) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(customer.id) {
        balanceCents = onGetBalance(customer.id)
        if (balanceCents > 0) {
            amountText = (balanceCents / 100.0).toString()
        }
    }

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${Localization.get(Localization.Key.RECORD_PAYMENT, language)}: ${customer.name}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${Localization.get(Localization.Key.CURRENT_BALANCE, language)}: ${LkrFormatter.format(balanceCents, language)}",
                fontSize = 13.sp,
                color = AccentLavender
            )
            Spacer(modifier = Modifier.height(16.dp))

            GlassTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = Localization.get(Localization.Key.PAYMENT_AMOUNT, language),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassTextField(
                value = note,
                onValueChange = { note = it },
                label = "Note (e.g. Cash payment)"
            )

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
                        val amount = LkrFormatter.parseToCents(amountText)
                        if (amount <= 0) {
                            errorMessage = "Amount must be greater than zero"
                            return@Button
                        }
                        onConfirm(amount, note.trim().takeIf { it.isNotBlank() })
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

@Composable
private fun CustomerHistoryDialog(
    customer: CustomerEntity,
    language: String,
    onGetBalance: suspend (Long) -> Long,
    onGetTransactions: (Long) -> Flow<List<CustomerCreditTransactionEntity>>,
    onDismiss: () -> Unit,
    onRepayment: () -> Unit
) {
    var balanceCents by remember { mutableStateOf(0L) }
    var transactions by remember { mutableStateOf<List<CustomerCreditTransactionEntity>>(emptyList()) }

    LaunchedEffect(customer.id) {
        balanceCents = onGetBalance(customer.id)
        onGetTransactions(customer.id).collect {
            transactions = it
        }
    }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = customer.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (!customer.phone.isNullOrBlank()) {
                        Text(customer.phone, fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Text(
                    text = LkrFormatter.format(balanceCents, language),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (balanceCents <= 0) StatusSuccess else AccentLavender
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = Localization.get(Localization.Key.TRANSACTION_HISTORY, language),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "No credit transactions recorded yet",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(transactions) { tx ->
                        val isPayment = tx.type == "PAYMENT_RECEIVED"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E0F33), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isPayment) "Repayment Received" else (tx.note ?: "Credit Sale"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayment) StatusSuccess else LightLavender
                                )
                                Text(
                                    text = dateFormat.format(Date(tx.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isPayment) "-" else "+"}${LkrFormatter.format(tx.amountCents, language)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPayment) StatusSuccess else StatusError
                                )
                                Text(
                                    text = "Bal: ${LkrFormatter.format(tx.balanceAfterCents, language)}",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    Text("Close")
                }
                if (balanceCents > 0) {
                    Button(
                        onClick = {
                            onDismiss()
                            onRepayment()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                    ) {
                        Text("Repay", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
