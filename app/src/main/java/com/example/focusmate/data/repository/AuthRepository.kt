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

const val ERROR_EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"
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
            // 1. Tạo user trên Firebase
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // 2. Gửi email xác thực NGAY LẬP TỨC
                try {
                    firebaseUser.sendEmailVerification().await()
                } catch (e: Exception) {
                    // Nếu gửi mail lỗi, ta vẫn trả về user nhưng UI sẽ cảnh báo
                    // Hoặc có thể return Error tùy nghiệp vụ. Ở đây Thầy cho return Success để user biết đã tạo tk
                }

                // LƯU Ý: Ở bước đăng ký, ta KHÔNG lưu vào Room ngay,
                // vì user chưa xác thực. Ta chỉ trả về Success để UI hiện thông báo.
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
                // 3. QUAN TRỌNG: Reload để lấy trạng thái mới nhất từ Server
                // Nếu không reload, isEmailVerified có thể bị cache là false dù user đã click link
                firebaseUser.reload().await()

                if (firebaseUser.isEmailVerified) {
                    // Đã xác thực -> Lưu vào Room -> Cho phép đăng nhập
                    val userEntity = UserEntity(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
                    userDao.cacheUser(userEntity)
                    AuthResultWrapper.Success(firebaseUser)
                } else {
                    // Chưa xác thực -> Trả về lỗi đặc biệt
                    // Không lưu vào Room
                    AuthResultWrapper.Error(ERROR_EMAIL_NOT_VERIFIED)
                }
            } else {
                AuthResultWrapper.Error("User null.")
            }
        } catch (e: Exception) {
            AuthResultWrapper.Error(e.message ?: "Đăng nhập thất bại.")
        }
    }

    suspend fun sendVerificationEmail(): AuthResultWrapper<String> {
        val user = firebaseAuth.currentUser
        return if (user != null) {
            try {
                user.sendEmailVerification().await()
                AuthResultWrapper.Success("Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư.")
            } catch (e: Exception) {
                // Xử lý lỗi quá nhiều request (FirebaseTooManyRequestsException)
                AuthResultWrapper.Error(e.message ?: "Không thể gửi email.")
            }
        } else {
            AuthResultWrapper.Error("Không tìm thấy thông tin người dùng.")
        }
    }

    suspend fun signOut() {
        // 1. Đăng xuất khỏi Firebase
        firebaseAuth.signOut()
        // 2. Xóa cache user khỏi Room
        userDao.clearCachedUser()
    }

    fun getCurrentUserId(): String {
        val firebaseUser = firebaseAuth.currentUser
        return if (firebaseUser != null) {
            firebaseUser.uid // Nếu đã đăng nhập, trả về UID
        } else {
            GUEST_USER_ID // Nếu chưa đăng nhập, trả về ID Khách
        }
    }
}
