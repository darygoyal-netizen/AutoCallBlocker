package com.harshit.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefixAdapter: ArrayAdapter<String>
    private lateinit var historyAdapter: ArrayAdapter<String>
    private val prefixList = mutableListOf<String>()
    private val historyList = mutableListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        requestScreeningRole()
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "0ms Pre-Ring Blocker एक्टिवेट हो गया!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefixList.addAll(BlockHelper.getPrefixes(this))
        historyList.addAll(BlockHelper.getHistoryList(this))

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val roleBtn = Button(this).apply {
            text = "1. ENABLE 0ms SILENT BLOCKER (SET DEFAULT)"
            setOnClickListener {
                requestAllPermissions()
                requestScreeningRole()
            }
        }

        val input = EditText(this).apply {
            hint = "सीरीज या पूरा नंबर डालें (जैसे +91124)"
        }

        val addBtn = Button(this).apply {
            text = "ADD TO PERMANENT BLOCKLIST"
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty() && !prefixList.contains(text)) {
                    prefixList.add(text)
                    savePrefixes()
                    prefixAdapter.notifyDataSetChanged()
                    input.text.clear()
                }
            }
        }

        val prefixTitle = TextView(this).apply {
            text = "Active Block Series (हटाने के लिए टैप करें):"
            textSize = 15f
            setPadding(0, 20, 0, 8)
        }

        val prefixListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 380
            )
        }
        prefixAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, prefixList)
        prefixListView.adapter = prefixAdapter

        prefixListView.setOnItemClickListener { _, _, position, _ ->
            val removed = prefixList.removeAt(position)
            savePrefixes()
            prefixAdapter.notifyDataSetChanged()
            Toast.makeText(this, "$removed हटाया गया", Toast.LENGTH_SHORT).show()
        }

        val historyHeaderLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 8)
        }

        val historyTitle = TextView(this).apply {
            text = "Blocked Calls History (Permanently Blocked):"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val clearBtn = Button(this).apply {
            text = "Clear"
            setOnClickListener {
                BlockHelper.clearHistory(this@MainActivity)
                refreshHistory()
            }
        }
        historyHeaderLayout.addView(historyTitle)
        historyHeaderLayout.addView(clearBtn)

        val historyListView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        historyAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historyList)
        historyListView.adapter = historyAdapter

        layout.addView(roleBtn)
        layout.addView(input)
        layout.addView(addBtn)
        layout.addView(prefixTitle)
        layout.addView(prefixListView)
        layout.addView(historyHeaderLayout)
        layout.addView(historyListView)

        setContentView(layout)
        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun refreshHistory() {
        historyList.clear()
        historyList.addAll(BlockHelper.getHistoryList(this))
        historyAdapter.notifyDataSetChanged()
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
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                } else {
                    Toast.makeText(this, "0ms Silent Blocker पहले से चालू है!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun savePrefixes() {
        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("prefixes", prefixList.toSet()).apply()
    }
}
