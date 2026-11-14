package com.example.smarty.model

enum class HungerLevel(val label: String) {
    LIGHT("A little hungry"),
    NORMAL("Hungry"),
    VERY_HUNGRY("Very hungry")
}

enum class TastePreference(val label: String) {
    SWEET("Sweet"),
    SAVORY("Savory"),
    ANY("Either")
}

data class RecipePreferences(
    val hunger: HungerLevel = HungerLevel.NORMAL,
    val taste: TastePreference = TastePreference.ANY,
    val servings: Int = 1
)

// An exact quantity from a specific item in the fridge.
// The unit must match that item's stored unit.
data class FridgeIngredientUsage(
    val itemId: String,
    val name: String,
    val quantity: Double,
    val unit: String
)

data class MissingIngredient(
    val name: String,
    val quantity: Double,
    val unit: String
)

data class RecipeSuggestion(
    val id: String,
    val title: String,
    val servings: Int,
    val preparationMinutes: Int,
    val explanation: String,
    val fridgeIngredients: List<FridgeIngredientUsage>,
    val missingIngredients: List<MissingIngredient>,
    val steps: List<String>
)