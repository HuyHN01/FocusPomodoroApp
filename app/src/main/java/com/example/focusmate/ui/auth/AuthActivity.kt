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
            supportFragmentManager.commit {
                replace(R.id.auth_fragment_container, SignInFragment())
            }
        }

        binding.signInToggle.setOnClickListener {
            setActiveTab(true)
            supportFragmentManager.commit {
                setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                replace(R.id.auth_fragment_container, SignInFragment())
            }
        }

        binding.signUpToggle.setOnClickListener {
            setActiveTab(false)
            supportFragmentManager.commit {
                setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                replace(binding.authFragmentContainer.id, SignUpFragment())
            }
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
