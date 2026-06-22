package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.Category
import com.example.data.model.DailyTask
import com.example.data.model.UserStats
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirestoreManager {
    private const val TAG = "FirestoreManager"
    private const val PREFS_NAME = "firestore_sync_prefs"
    private const val KEY_USER_ID = "sync_user_id"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"

    private var db: FirebaseFirestore? = null
    private var isInitialized = false

    // Suspend helper to await Task without play-services-coroutines library dependency
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Firestore task failed"))
            }
        }
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Check if Firebase is already initialized by standard config
            FirebaseApp.getInstance()
            db = FirebaseFirestore.getInstance()
            isInitialized = true
            Log.d(TAG, "Firebase initialized safely using auto-config")
        } catch (e: Exception) {
            Log.d(TAG, "Auto-config not found, performing safe custom init: ${e.message}")
            try {
                // Initialize programmatically with fallback values to ensure zero crashes
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyB-placeholder-key-for-initiative")
                    .setApplicationId("com.aistudio.study365.vxtpzl")
                    .setProjectId("sayan-initiative-prep")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                db = FirebaseFirestore.getInstance()
                isInitialized = true
                Log.d(TAG, "Firebase initialized safely with custom fallback options")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to initialize Firebase programmatically: ${ex.message}", ex)
            }
        }
    }

    fun getUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_USER_ID, null)
        if (userId.isNullOrBlank()) {
            userId = "s_prep_" + UUID.randomUUID().toString().replace("-", "").take(6)
            prefs.edit().putString(KEY_USER_ID, userId).apply()
        }
        return userId
    }

    fun updateUserId(context: Context, newId: String): Boolean {
        val trimmed = newId.trim()
        if (trimmed.length < 3) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_ID, trimmed).apply()
        return true
    }

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    private fun saveLastSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, time).apply()
    }

    suspend fun uploadData(
        context: Context,
        stats: UserStats?,
        categories: List<Category>,
        tasks: List<DailyTask>
    ): Boolean {
        initialize(context)
        val firestoreDb = db ?: return false
        val userId = getUserId(context)

        return try {
            val dataMap = hashMapOf<String, Any>(
                "userId" to userId,
                "userName" to (stats?.userName ?: ""),
                "userDob" to (stats?.userDob ?: ""),
                "profilePictureUri" to (stats?.profilePictureUri ?: ""),
                "categories" to categories.map { it.name },
                "dailyTasks" to tasks.map {
                    hashMapOf(
                        "date" to it.date,
                        "categoryName" to it.categoryName,
                        "isCompleted" to it.isCompleted
                    )
                },
                "lastUpdated" to System.currentTimeMillis()
            )

            firestoreDb.collection("users")
                .document(userId)
                .set(dataMap)
                .awaitTask()

            saveLastSyncTime(context, System.currentTimeMillis())
            Log.d(TAG, "Data successfully uploaded and backed up to Firestore for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed uploading user data to cloud Firestore: ${e.message}", e)
            false
        }
    }

    class CloudData(
        val userName: String = "",
        val userDob: String = "",
        val profilePictureUri: String? = null,
        val categories: List<String> = emptyList(),
        val dailyTasks: List<DailyTask> = emptyList()
    )

    suspend fun downloadData(context: Context, customUserId: String? = null): CloudData? {
        initialize(context)
        val firestoreDb = db ?: return null
        val targetUserId = customUserId ?: getUserId(context)

        return try {
            val doc = firestoreDb.collection("users")
                .document(targetUserId)
                .get()
                .awaitTask()

            if (!doc.exists()) {
                Log.d(TAG, "No remote cloudsync found in firestore for user: $targetUserId")
                return null
            }

            val userName = doc.getString("userName") ?: ""
            val userDob = doc.getString("userDob") ?: ""
            val profilePictureUri = doc.getString("profilePictureUri")
            
            @Suppress("UNCHECKED_CAST")
            val rawCategories = doc.get("categories") as? List<String> ?: emptyList()
            
            @Suppress("UNCHECKED_CAST")
            val rawTasks = doc.get("dailyTasks") as? List<Map<String, Any>> ?: emptyList()

            val dailyTasks = rawTasks.map { map ->
                DailyTask(
                    date = map["date"] as? String ?: "",
                    categoryName = map["categoryName"] as? String ?: "",
                    isCompleted = map["isCompleted"] as? Boolean ?: false
                )
            }

            saveLastSyncTime(context, System.currentTimeMillis())
            CloudData(
                userName = userName,
                userDob = userDob,
                profilePictureUri = profilePictureUri,
                categories = rawCategories,
                dailyTasks = dailyTasks
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading user data from cloud Firestore: ${e.message}", e)
            null
        }
    }
}
