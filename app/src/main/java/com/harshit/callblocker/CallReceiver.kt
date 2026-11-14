package com.harshit.callblocker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
            val cleanNumber = incomingNumber.replace("[^0-9+]".toRegex(), "")

            val prefs = context.getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
            val defaultPrefixes = setOf("+91124", "91124", "0124", "+91140", "91140")
            val blockedPrefixes = prefs.getStringSet("prefixes", defaultPrefixes) ?: defaultPrefixes

            val shouldBlock = blockedPrefixes.any { prefix ->
                val cleanPrefix = prefix.replace("[^0-9+]".toRegex(), "")
                cleanNumber.startsWith(cleanPrefix) ||
                cleanNumber.startsWith(cleanPrefix.removePrefix("+")) ||
                cleanNumber.removePrefix("+91").removePrefix("91").removePrefix("0")
                    .startsWith(cleanPrefix.removePrefix("+91").removePrefix("91").removePrefix("0"))
            }

            if (shouldBlock) {
                cutCallInstantly(context, cleanNumber)
            }
        }
    }

    private fun cutCallInstantly(context: Context, number: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                telecomManager.endCall()
                Toast.makeText(context, "Blocked Spam Call: $number", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
