package com.example.focusmate.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.focusmate.data.local.entity.UserEntity
import com.example.focusmate.data.repository.AuthRepository
import kotlinx.coroutines.launch

sealed class AuthResult {
    object Loading : AuthResult()
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            val success = repository.signUp(email, password)
            if (success) {
                _authResult.value = AuthResult.Success(UserEntity(email = email, password = password))
            } else {
                _authResult.value = AuthResult.Error("User already exists or DB error")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            val user = repository.signIn(email, password)
            if (user != null) {
                _authResult.value = AuthResult.Success(user)
            } else {
                _authResult.value = AuthResult.Error("Invalid credentials")
            }
        }
    }
}
