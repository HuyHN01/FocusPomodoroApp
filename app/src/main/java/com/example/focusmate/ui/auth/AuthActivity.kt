package com.example.focusmate.ui.auth

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.focusmate.R

class AuthActivity : AppCompatActivity() {

    private lateinit var signInToggle: TextView
    private lateinit var signUpToggle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        signInToggle = findViewById(R.id.sign_in_toggle)
        signUpToggle = findViewById(R.id.sign_up_toggle)

        // load mặc định Sign In
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.auth_fragment_container, SignInFragment())
            }
        }

        signInToggle.setOnClickListener {
            setActiveTab(true)
            supportFragmentManager.commit {
                setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                replace(R.id.auth_fragment_container, SignInFragment())
            }
        }

        signUpToggle.setOnClickListener {
            setActiveTab(false)
            supportFragmentManager.commit {
                setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
                replace(R.id.auth_fragment_container, SignUpFragment())
            }
        }
    }

    private fun setActiveTab(isSignIn: Boolean) {
        if (isSignIn) {
            signInToggle.setBackgroundResource(R.drawable.bg_toggle_selected)
            signInToggle.setTextColor(resources.getColor(android.R.color.white))
            signUpToggle.setBackgroundResource(android.R.color.transparent)
            signUpToggle.setTextColor(resources.getColor(R.color.red))
        } else {
            signUpToggle.setBackgroundResource(R.drawable.bg_toggle_selected)
            signUpToggle.setTextColor(resources.getColor(android.R.color.white))
            signInToggle.setBackgroundResource(android.R.color.transparent)
            signInToggle.setTextColor(resources.getColor(R.color.red))
        }
    }
}
