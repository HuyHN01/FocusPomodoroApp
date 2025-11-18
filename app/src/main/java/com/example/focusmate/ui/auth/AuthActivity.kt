package com.example.focusmate.ui.auth

import android.os.Bundle
//import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.focusmate.R
import com.example.focusmate.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // load mac dinh Sign In
        if (savedInstanceState == null) {
            navigateToSignIn() // Sử dụng hàm vừa tách ra
        }

        binding.signInToggle.setOnClickListener {
            navigateToSignIn()
        }

        binding.signUpToggle.setOnClickListener {
            navigateToSignUp()
        }
    }

    fun navigateToSignIn() {
        setActiveTab(true) // Cập nhật UI của Toggle Button
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            replace(R.id.auth_fragment_container, SignInFragment())
        }
    }

    fun navigateToSignUp() {
        setActiveTab(false) // Cập nhật UI của Toggle Button
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            replace(R.id.auth_fragment_container, SignUpFragment())
        }
    }
    private fun setActiveTab(isSignIn: Boolean) {
        if (isSignIn) {
            binding.signInToggle.setBackgroundResource(R.drawable.bg_toggle_selected)
            binding.signInToggle.setTextColor(getColor(android.R.color.white))
            binding.signUpToggle.setBackgroundResource(android.R.color.transparent)
            binding.signUpToggle.setTextColor(getColor(R.color.red))
        } else {
            binding.signUpToggle.setBackgroundResource(R.drawable.bg_toggle_selected)
            binding.signUpToggle.setTextColor(getColor(android.R.color.white))
            binding.signInToggle.setBackgroundResource(android.R.color.transparent)
            binding.signInToggle.setTextColor(getColor(R.color.red))
        }
    }
}
