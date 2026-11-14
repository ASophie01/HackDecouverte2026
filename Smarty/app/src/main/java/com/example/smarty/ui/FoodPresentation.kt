package com.example.smarty.ui

import com.example.smarty.model.FridgeItem
import java.text.Normalizer
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.math.BigDecimal
import java.util.Locale

enum class FoodKind(val group: String) {
    DAIRY("Dairy"), EGGS("Protein"), VEGETABLE("Veggies"), FRUIT("Fruit"),
    BREAD("Pantry"), MEAT("Protein"), FISH("Protein"), PANTRY("Pantry")
}

fun foodKind(name: String, category: String = ""): FoodKind {
    val normalized = Normalizer.normalize("$name $category".lowercase(Locale.ROOT)
        .replace("œ", "oe"), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    val words = normalized.split(Regex("[^a-z0-9]+" )).toSet()
    fun has(vararg choices: String) = choices.any { it in words }
    return when {
        has("egg", "eggs", "oeuf", "oeufs") -> FoodKind.EGGS
        has("milk", "lait", "yogurt", "yoghurt", "yaourt", "yogourt", "cheese", "fromage", "dairy", "cream", "creme", "butter", "beurre") -> FoodKind.DAIRY
        has("spinach", "epinard", "epinards", "lettuce", "laitue", "broccoli", "brocoli", "carrot", "carrots", "carotte", "carottes", "vegetable", "vegetables", "veggies", "legume", "legumes", "eggplant", "aubergine") -> FoodKind.VEGETABLE
        has("fruit", "fruits", "strawberry", "strawberries", "fraise", "fraises", "apple", "apples", "pomme", "pommes", "banana", "banane", "orange", "berries", "blueberry", "bleuet", "bleuets", "tomato", "tomate") -> FoodKind.FRUIT
        has("bread", "pain", "toast", "bagel", "croissant", "grains", "cereal", "cereales") -> FoodKind.BREAD
        has("fish", "poisson", "salmon", "saumon", "tuna", "thon", "shrimp", "crevette", "crevettes", "seafood") -> FoodKind.FISH
        has("meat", "viande", "chicken", "poulet", "beef", "boeuf", "pork", "porc", "turkey", "dinde", "protein", "proteine", "tofu") -> FoodKind.MEAT
        else -> FoodKind.PANTRY
    }
}

fun itemDate(item: FridgeItem): LocalDate? = runCatching { LocalDate.parse(item.expiryDate) }.getOrNull()
fun isUseSoon(item: FridgeItem, today: LocalDate = LocalDate.now()): Boolean {
    val date = itemDate(item) ?: return false
    return !date.isBefore(today) && !date.isAfter(today.plusDays(3))
}
fun expiryLabel(item: FridgeItem, today: LocalDate = LocalDate.now()): String {
    val date = itemDate(item) ?: return "Check date"
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> "Past entered date"
        days == 0L -> "Today"
        days == 1L -> "Tomorrow"
        days <= 7 -> "In $days days"
        else -> item.expiryDate
    }
}
fun amountLabel(quantity: Double): String = if (quantity.isFinite()) {
    BigDecimal.valueOf(quantity).stripTrailingZeros().toPlainString()
} else "—"
