package com.omkarnub.kanri.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility for verifying and requesting SMS runtime permissions (RECEIVE_SMS and READ_SMS).
 * Ensures Kanri's primary transaction capture engine is fully empowered.
 */
object SmsPermissionHelper {
    const val SMS_PERMISSION_REQUEST_CODE = 4101

    fun hasSmsPermissions(context: Context): Boolean {
        val receiveGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return receiveGranted && readGranted
    }

    fun requestSmsPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ),
            SMS_PERMISSION_REQUEST_CODE
        )
    }
}
