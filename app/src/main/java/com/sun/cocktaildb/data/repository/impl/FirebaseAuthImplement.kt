package com.sun.cocktaildb.data.repository.impl

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.sun.cocktaildb.data.model.User
import com.sun.cocktaildb.data.repository.remote.AuthRepository
import com.sun.cocktaildb.utils.Constants
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class FirebaseAuthImplement(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val executor: Executor = Executors.newSingleThreadExecutor(),
) : AuthRepository {
    override fun register(
        email: String,
        password: String,
        callback: (Result<String>) -> Unit,
    ) {
        executor.execute {
            auth
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Create user profile in Firestore after successful registration
                        val user = task.result?.user
                        if (user != null) {
                            createUserProfile(user, email, callback)
                        } else {
                            callback(Result.failure(Exception("User creation failed")))
                        }
                    } else {
                        callback(Result.failure(task.exception ?: Exception(Constants.UNKNOWN_ERROR)))
                    }
                }
        }
    }

    override fun login(
        email: String,
        password: String,
        callback: (Result<String>) -> Unit,
    ) {
        executor.execute {
            auth
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback(Result.success("Success"))
                    } else {
                        callback(Result.failure(task.exception ?: Exception(Constants.UNKNOWN_ERROR)))
                    }
                }
        }
    }

    override fun logout() {
        auth.signOut()
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    /**
     * Get current Firebase user object
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Create user profile in Firestore after registration
     * @param firebaseUser Firebase Auth user object
     * @param email User's email address
     * @param callback Result callback
     */
    private fun createUserProfile(
        firebaseUser: FirebaseUser,
        email: String,
        callback: (Result<String>) -> Unit,
    ) {
        val user =
            User(
                userId = firebaseUser.uid,
                email = email,
                name = "", // Will be filled later by user
                phoneNumber = "", // Will be filled later by user
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )

        firestore
            .collection("users")
            .document(firebaseUser.uid)
            .set(user)
            .addOnSuccessListener {
                callback(Result.success("Success"))
            }.addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }

    fun getCurrentUserFavouriteCocktails(callback: (Result<List<String>?>) -> Unit) {
        val currentUser = getCurrentUser()
        if (currentUser == null) {
            callback(Result.failure(Exception("User not found")))
        } else {
            executor.execute {
                firestore
                    .collection("favourite")
                    .whereEqualTo("userID", currentUser.uid)
                    .get()
                    .addOnSuccessListener {
                        callback(
                            Result.success(
                                it.documents
                                    .map { item ->
                                        item.get("cocktails") as List<String>
                                    }.firstOrNull(),
                            ),
                        )
                    }.addOnFailureListener {
                        callback(Result.failure(it))
                    }
            }
        }
    }
}
