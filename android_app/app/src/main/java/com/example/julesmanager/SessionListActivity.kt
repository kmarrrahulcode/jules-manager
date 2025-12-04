package com.example.julesmanager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.julesmanager.api.JulesClient
import com.example.julesmanager.api.Session
import com.example.julesmanager.databinding.ActivitySessionListBinding
import kotlinx.coroutines.launch

class SessionListActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionListBinding
    private val adapter = SessionAdapter { session ->
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("SESSION_NAME", session.name)
        intent.putExtra("SESSION_TITLE", session.title)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Sessions"

        binding.rvSessions.layoutManager = LinearLayoutManager(this)
        binding.rvSessions.adapter = adapter

        checkAuthAndLoad()
    }

    private fun checkAuthAndLoad() {
        if (JulesClient.api == null) {
            val prefs = getSharedPreferences("jules_prefs", MODE_PRIVATE)
            val savedKey = prefs.getString("api_key", null)
            if (!savedKey.isNullOrEmpty()) {
                JulesClient.init(savedKey)
                loadSessions()
            } else {
                // Return to login if no key found (shouldn't happen if LoginActivity logic is correct, but safe fallback)
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } else {
            loadSessions()
        }
    }

    private fun loadSessions() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = JulesClient.api?.listSessions()
                val sessions = response?.sessions ?: emptyList()
                adapter.submitList(sessions)
            } catch (e: Exception) {
                Toast.makeText(this@SessionListActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}

class SessionAdapter(private val onClick: (Session) -> Unit) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {
    private var list: List<Session> = emptyList()

    fun submitList(newList: List<Session>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = list[position]
        holder.bind(session)
        holder.itemView.setOnClickListener { onClick(session) }
    }

    override fun getItemCount() = list.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.tvSessionTitle)
        private val state: TextView = view.findViewById(R.id.tvSessionState)
        private val id: TextView = view.findViewById(R.id.tvSessionId)

        fun bind(session: Session) {
            title.text = session.title ?: "Untitled"
            state.text = session.state ?: "UNKNOWN"
            id.text = session.name
        }
    }
}
