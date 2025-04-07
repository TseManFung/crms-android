package com.crms.crmsAndroid.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.crms.crmsAndroid.R

object DialogUtils {
    fun showErrorDialog(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.error_title))
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }
}