package com.example.julesmanager

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
import com.example.julesmanager.api.ActivityItem
import com.example.julesmanager.api.JulesClient
import com.example.julesmanager.api.SendMessageRequest
import com.example.julesmanager.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var sessionName: String
    private val adapter = ChatAdapter()
    private var branchCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionName = intent.getStringExtra("SESSION_NAME") ?: return
        val title = intent.getStringExtra("SESSION_TITLE") ?: "Chat"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = title
        supportActionBar?.subtitle = sessionName.substringAfterLast("/")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        loadActivities()

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                binding.etMessage.text.clear()
            }
        }

        binding.btnPublish.setOnClickListener {
            branchCounter++
            val branchName = "feature/change$branchCounter"
            val msg = "Please publish the changes to a new branch named $branchName and create a PR."
            sendMessage(msg)
            Toast.makeText(this, "Requested publish to $branchName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadActivities() {
        lifecycleScope.launch {
            try {
                val response = JulesClient.api?.listActivities(sessionName)
                val activities = response?.activities ?: emptyList()
                // Filter only messages for this simple view
                val messages = activities.filter { it.userMessaged != null || it.agentMessaged != null }
                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error loading chat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(text: String) {
        lifecycleScope.launch {
            try {
                JulesClient.api?.sendMessage(sessionName, SendMessageRequest(text))
                loadActivities() // Refresh chat
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Send failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list: List<ActivityItem> = emptyList()
    private val TYPE_USER = 1
    private val TYPE_AGENT = 2

    fun submitList(newList: List<ActivityItem>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].userMessaged != null) TYPE_USER else TYPE_AGENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
            UserHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_agent, parent, false)
            AgentHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        if (holder is UserHolder) {
            holder.text.text = item.userMessaged?.userMessage
        } else if (holder is AgentHolder) {
            holder.text.text = item.agentMessaged?.agentMessage
        }
    }

    override fun getItemCount() = list.size

    class UserHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvMessage)
    }

    class AgentHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvMessage)
    }
}
