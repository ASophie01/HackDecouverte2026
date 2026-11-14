package com.example.smarty.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smarty.R
import java.text.Normalizer

enum class FoodGroup(val displayName: String, @DrawableRes val icon: Int, val stems: List<String>) {
    LEAFY("Veggies", R.drawable.ic_food_leafy,
        listOf("spinach", "epinard", "lettuce", "laitue", "kale", "salad", "salade", "arugula", "roquette", "broccoli")),
    DAIRY("Dairy", R.drawable.ic_food_dairy,
        listOf("milk", "lait", "yogurt", "yaourt", "cheese", "fromage", "butter", "beurre", "cream", "creme")),
    EGGS("Dairy", R.drawable.ic_food_eggs, listOf("egg", "oeuf")),
    FRUIT("Fruit", R.drawable.ic_food_fruit,
        listOf("strawberr", "fraise", "apple", "pomme", "banana", "banane", "orange", "grape", "raisin", "berr", "lemon", "citron", "pear", "poire", "peach", "peche")),
    BREAD("Pantry", R.drawable.ic_food_bread,
        listOf("bread", "pain", "rice", "riz", "pasta", "pate", "flour", "farine", "cereal", "oat", "avoine")),
    MEAT("Meat", R.drawable.ic_food_meat,
        listOf("chicken", "poulet", "beef", "boeuf", "pork", "porc", "steak", "bacon", "ham", "jambon", "sausage", "saucisse", "viande", "meat")),
    FISH("Fish", R.drawable.ic_food_fish,
        listOf("fish", "poisson", "salmon", "saumon", "tuna", "thon", "shrimp", "crevette", "cod", "morue")),
    OTHER("Other", R.drawable.ic_food_pantry, emptyList())
}

private fun normalize(text: String): List<String> =
    Normalizer.normalize(text.lowercase().replace("œ", "oe"), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")          // strip accents: épinards -> epinards
        .split(Regex("[^a-z]+"))
        .filter { it.isNotBlank() }

// Short stems must match a whole word ("lait" must not match "laitue"); long stems match prefixes (plurals).
private fun tokenMatches(token: String, stem: String) =
    if (stem.length <= 4) token == stem || token == stem + "s" else token.startsWith(stem)

/** Display decision only: never written to Firestore. Name wins over category. */
fun foodGroupFor(name: String, category: String): FoodGroup {
    for (source in listOf(name, category)) {
        val tokens = normalize(source)
        FoodGroup.entries.firstOrNull { g ->
            g.stems.any { stem -> tokens.any { tokenMatches(it, stem) } }
        }?.let { return it }
    }
    return FoodGroup.OTHER
}

@Composable
fun FoodIllustration(name: String, category: String, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(foodGroupFor(name, category).icon),
            contentDescription = null,          // decorative; the name is shown next to it
            modifier = Modifier.padding(6.dp)
        )
    }
}