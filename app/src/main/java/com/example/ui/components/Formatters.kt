package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.CategoryOption
import com.example.data.CustomCategory
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val esCubaLocale = Locale.Builder().setLanguage("es").setRegion("CU").build()
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    private val cupDecimalFormat = DecimalFormat("#,##0.00", symbols)
    private val cupWholeFormat = DecimalFormat("#,##0", symbols)
    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", esCubaLocale)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", esCubaLocale)
    private val dayMonthFormat = SimpleDateFormat("EEE dd MMM", esCubaLocale)
    private val yearMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    fun formatCup(amount: Double, includeCode: Boolean = true): String {
        val isWhole = kotlin.math.abs(amount - amount.toLong()) < 0.005
        val numStr = if (isWhole) cupWholeFormat.format(amount) else cupDecimalFormat.format(amount)
        return if (includeCode) "$$numStr CUP" else "$$numStr"
    }

    fun formatShortDate(millis: Long): String = shortDateFormat.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun formatDayHeader(millis: Long): String =
        dayMonthFormat.format(Date(millis)).replaceFirstChar { it.uppercase(esCubaLocale) }

    fun currentYearMonth(): String = yearMonthFormat.format(Date())

    fun yearMonthFromMillis(millis: Long): String = yearMonthFormat.format(Date(millis))

    fun shiftYearMonth(yearMonth: String, deltaMonths: Int): String {
        val parts = yearMonth.split("-")
        val cal = Calendar.getInstance()
        if (parts.size == 2) {
            cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11))
            cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.MONTH, deltaMonths)
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun formatYearMonthLabel(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val monthIdx = (parts.getOrNull(1)?.toIntOrNull() ?: 1) - 1
        val year = parts.getOrNull(0) ?: ""
        val monthNames = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        return "${monthNames.getOrElse(monthIdx) { "Mes" }} $year"
    }

    fun daysInYearMonth(yearMonth: String): Int {
        val parts = yearMonth.split("-")
        val cal = Calendar.getInstance()
        if (parts.size == 2) {
            cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11))
            cal.set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun dayOfMonth(millis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return cal.get(Calendar.DAY_OF_MONTH)
    }
}

object CategoryDefaults {
    val builtInCategories: List<CategoryOption> = listOf(
        CategoryOption("food", "Comida", "restaurant", 0xFF047857),
        CategoryOption("transport", "Transporte", "bus", 0xFF0284C7),
        CategoryOption("home", "Hogar", "home", 0xFF0F766E),
        CategoryOption("services", "Servicios", "receipt", 0xFFD97706),
        CategoryOption("health", "Salud", "favorite", 0xFFDC2626),
        CategoryOption("shopping", "Compras", "shopping", 0xFF7C3AED),
        CategoryOption("leisure", "Ocio", "leisure", 0xFFDB2777),
        CategoryOption("education", "Educación", "school", 0xFF334155)
    )

    val selectableIcons: List<Pair<String, ImageVector>> = listOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "bus" to Icons.Rounded.DirectionsBus,
        "home" to Icons.Rounded.Home,
        "receipt" to Icons.AutoMirrored.Rounded.ReceiptLong,
        "favorite" to Icons.Rounded.Favorite,
        "shopping" to Icons.Rounded.ShoppingBag,
        "leisure" to Icons.Rounded.SportsEsports,
        "school" to Icons.Rounded.School,
        "cafe" to Icons.Rounded.LocalCafe,
        "pets" to Icons.Rounded.Pets,
        "work" to Icons.Rounded.Work,
        "category" to Icons.Rounded.Category
    )

    val selectableColorHexes: List<String> = listOf(
        "#047857",
        "#0284C7",
        "#0F766E",
        "#D97706",
        "#DC2626",
        "#7C3AED",
        "#DB2777",
        "#334155"
    )

    fun iconForKey(key: String): ImageVector =
        selectableIcons.firstOrNull { it.first == key }?.second ?: Icons.Rounded.Category

    fun mergeCategories(custom: List<CustomCategory>): List<CategoryOption> {
        val customOptions = custom.map { cat ->
            val parsedColor = runCatching {
                android.graphics.Color.parseColor(cat.colorHex).toLong() and 0xFFFFFFFFL
            }.getOrDefault(0xFF047857L)
            CategoryOption(
                id = cat.id,
                name = cat.name,
                iconKey = cat.iconKey,
                colorHex = parsedColor,
                isCustom = true
            )
        }
        return builtInCategories + customOptions
    }

    fun findCategoryColor(categoryId: String, categories: List<CategoryOption>): Color {
        val hex = categories.firstOrNull { it.id == categoryId }?.colorHex ?: 0xFF047857L
        return Color(hex)
    }

    fun findCategoryIcon(categoryId: String, categories: List<CategoryOption>): ImageVector {
        val key = categories.firstOrNull { it.id == categoryId }?.iconKey ?: "category"
        return iconForKey(key)
    }
}
