package com.example.smarty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
    var showForm by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var deletingIds by remember { mutableStateOf(setOf<String>()) }

    // Listen for changes and stop listening when this screen closes.
    DisposableEffect(collection) {
        val listener = collection
            .orderBy("expiryDate")
            .addSnapshotListener { snapshot, exception ->
                loading = false

                if (exception != null) {
                    error = exception.localizedMessage
                } else {
                    error = null
                    fridgeItems = snapshot?.documents.orEmpty().mapNotNull {
                        it.toObject(FridgeItem::class.java)?.copy(id = it.id)
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

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showForm = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add item")
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (fridgeItems.isEmpty()) {
            Text("Your fridge is empty. Add your first item!")
        } else {
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

                            TextButton(
                                enabled = item.id !in deletingIds,
                                onClick = {
                                    error = null
                                    deletingIds = deletingIds + item.id

                                    collection.document(item.id).delete()
                                        .addOnCompleteListener { task ->
                                            deletingIds = deletingIds - item.id

                                            if (!task.isSuccessful) {
                                                error = task.exception
                                                    ?.localizedMessage
                                                    ?: "Could not remove item."
                                            }
                                        }
                                }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        AddItemDialog(
            saving = saving,
            onDismiss = { showForm = false },
            onAdd = { name, quantity, expiryDate ->
                saving = true
                error = null

                val document = collection.document()

                val item = FridgeItem(
                    id = document.id,
                    name = name,
                    quantity = quantity,
                    unit = "item(s)",
                    expiryDate = expiryDate
                )

                document.set(item)
                    .addOnCompleteListener { task ->
                        saving = false

                        if (task.isSuccessful) {
                            showForm = false
                        } else {
                            error = task.exception?.localizedMessage
                                ?: "Could not save item."
                            showForm = false
                        }
                    }
            }
        )
    }
}

@Composable
private fun AddItemDialog(
    saving: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, Double, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        title = { Text("Add fridge item") },
        text = {
            Column(
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
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry date: YYYY-MM-DD") },
                    singleLine = true,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val amount = quantity.replace(',', '.').toDoubleOrNull()
                    val dateText = expiryDate.trim()

                    val validDate =
                        dateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                                runCatching {
                                    LocalDate.parse(dateText)
                                }.isSuccess

                    error = when {
                        name.isBlank() -> "Enter an item name."

                        amount == null || !amount.isFinite() ||
                                amount <= 0 -> "Enter a quantity greater than zero."

                        !validDate -> "Enter a real date using YYYY-MM-DD."

                        else -> null
                    }

                    if (error == null && amount != null) {
                        onAdd(name.trim(), amount, dateText)
                    }
                }
            ) {
                Text(if (saving) "Saving..." else "Add")
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