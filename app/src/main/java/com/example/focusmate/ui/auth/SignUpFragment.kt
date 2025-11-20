package com.example.focusmate.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focusmate.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText

@Suppress("DEPRECATION")
class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-Up failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<TextInputEditText>(R.id.email_or_username_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.confirm_password_input)
        val signUpButton = view.findViewById<AppCompatButton>(R.id.sign_up_button)

        
        val googleButton = view.findViewById<View>(R.id.ic_google)

        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        
        signUpButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password == confirmPassword) {
                viewModel.signUp(email, password)
            } else {
                Toast.makeText(context, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
            }
        }

        
        googleButton.setOnClickListener {
            signInGoogle()
        }

        
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    
                }
                is AuthResult.Success -> {
                    
                    
                    

                    if (result.user.isEmailVerified) {
                        
                        Toast.makeText(context, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show()
                        requireActivity().setResult(AppCompatActivity.RESULT_OK)
                        requireActivity().finish()
                    } else {
                        
                        showVerificationDialog()
                    }
                }
                is AuthResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun showVerificationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Đăng ký thành công")
            .setMessage("Một email xác thực đã được gửi đến hộp thư của bạn. Vui lòng xác thực trước khi đăng nhập.")
            .setPositiveButton("Đã hiểu") { dialog, _ ->
                dialog.dismiss()
                if (activity is AuthActivity) {
                    (activity as AuthActivity).navigateToSignIn()
                }
            }
            .setCancelable(false)
            .show()
    }
}