package com.example.julesmanager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.julesmanager.api.CreateSessionRequest
import com.example.julesmanager.api.JulesClient
import com.example.julesmanager.api.SourceContext
import com.example.julesmanager.databinding.ActivityCodebaseDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodebaseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCodebaseDetailBinding
    private lateinit var sourceName: String
    private val adapter = SessionCardAdapter { session ->
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("SESSION_NAME", session.name)
        intent.putExtra("SESSION_TITLE", session.title)
        intent.putExtra("SESSION_STATE", session.state)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCodebaseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sourceName = intent.getStringExtra("SOURCE_NAME") ?: ""
        val repoName = intent.getStringExtra("REPO_NAME") ?: "Codebase"
        val repoOwner = intent.getStringExtra("REPO_OWNER") ?: ""

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = if (repoOwner.isNotEmpty()) "$repoOwner/$repoName" else repoName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { loadSessions() }

        binding.fabCreateSession.setOnClickListener {
            showCreateSessionDialog()
        }

        loadSessions()
    }

    private fun loadSessions() {
        binding.swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = JulesClient.api?.listSessions()
                val allSessions = response?.sessions ?: emptyList()
                // Filter sessions for this source
                // The API session object's sourceContext.source usually matches the sourceName
                val filtered = allSessions.filter {
                    it.sourceContext?.source == sourceName
                }

                withContext(Dispatchers.Main) {
                    adapter.submitList(filtered)
                    binding.swipeRefresh.isRefreshing = false
                    if (filtered.isEmpty()) {
                        Toast.makeText(this@CodebaseDetailActivity, "No sessions found for this codebase", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CodebaseDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    private fun showCreateSessionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_session, null)
        val etPrompt = dialogView.findViewById<EditText>(R.id.etPrompt)
        val etBranch = dialogView.findViewById<EditText>(R.id.etBranch)

        AlertDialog.Builder(this)
            .setTitle("New Session")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val prompt = etPrompt.text.toString().trim()
                val branch = etBranch.text.toString().trim()
                if (prompt.isNotEmpty()) {
                    createSession(prompt, branch)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createSession(prompt: String, branch: String) {
        binding.swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Note: 'branch' field is currently removed from API model as it causes 400 errors.
                // We ignore the branch input for now until the API supports it.
                val request = CreateSessionRequest(
                    prompt = prompt,
                    sourceContext = SourceContext(source = sourceName)
                )
                val newSession = JulesClient.api?.createSession(request)

                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    if (newSession != null) {
                        Toast.makeText(this@CodebaseDetailActivity, "Session Created!", Toast.LENGTH_SHORT).show()
                        loadSessions() // Refresh list
                        // Optionally open the new session immediately
                         val intent = Intent(this@CodebaseDetailActivity, ChatActivity::class.java)
                         intent.putExtra("SESSION_NAME", newSession.name)
                         intent.putExtra("SESSION_TITLE", newSession.title)
                         intent.putExtra("SESSION_STATE", newSession.state)
                         startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@CodebaseDetailActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
