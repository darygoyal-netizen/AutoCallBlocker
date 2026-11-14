package com.harshit.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var prefixContainer: LinearLayout
    private lateinit var historyTextView: TextView
    private val prefixList = mutableListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updateStatus()
        requestScreeningRole()
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefixList.addAll(BlockHelper.getPrefixes(this))

        val scrollView = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
        }

        // 1. Live Status Banner
        statusText = TextView(this).apply {
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setPadding(30, 30, 30, 30)
            setTextColor(Color.WHITE)
        }

        // 2. Activate Button
        val activateBtn = Button(this).apply {
            text = "1. ENABLE PERMISSIONS & 0ms BLOCKER"
            setOnClickListener {
                requestAllPermissions()
                requestScreeningRole()
            }
        }

        // 3. Default App Settings Shortcut (अगर पॉपअप काम न करे)
        val settingsBtn = Button(this).apply {
            text = "Open Default Caller ID Settings"
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Settings -> Apps -> Default Apps में जाएं", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 4. Add New Prefix Section
        val input = EditText(this).apply {
            hint = "नया नंबर या सीरीज डालें (जैसे +91124)"
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
        }

        val addBtn = Button(this).apply {
            text = "+ ADD TO PERMANENT BLOCKLIST"
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty() && !prefixList.contains(text)) {
                    prefixList.add(text)
                    savePrefixes()
                    renderPrefixes()
                    input.text.clear()
                    Toast.makeText(this@MainActivity, "$text ब्लॉकलिस्ट में जुड़ गया!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 5. Active Block Series Container
        val prefixTitle = TextView(this).apply {
            text = "\nActive Block Series (हटाने के लिए नंबर पर टैप करें):"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        prefixContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 10, 0, 20)
        }

        // 6. Blocked Calls History Section
        val historyTitle = TextView(this).apply {
            text = "\nBlocked Calls History (Permanently Blocked):"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val clearHistoryBtn = Button(this).apply {
            text = "Clear History"
            setOnClickListener {
                BlockHelper.clearHistory(this@MainActivity)
                renderHistory()
            }
        }

        historyTextView = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#F2F2F2"))
        }

        mainLayout.addView(statusText)
        mainLayout.addView(activateBtn)
        mainLayout.addView(settingsBtn)
        mainLayout.addView(input)
        mainLayout.addView(addBtn)
        mainLayout.addView(prefixTitle)
        mainLayout.addView(prefixContainer)
        mainLayout.addView(historyTitle)
        mainLayout.addView(clearHistoryBtn)
        mainLayout.addView(historyTextView)

        scrollView.addView(mainLayout)
        setContentView(scrollView)

        renderPrefixes()
        renderHistory()
        updateStatus()
        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        renderHistory()
    }

    private fun updateStatus() {
        val hasPhonePerm = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ANSWER_PHONE_CALLS
        ) == PackageManager.PERMISSION_GRANTED

        var hasScreeningRole = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            hasScreeningRole = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        }

        when {
            hasScreeningRole && hasPhonePerm -> {
                statusText.text = "✅ STATUS: 0ms SILENT BLOCKER ACTIVE"
                statusText.setBackgroundColor(Color.parseColor("#2E7D32"))
            }
            hasPhonePerm -> {
                statusText.text = "✅ STATUS: AUTO CALL CUT ACTIVE\n(0ms के लिए Default Caller ID सेट करें)"
                statusText.setBackgroundColor(Color.parseColor("#E65100"))
            }
            else -> {
                statusText.text = "❌ STATUS: INACTIVE (नीचे बटन दबाकर परमिशन दें)"
                statusText.setBackgroundColor(Color.parseColor("#C62828"))
            }
        }
    }

    private fun renderPrefixes() {
        prefixContainer.removeAllViews()
        for (prefix in prefixList) {
            val item = TextView(this).apply {
                text = "🚫  $prefix   [हटाने के लिए टैप करें]"
                textSize = 15f
                setTextColor(Color.BLACK)
                setPadding(25, 25, 25, 25)
                setBackgroundColor(Color.parseColor("#FFEBEE"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 8)
                layoutParams = params

                setOnClickListener {
                    prefixList.remove(prefix)
                    savePrefixes()
                    renderPrefixes()
                    Toast.makeText(this@MainActivity, "$prefix हटाया गया", Toast.LENGTH_SHORT).show()
                }
            }
            prefixContainer.addView(item)
        }
    }

    private fun renderHistory() {
        val list = BlockHelper.getHistoryList(this)
        historyTextView.text = list.joinToString("\n\n")
    }

    private fun requestAllPermissions() {
        val perms = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }
    }

    private fun savePrefixes() {
        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("prefixes", prefixList.toSet()).apply()
    }
}
