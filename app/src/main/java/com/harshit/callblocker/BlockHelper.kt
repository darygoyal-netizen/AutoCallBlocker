package com.harshit.callblocker

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object BlockHelper {

    private const val PREF_NAME = "BlockerPrefs"
    private const val KEY_PREFIXES = "prefixes"
    private const val KEY_PERMANENT_NUMBERS = "permanent_numbers"
    private const val KEY_HISTORY = "blocked_history"

    fun getPrefixes(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val defaultSet = setOf("+91124", "91124", "0124", "+91140", "91140", "124", "140")
        return prefs.getStringSet(KEY_PREFIXES, defaultSet) ?: defaultSet
    }

    fun getPermanentBlockedNumbers(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PERMANENT_NUMBERS, emptySet()) ?: emptySet()
    }

    fun shouldBlockNumber(context: Context, rawNumber: String): Boolean {
        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanNumber.isEmpty()) return false

        // 1. पहले चेक करें कि क्या यह नंबर परमानेंट ब्लॉक लिस्ट में है
        val permanentList = getPermanentBlockedNumbers(context)
        if (permanentList.contains(cleanNumber)) return true

        // 2. फिर चेक करें कि क्या यह ब्लॉक की गई सीरीज से शुरू होता है
        val coreNumber = cleanNumber.removePrefix("+91").removePrefix("91").removePrefix("0")
        val prefixes = getPrefixes(context)

        return prefixes.any { prefix ->
            val cleanPrefix = prefix.replace("[^0-9+]".toRegex(), "")
            val corePrefix = cleanPrefix.removePrefix("+91").removePrefix("91").removePrefix("0")
            cleanNumber.startsWith(cleanPrefix) || 
            (corePrefix.isNotEmpty() && coreNumber.startsWith(corePrefix))
        }
    }

    fun recordAndPermanentlyBlock(context: Context, rawNumber: String) {
        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")
        if (cleanNumber.isEmpty()) return

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // उस नंबर को परमानेंट ब्लॉक लिस्ट में डाल दें
        val permanentSet = getPermanentBlockedNumbers(context).toMutableSet()
        permanentSet.add(cleanNumber)

        // समय और तारीख के साथ हिस्ट्री सेव करें
        val time = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()).format(Date())
        val entry = "$cleanNumber  •  $time (Permanently Blocked)"

        val oldHistory = prefs.getString(KEY_HISTORY, "") ?: ""
        // डुप्लीकेट एंट्री (अगर एक ही सेकंड में दोनों सर्विस से ट्रिगर हो) से बचें
        val latestLine = oldHistory.lines().firstOrNull() ?: ""
        if (!latestLine.startsWith("$cleanNumber  •  ${time.substring(0, 12)}")) {
            val newHistory = if (oldHistory.isEmpty()) entry else "$entry\n$oldHistory"
            prefs.edit()
                .putStringSet(KEY_PERMANENT_NUMBERS, permanentSet)
                .putString(KEY_HISTORY, newHistory)
                .apply()
        }
    }

    fun getHistoryList(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (raw.isEmpty()) listOf("अभी तक कोई स्पैम कॉल ब्लॉक नहीं हुआ है।") else raw.lines()
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
