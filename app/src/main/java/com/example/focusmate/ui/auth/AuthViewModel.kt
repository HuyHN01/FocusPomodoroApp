// File: ui/auth/AuthViewModel.kt
package com.example.focusmate.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.focusmate.data.repository.AuthRepository
import com.example.focusmate.data.repository.AuthResultWrapper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

// Sealed class của em đã tốt, thầy đổi Success để chứa FirebaseUser
sealed class AuthResult {
    data object Loading : AuthResult()
    data class Success(val user: FirebaseUser) : AuthResult() // Trả về FirebaseUser
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    // Thêm một LiveData để kiểm tra trạng thái đăng nhập ban đầu
    private val _userLoggedIn = MutableLiveData<FirebaseUser?>()
    val userLoggedIn: LiveData<FirebaseUser?> = _userLoggedIn

    init {
        // Kiểm tra ngay khi ViewModel được tạo
        _userLoggedIn.value = repository.getCurrentFirebaseUser()
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            when (val result = repository.signUp(email, password)) {
                is AuthResultWrapper.Success -> {
                    Log.d("AuthViewModel", "Sign Up Success, UID: ${result.data.uid}")
                    _authResult.value = AuthResult.Success(result.data)
                }
                is AuthResultWrapper.Error -> {
                    Log.e("AuthViewModel", "Sign Up Error: ${result.message}")
                    _authResult.value = AuthResult.Error(result.message)
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            when (val result = repository.signIn(email, password)) {
                is AuthResultWrapper.Success -> {
                    Log.d("AuthViewModel", "Sign In Success, UID: ${result.data.uid}")
                    _authResult.value = AuthResult.Success(result.data)
                }
                is AuthResultWrapper.Error -> {
                    Log.d("AuthViewModel", "Sign In Success, UID: ${result.message}")
                    _authResult.value = AuthResult.Error(result.message)
                }
            }
        }
    }
}