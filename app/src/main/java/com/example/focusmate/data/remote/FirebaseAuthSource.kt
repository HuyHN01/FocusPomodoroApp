//Đây là lớp trung gian giao tiếp với Firebase

package com.example.focusmate.data.remote

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class FirebaseAuthSource() {
    private val auth: FirebaseAuth = Firebase.auth
}