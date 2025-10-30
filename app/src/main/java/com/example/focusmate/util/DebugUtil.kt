package com.example.focusmate.util

import android.content.Context
import android.widget.Toast
import android.os.Handler
import android.os.Looper // Import Looper

class DebugUtil {
    companion object {
        fun showDebugToast(context: Context, message: String) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}