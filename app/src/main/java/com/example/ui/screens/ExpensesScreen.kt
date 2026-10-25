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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.data.local.entity.ExpenseEntity
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.LkrFormatter
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpensesScreen(
    language: String,
    expenses: List<ExpenseEntity>,
    onSaveExpense: (ExpenseEntity, Boolean) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onBack: () -> Unit
) {
    var isCreatingNew by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val categories = listOf("All", "Utilities", "Rent", "Salary", "Transport", "Purchases", "Supplies", "Other")

    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == null || selectedCategoryFilter == "All") {
            expenses
        } else {
            expenses.filter { it.category == selectedCategoryFilter }
        }
    }

    val totalExpensesCents = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amountCents }
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
                        IconButton(onClick = onBack, modifier = Modifier.testTag("expenses_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = Localization.get(Localization.Key.EXPENSES_TITLE, language),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    GradientButton(
                        text = Localization.get(Localization.Key.ADD_EXPENSE, language),
                        icon = Icons.Default.Add,
                        onClick = { isCreatingNew = true },
                        testTag = "add_expense_button",
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                // Summary Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFF27133D)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = Localization.get(Localization.Key.RECORDED_EXPENSES, language),
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LkrFormatter.format(totalExpensesCents, language),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = StatusError
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StatusError.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = (selectedCategoryFilter == null && cat == "All") || selectedCategoryFilter == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = if (cat == "All") null else cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryViolet,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF241438),
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Expenses List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredExpenses.isEmpty()) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = Localization.get(Localization.Key.NO_EXPENSES_FOUND, language),
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredExpenses, key = { it.id }) { expense ->
                            ExpenseRow(
                                expense = expense,
                                language = language,
                                onEdit = { editingExpense = expense },
                                onDelete = { expenseToDelete = expense }
                            )
                        }
                    }
                }
            }

            // Add/Edit Dialog
            if (isCreatingNew || editingExpense != null) {
                ExpenseEditorDialog(
                    initialExpense = editingExpense,
                    language = language,
                    onDismiss = {
                        isCreatingNew = false
                        editingExpense = null
                    },
                    onSave = { entity, isEdit ->
                        onSaveExpense(entity, isEdit)
                        isCreatingNew = false
                        editingExpense = null
                    }
                )
            }

            // Delete Confirmation
            if (expenseToDelete != null) {
                ConfirmationDialog(
                    title = Localization.get(Localization.Key.DELETE_EXPENSE, language),
                    message = "Are you sure you want to delete this expense?\n\"${expenseToDelete?.title}\"",
                    confirmText = Localization.get(Localization.Key.DELETE_EXPENSE, language),
                    cancelText = Localization.get(Localization.Key.CANCEL, language),
                    onConfirm = {
                        expenseToDelete?.let { onDeleteExpense(it) }
                        expenseToDelete = null
                    },
                    onDismiss = { expenseToDelete = null }
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: ExpenseEntity,
    language: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val formattedDate = dateFormat.format(Date(expense.dateTimestamp))

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                    text = expense.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.category} • $formattedDate${if (!expense.note.isNullOrBlank()) " • ${expense.note}" else ""}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = LkrFormatter.format(expense.amountCents, language),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = StatusError
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrightLavender, modifier = Modifier.size(15.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpenseEditorDialog(
    initialExpense: ExpenseEntity?,
    language: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity, Boolean) -> Unit
) {
    val isEdit = initialExpense != null
    var title by remember { mutableStateOf(initialExpense?.title ?: "") }
    var amountText by remember {
        mutableStateOf(if (initialExpense != null) (initialExpense.amountCents / 100.0).toString() else "")
    }
    var category by remember { mutableStateOf(initialExpense?.category ?: "Utilities") }
    var note by remember { mutableStateOf(initialExpense?.note ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Utilities", "Rent", "Salary", "Transport", "Purchases", "Supplies", "Other")

    GlassDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isEdit) Localization.get(Localization.Key.EDIT_EXPENSE, language)
                else Localization.get(Localization.Key.ADD_EXPENSE, language),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = Localization.get(Localization.Key.EXPENSE_TITLE_FIELD, language)
            )
            Spacer(modifier = Modifier.height(10.dp))

            GlassTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = Localization.get(Localization.Key.EXPENSE_AMOUNT, language),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Category picker
            Text(
                text = Localization.get(Localization.Key.EXPENSE_CATEGORY, language),
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    val isSelected = category == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryViolet,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF241438),
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            GlassTextField(
                value = note,
                onValueChange = { note = it },
                label = Localization.get(Localization.Key.EXPENSE_NOTE, language)
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
                        if (title.isBlank()) {
                            errorMessage = "Title is required"
                            return@Button
                        }
                        val amountCents = LkrFormatter.parseToCents(amountText)
                        if (amountCents <= 0) {
                            errorMessage = "Amount must be greater than zero"
                            return@Button
                        }
                        val entity = ExpenseEntity(
                            id = initialExpense?.id ?: 0L,
                            title = title.trim(),
                            amountCents = amountCents,
                            category = category,
                            dateTimestamp = initialExpense?.dateTimestamp ?: System.currentTimeMillis(),
                            note = note.trim().takeIf { it.isNotBlank() }
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
