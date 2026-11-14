package com.example.smarty

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.UUID

/** Membership and invitation writes must match firestore.rules. */
class SharedFridgeService {
    private val db = FirebaseFirestore.getInstance()

    private fun requireUser(userId: String) {
        check(FirebaseAuth.getInstance().currentUser?.uid == userId) {
            "Please sign in again."
        }
    }

    private fun newCode() = UUID.randomUUID().toString().replace("-", "").uppercase()

    fun create(userId: String, name: String): Task<String> {
        requireUser(userId)
        val cleanName = name.trim()
        require(cleanName.length in 1..60) { "Use a fridge name of 1–60 characters." }
        val fridge = db.collection("fridges").document()
        val code = newCode()
        val data = mapOf("name" to cleanName, "ownerId" to userId, "inviteCode" to code)
        val batch = db.batch()
        batch.set(fridge, data)
        batch.set(fridge.collection("members").document(userId), mapOf("userId" to userId, "inviteCode" to code))
        batch.set(db.collection("fridgeInvites").document(code), mapOf("fridgeId" to fridge.id, "name" to cleanName, "ownerId" to userId))
        batch.set(db.collection("users").document(userId).collection("fridges").document(fridge.id),
            mapOf("name" to cleanName, "ownerId" to userId))
        return batch.commit().continueWith { task ->
            task.getResult(Exception::class.java)
            fridge.id
        }
    }

    fun join(userId: String, rawCode: String): Task<String> {
        requireUser(userId)
        val code = rawCode.trim().replace("-", "").uppercase()
        require(code.matches(Regex("[A-F0-9]{32}"))) { "Paste the complete invite code." }
        val invite = db.collection("fridgeInvites").document(code)
        return db.runTransaction { transaction ->
            requireUser(userId)
            val invitation = transaction.get(invite)
            val fridgeId = invitation.getString("fridgeId")
                ?: error("Invite code not found.")
            val name = invitation.getString("name") ?: error("Invalid invite.")
            val member = db.collection("fridges").document(fridgeId)
                .collection("members").document(userId)
            val existing = transaction.get(member)
            if (!existing.exists()) {
                transaction.set(member, mapOf("userId" to userId, "inviteCode" to code))
                transaction.set(db.collection("users").document(userId).collection("fridges").document(fridgeId),
                    mapOf("name" to name, "ownerId" to invitation.getString("ownerId").orEmpty()))
            }
            fridgeId
        }
    }

    /** Atomic, one-time snapshot copy. Legacy originals are deliberately retained. */
    fun ensurePersonalFridge(userId: String): Task<String> {
        requireUser(userId)
        val user = db.collection("users").document(userId)
        return user.get(Source.SERVER).continueWithTask { profileTask ->
            val profile = profileTask.getResult(Exception::class.java)
            val existingId = profile.getString("personalFridgeId")
            if (existingId != null) {
                Tasks.forResult(existingId)
            } else {
                user.collection("items").get(Source.SERVER).continueWithTask { itemsTask ->
                    val legacy = itemsTask.getResult(Exception::class.java).documents
                    require(legacy.size <= 200) {
                        "Automatic import supports 200 items. Your originals are unchanged."
                    }
                    val fridge = db.collection("fridges").document()
                    val code = newCode()
                    val name = "My personal fridge"
                    db.runTransaction { transaction ->
                        requireUser(userId)
                        // Concurrent launches share one marker; the losing attempt retries.
                        val marker = transaction.get(user).getString("personalFridgeId")
                        if (marker != null) return@runTransaction marker
                        val currentItems = legacy.map { transaction.get(it.reference) }
                        transaction.set(fridge, mapOf("name" to name, "ownerId" to userId, "inviteCode" to code))
                        transaction.set(fridge.collection("members").document(userId),
                            mapOf("userId" to userId, "inviteCode" to code))
                        transaction.set(db.collection("fridgeInvites").document(code),
                            mapOf("fridgeId" to fridge.id, "name" to name, "ownerId" to userId))
                        transaction.set(user.collection("fridges").document(fridge.id),
                            mapOf("name" to name, "ownerId" to userId))
                        currentItems.filter { it.exists() }.forEach { old ->
                            transaction.set(fridge.collection("items").document(old.id),
                                old.data.orEmpty() + ("id" to old.id))
                        }
                        transaction.set(user, mapOf(
                            "personalFridgeId" to fridge.id,
                            "legacyImportedAt" to FieldValue.serverTimestamp()
                        ), com.google.firebase.firestore.SetOptions.merge())
                        fridge.id
                    }
                }
            }
        }
    }

    fun inviteCode(fridgeId: String): Task<String> = db.collection("fridges")
        .document(fridgeId).get(Source.SERVER).continueWith { task ->
            task.getResult(Exception::class.java).getString("inviteCode")
                ?: error("No invite code found.")
        }
}
