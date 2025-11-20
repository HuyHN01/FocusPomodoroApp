package com.example.focusmate.data.repository

import android.content.Context
import com.example.focusmate.data.local.AppDatabase
import com.example.focusmate.data.local.dao.UserDao
import com.example.focusmate.data.local.entity.UserEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

import com.example.focusmate.util.Constants.GUEST_USER_ID
import com.google.firebase.auth.GoogleAuthProvider

const val ERROR_EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"

sealed class AuthResultWrapper<out T> {
    data class Success<out T>(val data: T) : AuthResultWrapper<T>()
    data class Error(val message: String) : AuthResultWrapper<Nothing>()
}

class AuthRepository(context: Context) {

    private val userDao: UserDao = AppDatabase.getDatabase(context).userDao()
    private val firebaseAuth: FirebaseAuth = Firebase.auth

    
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    suspend fun signUp(email: String, password: String): AuthResultWrapper<FirebaseUser> {
        return try {
            
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                
                try {
                    firebaseUser.sendEmailVerification().await()
                } catch (e: Exception) {
                    
                    
                }

                
                
                AuthResultWrapper.Success(firebaseUser)
            } else {
                AuthResultWrapper.Error("Không thể tạo người dùng, user trả về null.")
            }
        } catch (e: Exception) {
            AuthResultWrapper.Error(e.message ?: "Đăng ký thất bại.")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResultWrapper<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                
                
                firebaseUser.reload().await()

                if (firebaseUser.isEmailVerified) {
                    
                    val userEntity = UserEntity(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                    userDao.cacheUser(userEntity)
                    AuthResultWrapper.Success(firebaseUser)
                } else {
                    
                    
                    AuthResultWrapper.Error(ERROR_EMAIL_NOT_VERIFIED)
                }
            } else {
                AuthResultWrapper.Error("User null.")
            }
        } catch (e: Exception) {
            AuthResultWrapper.Error(e.message ?: "Đăng nhập thất bại.")
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResultWrapper<FirebaseUser> {
        return try {
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                
                val userEntity = UserEntity(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName 
                )
                userDao.cacheUser(userEntity)

                AuthResultWrapper.Success(firebaseUser)
            } else {
                AuthResultWrapper.Error("Google Sign-In thất bại: User null")
            }
        } catch (e: Exception) {
            AuthResultWrapper.Error(e.message ?: "Lỗi đăng nhập Google")
        }
    }

    suspend fun sendVerificationEmail(): AuthResultWrapper<String> {
        val user = firebaseAuth.currentUser
        return if (user != null) {
            try {
                user.sendEmailVerification().await()
                AuthResultWrapper.Success("Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư.")
            } catch (e: Exception) {
                
                AuthResultWrapper.Error(e.message ?: "Không thể gửi email.")
            }
        } else {
            AuthResultWrapper.Error("Không tìm thấy thông tin người dùng.")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResultWrapper<String> {
        return try {
            
            firebaseAuth.sendPasswordResetEmail(email).await()
            AuthResultWrapper.Success("Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.")
        } catch (e: Exception) {
            
            AuthResultWrapper.Error(e.message ?: "Không thể gửi email đặt lại mật khẩu.")
        }
    }


    suspend fun signOut() {
        
        firebaseAuth.signOut()

        
        userDao.clearCachedUser()

        
        
        
    }

    fun getCurrentUserId(): String {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            firebaseUser.uid
        } else {
            GUEST_USER_ID 
        }
    }
}
