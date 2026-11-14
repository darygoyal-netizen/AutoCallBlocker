package com.harshit.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val prefixList = mutableListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "Auto Call Cut एक्टिवेट हो गया!", Toast.LENGTH_LONG).show()
            requestScreeningRoleOptional()
        } else {
            Toast.makeText(this, "कृपया सभी Phone Permissions को Allow करें!", Toast.LENGTH_LONG).show()
        }
    }

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        val defaultSet = setOf("+91124", "91124", "0124", "+91140", "124")
        val saved = prefs.getStringSet("prefixes", defaultSet) ?: defaultSet
        prefixList.addAll(saved)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val roleBtn = Button(this).apply {
            text = "1. ACTIVATE AUTO CALL CUT (ALLOW PERMISSIONS)"
            setOnClickListener { requestAllPermissions() }
        }

        val input = EditText(this).apply {
            hint = "नंबर की शुरुआती सीरीज डालें (जैसे +91124 या 124)"
        }

        val addBtn = Button(this).apply {
            text = "ADD PREFIX TO BLOCKLIST"
            setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty() && !prefixList.contains(text)) {
                    prefixList.add(text)
                    savePrefixes()
                    adapter.notifyDataSetChanged()
                    input.text.clear()
                }
            }
        }

        val infoText = TextView(this).apply {
            text = "Blocked Prefixes (हटाने के लिए नंबर पर टैप करें):"
            setPadding(0, 30, 0, 10)
        }

        listView = ListView(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, prefixList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val removed = prefixList.removeAt(position)
            savePrefixes()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "$removed हटाया गया", Toast.LENGTH_SHORT).show()
        }

        layout.addView(roleBtn)
        layout.addView(input)
        layout.addView(addBtn)
        layout.addView(infoText)
        layout.addView(listView)

        setContentView(layout)
        
        // ऐप खुलते ही परमिशन चेक करें
        requestAllPermissions()
    }

    private fun requestAllPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE
        )

        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            Toast.makeText(this, "Auto Blocker पूरी तरह तैयार और चालू है!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestScreeningRoleOptional() {
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
