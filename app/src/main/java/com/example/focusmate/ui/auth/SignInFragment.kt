package com.example.focusmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focusmate.R
import com.example.focusmate.ui.pomodoro.PomodoroActivity
import com.google.android.material.textfield.TextInputEditText

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
                is AuthResult.Loading -> Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
                is AuthResult.Success -> {
                    //Toast.makeText(context, "Welcome ${result.user.email}", Toast.LENGTH_SHORT).show()
                    requireActivity().setResult(AppCompatActivity.RESULT_OK)
                    requireActivity().finish()
                }
                is AuthResult.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
