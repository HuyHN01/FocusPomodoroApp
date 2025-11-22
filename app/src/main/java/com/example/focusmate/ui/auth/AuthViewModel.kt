
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


sealed class AuthResult {
    data object Loading : AuthResult()
    data class Success(val user: FirebaseUser) : AuthResult() 
    data class Error(val message: String) : AuthResult()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    
    private val _userLoggedIn = MutableLiveData<FirebaseUser?>()
    val userLoggedIn: LiveData<FirebaseUser?> = _userLoggedIn

    private val _verificationResult = MutableLiveData<AuthResultWrapper<String>>()
    val verificationResult: LiveData<AuthResultWrapper<String>> = _verificationResult

    private val _resetPasswordResult = MutableLiveData<AuthResultWrapper<String>>()
    val resetPasswordResult: LiveData<AuthResultWrapper<String>> = _resetPasswordResult



    init {
        
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

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authResult.value = AuthResult.Loading
            
            val result = repository.signInWithGoogle(idToken)

            
            when (result) {
                is AuthResultWrapper.Success -> {
                    _authResult.value = AuthResult.Success(result.data)
                }
                is AuthResultWrapper.Error -> {
                    _authResult.value = AuthResult.Error(result.message)
                }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            
            val result = repository.sendVerificationEmail()
            _verificationResult.value = result
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            
            val result = repository.sendPasswordResetEmail(email)
            _resetPasswordResult.value = result
        }
    }
}