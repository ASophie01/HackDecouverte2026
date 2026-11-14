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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.smarty.model.FridgeItem
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

@Composable
fun FridgeScreen(
    userId: String,
    onLogout: () -> Unit
) {
    val collection = remember(userId) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("items")
    }

    var fridgeItems by remember(userId) {
        mutableStateOf<List<FridgeItem>>(emptyList())
    }
    var loading by remember(userId) { mutableStateOf(true) }
    var error by remember(userId) { mutableStateOf<String?>(null) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "My fridge",
                style = MaterialTheme.typography.headlineMedium
            )

            TextButton(onClick = onLogout) {
                Text("Log out")
            }
        }

        Text("Use what you have. Waste less.")
        ReminderControls(userId = userId)
        RecipeControls(
            userId = userId,
            inventory = fridgeItems
        )

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                selectedItem = null
                formError = null
                showForm = true
            }
        ) {
            Text("+ Add item")
        }

        error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> CircularProgressIndicator()

            fridgeItems.isEmpty() -> {
                Text("Your fridge is empty. Add your first item!")
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fridgeItems, key = { it.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text("${item.quantity} ${item.unit}")
                                Text("Expiry date: ${item.expiryDate}")

                                val date = runCatching {
                                    LocalDate.parse(item.expiryDate)
                                }.getOrNull()

                                val today = LocalDate.now()

                                when {
                                    date == null -> Unit

                                    date.isBefore(today) -> Text(
                                        text = "Past the entered date",
                                        color = MaterialTheme.colorScheme.error
                                    )

                                    !date.isAfter(today.plusDays(3)) -> Text(
                                        text = "Use soon",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row {
                                    TextButton(
                                        enabled = item.id !in deletingIds,
                                        onClick = {
                                            selectedItem = item
                                            formError = null
                                            showForm = true
                                        }
                                    ) {
                                        Text("Edit")
                                    }

                                    TextButton(
                                        enabled = item.id !in deletingIds,
                                        onClick = {
                                            error = null
                                            deletingIds = deletingIds + item.id

                                            collection.document(item.id)
                                                .delete()
                                                .addOnCompleteListener { task ->
                                                    deletingIds =
                                                        deletingIds - item.id

                                                    if (!task.isSuccessful) {
                                                        error = task.exception
                                                            ?.localizedMessage
                                                            ?: "Could not remove item."
                                                    }
                                                }
                                        }
                                    ) {
                                        Text(
                                            if (item.id in deletingIds) {
                                                "Removing..."
                                            } else {
                                                "Remove"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        key(selectedItem?.id) {
            ItemFormDialog(
                initialItem = selectedItem,
                saving = saving,
                saveError = formError,
                onDismiss = { showForm = false },
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
                    val updatedItem = (existingItem ?: FridgeItem()).copy(
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
    saving: Boolean,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String) -> Unit
) {
    var name by rememberSaveable {
        mutableStateOf(initialItem?.name.orEmpty())
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
                    "Add fridge item"
                } else {
                    "Edit fridge item"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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