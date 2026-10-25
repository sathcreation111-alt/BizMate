package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object LkrFormatter {
    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun format(cents: Long, language: String): String {
        val isNegative = cents < 0
        val absCents = Math.abs(cents)
        val rupees = absCents / 100.0
        val formattedNumber = decimalFormat.format(rupees)

        val prefix = if (language == "si") "රු. " else "Rs. "
        val sign = if (isNegative) "-" else ""

        return "$sign$prefix$formattedNumber"
    }

    fun parseToCents(input: String): Long {
        val sanitized = input.trim().replace(",", "").replace("Rs.", "").replace("රු.", "").trim()
        if (sanitized.isBlank()) return 0L
        return try {
            val value = sanitized.toDouble()
            (value * 100).toLong()
        } catch (e: Exception) {
            0L
        }
    }

    fun formatQuantity(qty: Double, unit: String = ""): String {
        val formatted = if (qty == qty.toLong().toDouble()) {
            qty.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", qty)
        }
        return if (unit.isNotBlank()) "$formatted $unit" else formatted
    }
}
