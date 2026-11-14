package com.harshit.callblocker

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService

class SpamBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")

        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        val defaultPrefixes = setOf("+91124", "91124", "0124", "+91140", "91140")
        val blockedPrefixes = prefs.getStringSet("prefixes", defaultPrefixes) ?: defaultPrefixes

        // चेक करें कि नंबर किसी ब्लॉक किए गए प्रीफिक्स से शुरू होता है या नहीं
        val shouldBlock = blockedPrefixes.any { prefix ->
            cleanNumber.startsWith(prefix) || 
            (prefix.startsWith("+") && cleanNumber.startsWith(prefix.substring(1)))
        }

        val response = CallResponse.Builder()
        if (shouldBlock) {
            response.setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
        } else {
            response.setDisallowCall(false)
        }

        respondToCall(callDetails, response.build())
    }
}
