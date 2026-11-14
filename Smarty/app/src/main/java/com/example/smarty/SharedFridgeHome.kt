package com.example.smarty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import com.example.smarty.ui.components.*
import com.example.smarty.ui.theme.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.smarty.model.Fridge
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SharedFridgeHome(userId: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val service = remember { SharedFridgeService() }
    val preferences = remember { context.getSharedPreferences("selected_fridges", 0) }
    var fridges by remember { mutableStateOf<List<Fridge>>(emptyList()) }
    var selectedId by remember { mutableStateOf((context as? android.app.Activity)?.intent?.getStringExtra("openFridgeId")
        ?: preferences.getString(userId, null)) }
    var ready by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showManager by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var code by remember { mutableStateOf<String?>(null) }

    fun select(id: String) {
        selectedId = id
        preferences.edit().putString(userId, id).apply()
        showManager = false
    }

    fun initialize() {
        busy = true
        error = null
        service.ensurePersonalFridge(userId).addOnCompleteListener { task ->
            busy = false
            if (task.isSuccessful) {
                if (selectedId == null) select(task.result)
                ready = true
            } else {
                error = task.exception?.localizedMessage ?: "Could not prepare your fridge."
            }
        }
    }

    DisposableEffect(userId) {
        // Retire the old user-only schedule; shared schedules are per fridge.
        cancelLegacyExpiryReminders(context)
        val listener = FirebaseFirestore.getInstance().collection("users")
            .document(userId).collection("fridges")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    error = exception.localizedMessage
                } else {
                    fridges = snapshot?.documents.orEmpty().map { document ->
                        Fridge(
                            id = document.id,
                            name = document.getString("name").orEmpty(),
                            ownerId = document.getString("ownerId").orEmpty()
                        )
                    }.sortedBy { it.name.lowercase() }
                }
            }
        initialize()
        onDispose { listener.remove() }
    }

    LaunchedEffect(fridges, ready) {
        if (ready && fridges.isNotEmpty() && fridges.none { it.id == selectedId }) {
            select(fridges.first().id)
        }
    }

    val selected = fridges.firstOrNull { it.id == selectedId }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            SmartyBrand()
            Text("Less waste.\nMore meals.", style = MaterialTheme.typography.labelMedium, color = InkMuted)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = { showManager = true }, enabled = ready && !busy,
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                color = Cream, border = BorderStroke(1.dp, WarmOutline)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AppGlyph("people")
                    Column(Modifier.weight(1f)) {
                        Text(selected?.name ?: "Your fridges", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Tap to switch, create or join", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    }
                    AppGlyph("chevron", label = "Choose a fridge")
                }
            }
            FilledTonalIconButton(
                enabled = selected != null && !busy,
                onClick = {
                    val current = selected
                    if (current != null) {
                        busy = true
                        service.inviteCode(current.id).addOnCompleteListener { task ->
                            busy = false
                            if (task.isSuccessful) code = task.result
                            else error = task.exception?.localizedMessage
                        }
                    }
                }
            ) { AppGlyph("plus", label = "Invite someone to this fridge") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
        }
        if (!ready) {
            Column(Modifier.padding(20.dp)) {
                if (busy) {
                    CircularProgressIndicator()
                    Text("Preparing your personal fridge and copying existing items...")
                } else {
                    Text("Publish the included Firestore rules before continuing.")
                    Button(onClick = { initialize() }) { Text("Retry") }
                    TextButton(onClick = onLogout) { Text("Log out") }
                }
            }
        } else if (selected != null) {
            Box(Modifier.weight(1f)) {
                // Switching destroys old forms and AI jobs, so they cannot target another fridge.
                key(selected.id) {
                    FridgeScreen(userId, selected.id, selected.name, onLogout)
                }
            }
        } else {
            CircularProgressIndicator(Modifier.padding(20.dp))
        }
    }

    if (showManager) {
        AlertDialog(
            onDismissRequest = { showManager = false },
            title = { Text("Your fridges") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    fridges.forEach { fridge ->
                        TextButton(onClick = { select(fridge.id) }) {
                            Text(if (fridge.id == selectedId) "✓ ${fridge.name}" else fridge.name)
                        }
                    }
                    Button(onClick = { action = "create"; input = ""; error = null; showManager = false }) {
                        Text("Create a fridge")
                    }
                    TextButton(onClick = { action = "join"; input = ""; error = null; showManager = false }) {
                        Text("Join with a code")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showManager = false }) { Text("Close") } }
        )
    }

    val currentAction = action
    if (currentAction != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) action = null },
            title = { Text(if (currentAction == "create") "Create a fridge" else "Join a fridge") },
            text = {
                Column {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text(if (currentAction == "create") "Fridge name" else "Invite code") },
                        enabled = !busy,
                        singleLine = true
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && input.isNotBlank(),
                    onClick = {
                        error = null
                        try {
                            val task = if (currentAction == "create") service.create(userId, input)
                            else service.join(userId, input)
                            busy = true
                            task.addOnCompleteListener { result ->
                                busy = false
                                if (result.isSuccessful) {
                                    select(result.result)
                                    action = null
                                } else {
                                    error = result.exception?.localizedMessage ?: "Please try again."
                                }
                            }
                        } catch (exception: Exception) {
                            busy = false
                            error = exception.localizedMessage
                        }
                    }
                ) { Text(if (busy) "Please wait..." else "Continue") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { action = null; error = null }) { Text("Cancel") }
            }
        )
    }

    code?.let { invite ->
        AlertDialog(
            onDismissRequest = { code = null },
            title = { Text("Good food is better shared") },
            text = { Text("Send this code to your family or roommates. Anyone signed in with the code can join.\n\n$invite") },
            confirmButton = {
                TextButton(onClick = { clipboard.setText(AnnotatedString(invite)); code = null }) { Text("Copy code") }
            },
            dismissButton = { TextButton(onClick = { code = null }) { Text("Close") } }
        )
    }
}
