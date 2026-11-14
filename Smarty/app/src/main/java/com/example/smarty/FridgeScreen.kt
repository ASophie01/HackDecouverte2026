package com.example.smarty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.smarty.ui.*
import com.example.smarty.ui.components.*
import com.example.smarty.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smarty.model.FridgeItem
import com.example.smarty.model.ScannedProduct
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

@Composable
fun FridgeScreen(
    userId: String,
    fridgeId: String,
    fridgeName: String,
    onLogout: () -> Unit
) {
    val collection = remember(fridgeId) {
        FirebaseFirestore.getInstance()
            .collection("fridges")
            .document(fridgeId)
            .collection("items")
    }

    var fridgeItems by remember(userId) {
        mutableStateOf<List<FridgeItem>>(emptyList())
    }
    var loading by remember(userId) { mutableStateOf(true) }
    var error by remember(userId) { mutableStateOf<String?>(null) }

    var tab by rememberSaveable { mutableStateOf(0) }
    var category by rememberSaveable { mutableStateOf("All") }
    var recipeRequest by remember { mutableStateOf(0) }
    var recipeBusy by remember { mutableStateOf(false) }
    var removeCandidate by remember { mutableStateOf<FridgeItem?>(null) }
    var pendingProduct by remember { mutableStateOf<ScannedProduct?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<FridgeItem?>(null) }
    var saving by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var deletingIds by remember { mutableStateOf(setOf<String>()) }

    DisposableEffect(collection) {
        val listener = collection
            .orderBy("expiryDate")
            .addSnapshotListener { snapshot, exception ->
                loading = false

                if (exception != null) {
                    error = exception.localizedMessage
                } else {
                    error = null
                    fridgeItems = snapshot?.documents.orEmpty()
                        .mapNotNull { document ->
                            document.toObject(FridgeItem::class.java)
                                ?.copy(id = document.id)
                        }
                }
            }

        onDispose {
            listener.remove()
        }
    }

    fun editItem(item: FridgeItem) {
        pendingProduct = null
        selectedItem = item
        formError = null
        showForm = true
    }

    fun openRecipes() { if (!recipeBusy) recipeRequest++ }
    val soon = fridgeItems.filter { isUseSoon(it) }.sortedBy { it.expiryDate }
    val visible = fridgeItems.filter { category == "All" || foodKind(it.name, it.category).group == category }

    Scaffold(
        containerColor = Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(containerColor = Cream, tonalElevation = 0.dp) {
                listOf("Fridge" to "fridge", "Recipes" to "recipe", "Settings" to "settings").forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == index, onClick = { tab = index },
                        icon = { AppGlyph(icon) }, label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = PaleSage, selectedTextColor = Forest, unselectedTextColor = InkMuted)
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        selectedItem = null; pendingProduct = null; formError = null; showForm = true
                    }, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)) {
                        AppGlyph("plus", tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp)); Text("Add item")
                    }
                    BarcodeLookupControls { product ->
                        selectedItem = null; pendingProduct = product; formError = null; showForm = true
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            error?.let { Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { MessageBanner(it) } }
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text("My fridge", style = MaterialTheme.typography.headlineLarge)
                            Text("A little less waste. A lot more possibility.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        }
                        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                        if (!loading && soon.isNotEmpty()) item {
                            Surface(color = Peach, shape = RoundedCornerShape(24.dp)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Use soon", style = MaterialTheme.typography.headlineSmall)
                                    Text("Good food deserves a second chance.", style = MaterialTheme.typography.bodyMedium)
                                    soon.take(3).forEach { item ->
                                        Surface(onClick = { editItem(item) }, enabled = item.id !in deletingIds, color = Cream, shape = RoundedCornerShape(18.dp)) {
                                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                FoodIllustration(item.name, item.category, Modifier.size(46.dp))
                                                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                                    Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("${amountLabel(item.quantity)} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                                                }
                                                ExpiryBadge(item)
                                            }
                                        }
                                    }
                                    if (soon.size > 3) Text("${soon.size - 3} more in your list below", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        item {
                            Button(onClick = { openRecipes() }, enabled = fridgeItems.isNotEmpty() && !recipeBusy,
                                modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                                AppGlyph("sparkle", tint = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(10.dp)); Text(if(recipeBusy) "Finding something delicious..." else "What can I cook?")
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("All items", style = MaterialTheme.typography.titleLarge)
                                Text("${fridgeItems.size} item${if(fridgeItems.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                            }
                            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("All", "Dairy", "Veggies", "Fruit", "Protein", "Pantry").forEach { group ->
                                    FilterChip(selected = category == group, onClick = { category = group }, label = { Text(group) })
                                }
                            }
                        }
                        if (!loading && visible.isEmpty()) item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                FoodIllustration("", modifier = Modifier.size(90.dp))
                                Text(if(fridgeItems.isEmpty()) "Fresh start, fresh fridge." else "Nothing in this category yet.", style = MaterialTheme.typography.titleMedium)
                                Text("Add an item or scan your groceries.", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        items(visible, key = { it.id }) { item ->
                            InventoryItemCard(item, item.id in deletingIds, onEdit = { editItem(item) }, onRemove = { removeCandidate = item })
                        }
                    }
                    1 -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Something delicious awaits", style = MaterialTheme.typography.headlineMedium)
                        MealIllustration(Modifier.size(180.dp))
                        Text("Made for your fridge", style = MaterialTheme.typography.titleLarge)
                        Text("Tell us what you're craving. We'll look for a recipe using what you have, with ingredients to use soon first.",
                            style = MaterialTheme.typography.bodyLarge, color = InkMuted)
                        HintCard("Your ingredients, your appetite", "Choose sweet or savory, how hungry you are, and how many people are eating.")
                        Button(onClick = { openRecipes() }, enabled = fridgeItems.isNotEmpty() && !recipeBusy,
                            modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                            Text(if(recipeBusy) "Finding a recipe..." else "Find a recipe")
                        }
                        if(fridgeItems.isEmpty()) Text("Add a fridge item to get started.", color = InkMuted)
                        Text("Ideas are AI-generated. Review ingredients and quantities before cooking.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                    else -> Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text("Make yourself at home", style = MaterialTheme.typography.headlineMedium)
                        Text("A few helpful things, all in one place.", color = InkMuted)
                        ReminderControls(userId = userId, fridgeId = fridgeId)
                        HintCard("Your active fridge", fridgeName)
                        Surface(color = PaleSage, shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Your account", style = MaterialTheme.typography.titleMedium)
                                Text(FirebaseAuth.getInstance().currentUser?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                                OutlinedButton(onClick = onLogout) { Text("Log out") }
                            }
                        }
                        Text("Smarty · Less waste. More meals.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                }
            }
            // Kept mounted across tabs so an in-flight recipe request isn't discarded.
            RecipeControls(userId = userId, fridgeId = fridgeId, inventory = fridgeItems,
                launchRequest = recipeRequest, buttonVisible = false, onBusyChange = { recipeBusy = it })
        }
    }

    removeCandidate?.let { item ->
        AlertDialog(onDismissRequest = { removeCandidate = null }, title = { Text("Remove ${item.name}?") },
            text = { Text("This removes it from $fridgeName for everyone with access.") },
            confirmButton = { TextButton(onClick = {
                removeCandidate = null; error = null; deletingIds = deletingIds + item.id
                collection.document(item.id).delete().addOnCompleteListener { task ->
                    deletingIds = deletingIds - item.id
                    if (!task.isSuccessful) error = task.exception?.localizedMessage ?: "Could not remove item."
                }
            }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { removeCandidate = null }) { Text("Keep item") } })
    }

    if (showForm) {
        key(selectedItem?.id, pendingProduct?.barcode) {
            ItemFormDialog(
                initialItem = selectedItem,
                prefill = pendingProduct,
                saving = saving,
                saveError = formError,
                onDismiss = { if (!saving) showForm = false },
                onSave = { name, quantity, unit, expiryDate ->
                    saving = true
                    formError = null

                    val existingItem = selectedItem

                    // Update an existing document or create a new one.
                    val document = if (existingItem != null) {
                        collection.document(existingItem.id)
                    } else {
                        collection.document()
                    }

                    // Copy preserves fields such as barcode and image URL.
                    val updatedItem = (existingItem ?: FridgeItem(
                        barcode = pendingProduct?.barcode.orEmpty(),
                        imageUrl = pendingProduct?.imageUrl.orEmpty()
                    )).copy(
                        id = document.id,
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        expiryDate = expiryDate
                    )

                    document.set(updatedItem)
                        .addOnCompleteListener { task ->
                            saving = false

                            if (task.isSuccessful) {
                                showForm = false
                                selectedItem = null
                            } else {
                                formError = task.exception?.localizedMessage
                                    ?: "Could not save item. Try again."
                            }
                        }
                }
            )
        }
    }
}

@Composable
private fun ItemFormDialog(
    initialItem: FridgeItem?,
    prefill: ScannedProduct?,
    saving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable {
        mutableStateOf(initialItem?.name ?: prefill?.name.orEmpty())
    }

    var quantity by rememberSaveable {
        mutableStateOf(initialItem?.quantity?.toString() ?: "1")
    }

    var unit by rememberSaveable {
        mutableStateOf(initialItem?.unit ?: "item(s)")
    }

    var expiryDate by rememberSaveable {
        mutableStateOf(initialItem?.expiryDate.orEmpty())
    }

    var validationError by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        title = {
            Text(
                if (initialItem == null) {
                    "Add to your fridge"
                } else {
                    "Edit your item"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val barcode = initialItem?.barcode ?: prefill?.barcode.orEmpty()
                if (barcode.isNotBlank()) Text("Barcode: $barcode")
                prefill?.brand?.takeIf { it.isNotBlank() }?.let { Text("Brand: $it") }
                prefill?.packageQuantity?.takeIf { it.isNotBlank() }?.let {
                    Text("Package label: $it (reference only)")
                }
                if (prefill != null) {
                    Text("Confirm the product, then enter the amount you own and the date printed on your package.")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    singleLine = true,
                    enabled = !saving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit: items, g, kg, mL...") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry date: YYYY-MM-DD") },
                    trailingIcon = {
                        IconButton(enabled = !saving, onClick = {
                            val current = runCatching { LocalDate.parse(expiryDate) }.getOrDefault(LocalDate.now())
                            android.app.DatePickerDialog(context, { _, year, month, day ->
                                expiryDate = LocalDate.of(year, month + 1, day).toString()
                            }, current.year, current.monthValue - 1, current.dayOfMonth).show()
                        }) { AppGlyph("calendar", label = "Choose expiry date") }
                    },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )

                (validationError ?: saveError)?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val amount = quantity
                        .replace(',', '.')
                        .toDoubleOrNull()

                    val dateText = expiryDate.trim()

                    val validDate =
                        dateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                                runCatching {
                                    LocalDate.parse(dateText)
                                }.isSuccess

                    validationError = when {
                        name.isBlank() ->
                            "Enter an item name."

                        amount == null || !amount.isFinite() || amount <= 0 ->
                            "Enter a quantity greater than zero."

                        unit.isBlank() ->
                            "Enter a unit."

                        !validDate ->
                            "Enter a real date using YYYY-MM-DD."

                        else -> null
                    }

                    if (validationError == null && amount != null) {
                        onSave(
                            name.trim(),
                            amount,
                            unit.trim(),
                            dateText
                        )
                    }
                }
            ) {
                Text(if (saving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !saving,
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InventoryItemCard(item: FridgeItem, busy: Boolean, onEdit: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxWidth(), color = Cream, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, WarmOutline)) {
        Row(Modifier.padding(start = 10.dp, top = 12.dp, bottom = 12.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            FoodIllustration(item.name, item.category)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${amountLabel(item.quantity)} ${item.unit}", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                ExpiryBadge(item)
            }
            Box {
                IconButton(onClick = { menu = true }, enabled = !busy) { AppGlyph("more", label = "Item actions for ${item.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit item") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Remove", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onRemove() })
                }
            }
        }
    }
}
