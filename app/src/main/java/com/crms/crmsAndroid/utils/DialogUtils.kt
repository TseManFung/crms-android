package com.crms.crmsAndroid.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper

object DialogUtils {
    fun showErrorDialog(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}