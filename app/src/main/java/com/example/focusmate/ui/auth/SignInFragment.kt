package com.example.focusmate.ui.auth

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.focusmate.R
import com.example.focusmate.data.repository.AuthResultWrapper
import com.example.focusmate.data.repository.ERROR_EMAIL_NOT_VERIFIED
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText

// Annotation này giúp ẩn các cảnh báo "Deprecated" vì chúng ta vẫn dùng cách cũ của Firebase
@Suppress("DEPRECATION")
class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private val viewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    // Launcher nhận kết quả từ màn hình đăng nhập Google
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Lấy tài khoản Google thành công
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    // Có Token -> Gửi sang ViewModel
                    viewModel.signInWithGoogle(idToken)
                } else {
                    Toast.makeText(context, "Không lấy được Google Token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // e.statusCode = 12500 thường là do sai SHA-1
                // e.statusCode = 10 thường là do sai cấu hình Console
                Toast.makeText(context, "Google Sign-In failed code: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ánh xạ View
        val emailInput = view.findViewById<TextInputEditText>(R.id.email_or_username_input)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.password_input)
        val signInButton = view.findViewById<AppCompatButton>(R.id.sign_in_button)
        val forgotPasswordText = view.findViewById<View>(R.id.forgot_password_text)

        // LƯU Ý: Em cần đảm bảo trong file XML fragment_sign_in.xml có View ID này
        // Nếu em dùng ImageView cho nút Google, hãy chắc chắn ID đúng
        // Ví dụ ở đây thầy dùng ID là ic_google (hoặc id em đặt trong layout)
        val googleButton = view.findViewById<View>(R.id.ic_google)

        // 2. Cấu hình Google Sign In (Khắc phục warning 'googleSignInClient never used')
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Tự động sinh ra từ google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        // 3. Xử lý sự kiện Click nút Đăng nhập thường
        signInButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signIn(email, password)
            } else {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Xử lý sự kiện Click nút Google (Khắc phục warning 'launcher never used')
        googleButton?.setOnClickListener {
            signInGoogle()
        }

        forgotPasswordText.setOnClickListener {
            showForgotPasswordDialog()
        }

        // 5. Quan sát dữ liệu từ ViewModel
        observeViewModel()
    }

    private fun signInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun observeViewModel() {
        // Quan sát kết quả Đăng nhập (Cả thường và Google đều trả về đây)
        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResult.Loading -> {
                    // Em có thể hiện ProgressBar ở đây nếu muốn
                    Toast.makeText(context, "Đang xử lý...", Toast.LENGTH_SHORT).show()
                }
                is AuthResult.Success -> {
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    requireActivity().setResult(AppCompatActivity.RESULT_OK)
                    requireActivity().finish()
                }
                is AuthResult.Error -> {
                    if (result.message == ERROR_EMAIL_NOT_VERIFIED) {
                        showResendVerificationDialog()
                    } else {
                        Toast.makeText(context, "Lỗi: ${result.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Quan sát kết quả gửi lại email
        viewModel.verificationResult.observe(viewLifecycleOwner) { wrapper ->
            when (wrapper) {
                is AuthResultWrapper.Success -> Toast.makeText(context, wrapper.data, Toast.LENGTH_LONG).show()
                is AuthResultWrapper.Error -> Toast.makeText(context, wrapper.message, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.resetPasswordResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is AuthResultWrapper.Success -> {
                    Toast.makeText(context, result.data, Toast.LENGTH_LONG).show()
                }
                is AuthResultWrapper.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showResendVerificationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Email chưa xác thực")
            .setMessage("Tài khoản của bạn chưa được xác thực email. Vui lòng kiểm tra hộp thư hoặc thư rác.")
            .setPositiveButton("Đóng") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Gửi lại Email") { _, _ ->
                viewModel.resendVerificationEmail()
            }
            .show()
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Quên mật khẩu")
        builder.setMessage("Nhập email của bạn để nhận liên kết đặt lại mật khẩu:")

        // Tạo một EditText bằng code (đỡ phải tạo file layout xml mới)
        val input = EditText(context)
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "example@email.com"

        // Thêm padding cho đẹp
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50
        params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        // Nút Gửi
        builder.setPositiveButton("Gửi") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                viewModel.resetPassword(email)
            } else {
                Toast.makeText(context, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
            }
        }

        // Nút Hủy
        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

}