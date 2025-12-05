package com.example.julesmanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.julesmanager.api.JulesClient
import com.example.julesmanager.databinding.ActivityMainBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for existing key
        val prefs = getSharedPreferences("jules_prefs", MODE_PRIVATE)
        val savedKey = prefs.getString("api_key", null)
        if (!savedKey.isNullOrEmpty()) {
            JulesClient.init(savedKey)
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        binding.btnLogin.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                JulesClient.init(key)
                prefs.edit().putString("api_key", key).apply()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Please enter API Key", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
