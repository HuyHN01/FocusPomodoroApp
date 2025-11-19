package com.example.focusmate.ui.auth

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.commit
import com.example.focusmate.R
import com.example.focusmate.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header_view)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val statusBarHeight = bars.top

            val headerView = findViewById<View>(R.id.header_view)
            val originalHeaderHeight = 150

            headerView.updateLayoutParams {
                height = 170.dpToPx() + statusBarHeight
            }

            val logoImage = findViewById<View>(R.id.logo_image)
            val originalMarginTop = 25.dpToPx()

            logoImage.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = originalMarginTop + statusBarHeight
            }

            insets
        }



        
        if (savedInstanceState == null) {
            navigateToSignIn() 
        }

        binding.signInToggle.setOnClickListener {
            navigateToSignIn()
        }

        binding.signUpToggle.setOnClickListener {
            navigateToSignUp()
        }
    }

    fun navigateToSignIn() {
        setActiveTab(true) 
        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            replace(R.id.auth_fragment_container, SignInFragment())
        }
    }

    fun navigateToSignUp() {
        setActiveTab(false) 
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
            binding.signUpToggle.setTextColor(getColor(R.color.dark_gray))
        } else {
            binding.signUpToggle.setBackgroundResource(R.drawable.bg_toggle_selected)
            binding.signUpToggle.setTextColor(getColor(android.R.color.white))
            binding.signInToggle.setBackgroundResource(android.R.color.transparent)
            binding.signInToggle.setTextColor(getColor(R.color.dark_gray))
        }
    }

    
    fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
