package com.example.smarty

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.smarty.ui.components.*
import com.example.smarty.ui.theme.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "expiry_reminders"
private const val NOTIFICATION_ID = 1001
private const val DAILY_WORK = "daily_expiry_check"
private const val TEST_WORK = "manual_expiry_check"
private const val SHARED_TAG = "shared_fridge_expiry"

private fun notificationAllowed(context: Context): Boolean {
    return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
}

private fun reminderConstraints(): Constraints {
    return Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}

fun enableExpiryReminders(context: Context, userId: String, fridgeId: String) {
    val request = PeriodicWorkRequestBuilder<ExpiryWorker>(
        24, TimeUnit.HOURS
    )
        .setInputData(workDataOf("userId" to userId, "fridgeId" to fridgeId))
        .addTag(SHARED_TAG)
        .setConstraints(reminderConstraints())
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "${DAILY_WORK}_${userId}_$fridgeId",
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun checkExpiryNow(context: Context, userId: String, fridgeId: String) {
    val request = OneTimeWorkRequestBuilder<ExpiryWorker>()
        .setInputData(workDataOf("userId" to userId, "fridgeId" to fridgeId))
        .addTag(SHARED_TAG)
        .setConstraints(reminderConstraints())
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "${TEST_WORK}_${userId}_$fridgeId",
        ExistingWorkPolicy.REPLACE,
        request
    )
}

fun cancelLegacyExpiryReminders(context: Context) {
    WorkManager.getInstance(context).apply {
        cancelUniqueWork(DAILY_WORK)
        cancelUniqueWork(TEST_WORK)
    }
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}

fun disableExpiryReminders(context: Context, userId: String? = null, fridgeId: String? = null) {
    val manager = WorkManager.getInstance(context)
    if (userId != null && fridgeId != null) {
        manager.cancelUniqueWork("${DAILY_WORK}_${userId}_$fridgeId")
        manager.cancelUniqueWork("${TEST_WORK}_${userId}_$fridgeId")
        NotificationManagerCompat.from(context).cancel(fridgeId, NOTIFICATION_ID)
    } else {
        manager.cancelAllWorkByTag(SHARED_TAG)
        cancelLegacyExpiryReminders(context)
        NotificationManagerCompat.from(context).cancelAll()
    }
}

class ExpiryWorker(
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        val userId = inputData.getString("userId")
            ?: return Result.failure()

        val fridgeId = inputData.getString("fridgeId") ?: return Result.success()

        // Never read another account's inventory.
        if (FirebaseAuth.getInstance().currentUser?.uid != userId) {
            return Result.success()
        }

        if (!notificationAllowed(applicationContext)) {
            return Result.success()
        }

        return try {
            val today = LocalDate.now()
            val lastDay = today.plusDays(3)

            val fridge = FirebaseFirestore.getInstance().collection("fridges").document(fridgeId)
            val membership = Tasks.await(fridge.collection("members").document(userId).get(Source.SERVER), 30, TimeUnit.SECONDS)
            if (!membership.exists()) return Result.success()
            val fridgeName = Tasks.await(fridge.get(Source.SERVER), 30, TimeUnit.SECONDS).getString("name") ?: "Your fridge"
            val query = fridge.collection("items")
                .whereGreaterThanOrEqualTo(
                    "expiryDate",
                    today.toString()
                )
                .whereLessThanOrEqualTo(
                    "expiryDate",
                    lastDay.toString()
                )

            // Read current server data, including edits and deletions.
            val snapshot = Tasks.await(
                query.get(Source.SERVER),
                30,
                TimeUnit.SECONDS
            )

            if (isStopped ||
                FirebaseAuth.getInstance().currentUser?.uid != userId
            ) {
                return Result.success()
            }

            val names = snapshot.documents.mapNotNull {
                it.getString("name")?.takeIf { name ->
                    name.isNotBlank()
                }
            }

            if (names.isEmpty()) {
                NotificationManagerCompat.from(applicationContext)
                    .cancel(fridgeId, NOTIFICATION_ID)
            } else {
                showReminder(names, fridgeId, fridgeName)
            }

            Result.success()
        } catch (exception: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun showReminder(names: List<String>, fridgeId: String, fridgeName: String) {
        val context = applicationContext

        if (!notificationAllowed(context)) return

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Food expiry reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            data = android.net.Uri.parse("smarty://fridge/$fridgeId")
            putExtra("openFridgeId", fridgeId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val preview = names.take(3).joinToString(", ")
        val extra = if (names.size > 3) {
            " and ${names.size - 3} more"
        } else {
            ""
        }

        val message =
            "$preview$extra: entered expiry dates are within 3 days."

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Check $fridgeName")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(message)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(fridgeId, NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked during the check.
        }
    }
}

@Composable
fun ReminderControls(userId: String, fridgeId: String) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf("") }

    fun runAction(action: String) {
        if (action == "enable") {
            enableExpiryReminders(context, userId, fridgeId)
            message = "Daily reminders enabled for this fridge on this device."
        } else {
            checkExpiryNow(context, userId, fridgeId)
            message = "Check queued. A notification appears if items qualify."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runAction(pendingAction)
        } else {
            message = "Notifications are disabled. Allow them in app settings."
        }
    }

    fun requestAction(action: String) {
        if (notificationAllowed(context)) {
            runAction(action)
        } else if (Build.VERSION.SDK_INT >= 33) {
            pendingAction = action
            permissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    Surface(color = PaleSage, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("A gentle nudge", style = MaterialTheme.typography.titleLarge)
            Text("Get a reminder when an entered expiry date is within three days.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { requestAction("enable") }, modifier = Modifier.fillMaxWidth()) { Text("Enable daily reminders") }
            OutlinedButton(onClick = { requestAction("check") }, modifier = Modifier.fillMaxWidth()) { Text("Check expiry now") }
            TextButton(onClick = {
                disableExpiryReminders(context, userId, fridgeId)
                message = "Reminders disabled for this fridge on this device."
            }) { Text("Disable for this fridge") }
            if (message.isNotEmpty()) Text(message, style = MaterialTheme.typography.bodyMedium)
            Text("This device only · Internet required · Android may delay daily checks.", style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
}
