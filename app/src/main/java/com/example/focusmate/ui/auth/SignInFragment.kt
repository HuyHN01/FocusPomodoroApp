package com.example.focusmate.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focusmate.R
import com.example.focusmate.data.repository.AuthResultWrapper
import com.google.android.material.textfield.TextInputEditText
import com.example.focusmate.data.repository.ERROR_EMAIL_NOT_VERIFIED


class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emailInput = view.findViewById<TextInputEditText>(R.id.email_or_username_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val signInButton = view.findViewById<AppCompatButton>(R.id.sign_in_button)

        signInButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            viewModel.signIn(email, password)
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> { /* Show Loading */ }
                is AuthResult.Success -> {
                    requireActivity().setResult(AppCompatActivity.RESULT_OK)
                    requireActivity().finish()
                }
                is AuthResult.Error -> {
                    if (result.message == ERROR_EMAIL_NOT_VERIFIED) {
                        showResendVerificationDialog()
                    } else {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.verificationResult.observe(viewLifecycleOwner) { wrapper ->
            when (wrapper) {
                is AuthResultWrapper.Success -> Toast.makeText(context, wrapper.data, Toast.LENGTH_LONG).show()
                is AuthResultWrapper.Error -> Toast.makeText(context, wrapper.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showResendVerificationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Email chưa xác thực")
            .setMessage("Tài khoản của bạn chưa được xác thực email. Vui lòng kiểm tra hộp thư hoặc thư rác.")
            .setPositiveButton("Đóng") { dialog, _ ->
                dialog.dismiss()
                // User có thể thử đăng nhập lại
            }
            .setNeutralButton("Gửi lại Email") { _, _ ->
                viewModel.resendVerificationEmail()
            }
            .show()
    }
}
