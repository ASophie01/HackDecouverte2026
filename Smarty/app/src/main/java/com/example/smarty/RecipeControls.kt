package com.example.smarty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.example.smarty.ui.components.*
import com.example.smarty.ui.theme.*
import com.example.smarty.ui.amountLabel
import androidx.compose.ui.unit.dp
import com.example.smarty.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RecipeControls(
    userId: String,
    fridgeId: String,
    inventory: List<FridgeItem>,
    launchRequest: Int = 0,
    buttonVisible: Boolean = true,
    onBusyChange: (Boolean) -> Unit = {}
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

    LaunchedEffect(launchRequest) {
        if (launchRequest > 0 && !loading && !cooking) {
            error = null; message = null; showPreferences = true
        }
    }
    LaunchedEffect(loading, cooking) { onBusyChange(loading || cooking) }

    Column(Modifier.fillMaxWidth()) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (buttonVisible) Button(
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
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { MessageBanner(it) }
        }

        message?.let {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) { MessageBanner(it, isError = false) }
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
                        PreferenceChoice(option.label, hunger == option) { hunger = option }
                    }

                    Text("Sweet or savory?")

                    TastePreference.entries.forEach { option ->
                        PreferenceChoice(option.label, taste == option) { taste = option }
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
        Dialog(
            onDismissRequest = { if (!cooking) recipe = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(Modifier.fillMaxSize(), color = Cream) {
                Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(enabled = !cooking, onClick = { recipe = null }) { AppGlyph("back", "Close recipe") }
                        Text("Made for your fridge", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MealIllustration(Modifier.size(156.dp)) }
                        Text(currentRecipe.title, style = MaterialTheme.typography.headlineMedium)
                        Text(currentRecipe.explanation, style = MaterialTheme.typography.bodyLarge, color = InkMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SuggestionBadge("${currentRecipe.preparationMinutes} min", "clock")
                            SuggestionBadge("${currentRecipe.servings} serving(s)", "people")
                        }
                        Text("${hunger.label} · ${taste.label}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Surface(color = PaleSage, shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("From your fridge", style = MaterialTheme.typography.titleLarge)
                                Text("Totals for all ${currentRecipe.servings} serving(s)", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                                currentRecipe.fridgeIngredients.forEach {
                                    IngredientRow(it.name, "${amountLabel(it.quantity)} ${it.unit}")
                                }
                            }
                        }
                        Surface(color = Peach.copy(alpha = 0.55f), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("You will also need", style = MaterialTheme.typography.titleMedium)
                                if(currentRecipe.missingIngredients.isEmpty()) Text("Nothing extra listed.", style = MaterialTheme.typography.bodyMedium)
                                currentRecipe.missingIngredients.forEach {
                                    IngredientRow(it.name, "${amountLabel(it.quantity)} ${it.unit}")
                                }
                            }
                        }
                        Text("Let's make it", style = MaterialTheme.typography.titleLarge)
                        currentRecipe.steps.forEachIndexed { index, step ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(color = PaleSage, shape = CircleShape, modifier = Modifier.size(30.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text("${index + 1}", style = MaterialTheme.typography.labelLarge) }
                                }
                                Text(step, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Text("Review ingredient suitability and quantities. The illustration is a general meal icon, not a photo of this recipe.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        error?.let { MessageBanner(it) }
                        if(alreadyCooked) MessageBanner("Cooking recorded. Your shared fridge is up to date.", isError = false)
                        Spacer(Modifier.height(8.dp))
                    }
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            enabled = !cooking && !alreadyCooked,
                            onClick = { showConfirmation = true },
                            modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)
                        ) {
                            AppGlyph("check", tint = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text(when { cooking -> "Updating fridge..."; alreadyCooked -> "Already recorded"; else -> "I cooked this" })
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Review quantities before updating your fridge.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                }
            }
        }
        if (showConfirmation) {
            AlertDialog(
                onDismissRequest = { showConfirmation = false },
                title = { Text("Update your fridge?") },
                text = { Text("Deduct the listed fridge quantities for all ${currentRecipe.servings} serving(s)? Additional ingredients are not deducted.") },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmation = false
                        cooking = true
                        error = null
                        service.confirmCooking(userId, fridgeId, currentRecipe).addOnCompleteListener { task ->
                            cooking = false
                            if (task.isSuccessful) {
                                cookedIds = cookedIds + currentRecipe.id
                                message = "Cooking recorded."
                            } else {
                                error = task.exception?.localizedMessage ?: "Could not update the fridge."
                            }
                        }
                    }) { Text("Confirm cooking") }
                },
                dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun SuggestionBadge(text: String, icon: String) {
    Surface(shape = RoundedCornerShape(50), color = PaleSage) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppGlyph(icon, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun IngredientRow(name: String, amount: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FoodIllustration(name, modifier = Modifier.size(38.dp))
        Text(name, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleSmall)
        Text(amount, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PreferenceChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if(selected) PaleSage else Cream,
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Text(label, Modifier.padding(start = 8.dp, top = 10.dp, bottom = 10.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
