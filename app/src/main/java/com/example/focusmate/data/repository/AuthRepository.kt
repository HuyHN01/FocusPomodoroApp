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

// Lớp Result để bọc kết quả trả về
sealed class AuthResultWrapper<out T> {
    data class Success<out T>(val data: T) : AuthResultWrapper<T>()
    data class Error(val message: String) : AuthResultWrapper<Nothing>()
}

class AuthRepository(context: Context) {

    private val userDao: UserDao = AppDatabase.getDatabase(context).userDao()
    private val firebaseAuth: FirebaseAuth = Firebase.auth

    // Kiểm tra xem user đã đăng nhập từ trước chưa
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    suspend fun signUp(email: String, password: String): AuthResultWrapper<FirebaseUser> {
        return try {
            // 1. Gọi Firebase Auth để tạo user
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // 2. Tạo UserEntity để cache vào Room
                val userEntity = UserEntity(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                userDao.cacheUser(userEntity)

                AuthResultWrapper.Success(firebaseUser)
            } else {
                AuthResultWrapper.Error("Không thể tạo người dùng, user trả về null.")
            }
        } catch (e: Exception) {
            // e.message sẽ chứa lỗi (ví dụ: email đã tồn tại, mật khẩu quá yếu)
            AuthResultWrapper.Error(e.message ?: "Đăng ký thất bại.")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResultWrapper<FirebaseUser> {
        return try {
            // 1. Gọi Firebase Auth để đăng nhập
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // 2. Cache thông tin user vào Room
                val userEntity = UserEntity(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                userDao.cacheUser(userEntity)

                AuthResultWrapper.Success(firebaseUser)
            } else {
                AuthResultWrapper.Error("Không thể đăng nhập, user trả về null.")
            }
        } catch (e: Exception) {
            // e.message sẽ chứa lỗi (ví dụ: sai mật khẩu, user không tồn tại)
            AuthResultWrapper.Error(e.message ?: "Đăng nhập thất bại.")
        }
    }

    suspend fun signOut() {
        // 1. Đăng xuất khỏi Firebase
        firebaseAuth.signOut()
        // 2. Xóa cache user khỏi Room
        userDao.clearCachedUser()
    }
}
