package com.example.smarty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarty.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RecipeControls(
    userId: String,
    fridgeId: String,
    inventory: List<FridgeItem>
) {
    val service = remember { RecipeService() }
    val scope = rememberCoroutineScope()

    var showPreferences by remember { mutableStateOf(false) }
    var hunger by remember { mutableStateOf(HungerLevel.NORMAL) }
    var taste by remember { mutableStateOf(TastePreference.ANY) }
    var servings by remember { mutableStateOf(1) }

    var loading by remember { mutableStateOf(false) }
    var recipe by remember { mutableStateOf<RecipeSuggestion?>(null) }
    var cooking by remember { mutableStateOf(false) }
    var cookedIds by remember { mutableStateOf(setOf<String>()) }
    var showConfirmation by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column {
        Button(
            enabled = inventory.isNotEmpty() && !loading && !cooking,
            onClick = {
                error = null
                message = null
                showPreferences = true
            }
        ) {
            Text(if (loading) "Thinking..." else "What can I cook?")
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        message?.let {
            Text(it)
        }
    }

    if (showPreferences) {
        AlertDialog(
            onDismissRequest = { showPreferences = false },
            title = { Text("What are you craving?") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    Text("How hungry are you?")

                    HungerLevel.entries.forEach { option ->
                        Row {
                            RadioButton(
                                selected = hunger == option,
                                onClick = { hunger = option }
                            )
                            Text(
                                text = option.label,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }

                    Text("Sweet or savory?")

                    TastePreference.entries.forEach { option ->
                        Row {
                            RadioButton(
                                selected = taste == option,
                                onClick = { taste = option }
                            )
                            Text(
                                text = option.label,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }

                    Row {
                        TextButton(
                            enabled = servings > 1,
                            onClick = { servings-- }
                        ) {
                            Text("−")
                        }

                        Text(
                            text = "$servings serving(s)",
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        TextButton(
                            enabled = servings < 8,
                            onClick = { servings++ }
                        ) {
                            Text("+")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val preferences = RecipePreferences(
                            hunger = hunger,
                            taste = taste,
                            servings = servings
                        )

                        val inventorySnapshot = inventory.toList()

                        showPreferences = false
                        loading = true
                        error = null
                        message = null

                        scope.launch {
                            try {
                                recipe = service.generate(
                                    inventorySnapshot,
                                    preferences
                                )
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                error = exception.localizedMessage
                                    ?: "Could not generate a recipe."
                            } finally {
                                loading = false
                            }
                        }
                    }
                ) {
                    Text("Generate recipe")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPreferences = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val currentRecipe = recipe

    if (currentRecipe != null) {
        val alreadyCooked = currentRecipe.id in cookedIds

        AlertDialog(
            onDismissRequest = {
                if (!cooking) recipe = null
            },
            title = { Text(currentRecipe.title) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${currentRecipe.servings} serving(s) · " +
                                "${currentRecipe.preparationMinutes} min"
                    )

                    Text(currentRecipe.explanation)

                    Text(
                        "From your fridge — totals for all servings:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    currentRecipe.fridgeIngredients.forEach {
                        Text("• ${it.quantity} ${it.unit} ${it.name}")
                    }

                    Text(
                        "Additional ingredients needed:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (currentRecipe.missingIngredients.isEmpty()) {
                        Text("None listed.")
                    } else {
                        currentRecipe.missingIngredients.forEach {
                            Text("• ${it.quantity} ${it.unit} ${it.name}")
                        }
                    }

                    Text(
                        "Instructions:",
                        style = MaterialTheme.typography.titleSmall
                    )

                    currentRecipe.steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step")
                    }

                    Text(
                        "Check ingredient suitability and quantities " +
                                "before cooking."
                    )

                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (alreadyCooked) {
                        Text("Saved! Your fridge has been updated.")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !cooking && !alreadyCooked,
                    onClick = { showConfirmation = true }
                ) {
                    Text(
                        when {
                            cooking -> "Updating..."
                            alreadyCooked -> "Already recorded"
                            else -> "I cooked this"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !cooking,
                    onClick = { recipe = null }
                ) {
                    Text("Close")
                }
            }
        )

        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { showConfirmation = false },
                title = { Text("Update your fridge?") },
                text = {
                    Text(
                        "Deduct the listed fridge quantities for all " +
                                "${currentRecipe.servings} serving(s)? " +
                                "Additional ingredients are not deducted."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmation = false
                            cooking = true
                            error = null

                            service.confirmCooking(
                                userId,
                                fridgeId,
                                currentRecipe
                            ).addOnCompleteListener { task ->
                                cooking = false

                                if (task.isSuccessful) {
                                    cookedIds =
                                        cookedIds + currentRecipe.id
                                    message = "Cooking recorded."
                                } else {
                                    error = task.exception
                                        ?.localizedMessage
                                        ?: "Could not update the fridge."
                                }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmation = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}