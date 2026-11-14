package com.harshit.callblocker

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpamBlockerService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) return

        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")

        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        val defaultPrefixes = setOf("+91124", "91124", "0124", "+91140", "91140", "124", "140")
        val blockedPrefixes = prefs.getStringSet("prefixes", defaultPrefixes)?.toMutableSet() ?: defaultPrefixes.toMutableSet()
        val exactBlockedNumbers = prefs.getStringSet("exact_numbers", emptySet())?.toMutableSet() ?: mutableSetOf()

        val coreNumber = cleanNumber.removePrefix("+91").removePrefix("91").removePrefix("0")

        val isBlockedByExact = exactBlockedNumbers.contains(cleanNumber) || exactBlockedNumbers.contains(coreNumber)
        val isBlockedByPrefix = blockedPrefixes.any { prefix ->
            val cleanPrefix = prefix.replace("[^0-9+]".toRegex(), "")
            val corePrefix = cleanPrefix.removePrefix("+91").removePrefix("91").removePrefix("0")
            cleanNumber.startsWith(cleanPrefix) || (corePrefix.isNotEmpty() && coreNumber.startsWith(corePrefix))
        }

        val response = CallResponse.Builder()
        if (isBlockedByExact || isBlockedByPrefix) {
            // 1. इस नंबर को हमेशा के लिए Exact Blocklist में ऑटो-सेव करें
            exactBlockedNumbers.add(cleanNumber)
            prefs.edit().putStringSet("exact_numbers", exactBlockedNumbers).apply()

            // 2. ऐप की हिस्ट्री में तारीख और समय के साथ सेव करें
            saveBlockedHistory(this, cleanNumber)

            // 3. रिंग बजने से पहले ही कॉल कट और साइलेंट करें
            response.setDisallowCall(true)
                .setRejectCall(true)
                .setSilenceCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
        } else {
            response.setDisallowCall(false)
        }

        respondToCall(callDetails, response.build())
    }

    companion object {
        fun saveBlockedHistory(context: Context, number: String) {
            val prefs = context.getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
            val time = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date())
            val newEntry = "🚫 $number  —$time"
            val oldHistory = prefs.getString("history_log", "") ?: ""
            
            // अगर ठीक इसी सेकंड यह नंबर पहले से लॉग हो गया है तो डुप्लीकेट न बनाएं
            if (!oldHistory.lines().firstOrNull().orEmpty().contains(number)) {
                val updatedHistory = if (oldHistory.isEmpty()) newEntry else "$newEntry\n$oldHistory"
                // टॉप 100 ब्लॉक कॉल्स सेव रखें
                val trimmed = updatedHistory.lines().take(100).joinToString("\n")
                prefs.edit().putString("history_log", trimmed).apply()
            }
        }
        
