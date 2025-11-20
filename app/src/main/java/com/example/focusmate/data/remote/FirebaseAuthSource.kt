

package com.example.focusmate.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthSource() {
    private val auth: FirebaseAuth = Firebase.auth

    suspend fun register(email: String, password: String): FirebaseUser? =
        suspendCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { cont.resume(it.user) }
                .addOnFailureListener { cont.resume(null) }
        }

    suspend fun login(email: String, password: String): FirebaseUser? =
        suspendCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { cont.resume(it.user) }
                .addOnFailureListener { cont.resume(null) }
        }

    fun logout() = auth.signOut()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser
}