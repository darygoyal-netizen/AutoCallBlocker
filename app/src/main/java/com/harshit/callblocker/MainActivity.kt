package com.harshit.callblocker

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private val prefixList = mutableListOf<String>()

    private val roleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Blocker Activated!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("prefixes", setOf("+91124", "91124", "0124", "+91140")) ?: emptySet()
        prefixList.addAll(saved)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val roleBtn = Button(this).apply {
            text = "1. Enable Auto Call Blocker Permission"
            setOnClickListener { requestScreeningRole() }
        }

        val input = EditText(this).apply {
            hint = "नंबर की शुरुआती सीरीज डालें (जैसे +91124)"
        }

        val addBtn = Button(this).apply {
            text = "Add Prefix to Blocklist"
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
    }

    private fun savePrefixes() {
        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("prefixes", prefixList.toSet()).apply()
    }

    private fun requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleLauncher.launch(intent)
                } else {
                    Toast.makeText(this, "पहले से ही एक्टिव है!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
