package com.example.focusmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focusmate.R
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.google.android.material.textfield.TextInputEditText

class SignUpFragment : Fragment(R.layout.fragment_sign_up) {

    private val viewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emailInput = view.findViewById<TextInputEditText>(R.id.email_or_username_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val confirmPasswordInput = view.findViewById<TextInputEditText>(R.id.confirm_password_input)
        val signUpButton = view.findViewById<AppCompatButton>(R.id.sign_up_button)

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val confirmPassword = confirmPasswordInput.text.toString()

            if (password == confirmPassword) {
                viewModel.signUp(email, password)
            } else {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
                is AuthResult.Success -> {
                    Toast.makeText(context, "Registered ${result.user.email}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(requireContext(), PomodoroActivity::class.java))
                    requireActivity().finish()
                }
                is AuthResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

