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

        val initialState = intent.getStringExtra("SESSION_STATE")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = title
        supportActionBar?.subtitle = sessionName.substringAfterLast("/")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Header Info Expansion
        binding.toolbar.setOnClickListener {
            binding.headerInfo.visibility = if(binding.headerInfo.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        binding.tvFullSessionId.text = sessionName
        binding.tvFullState.text = "State: $initialState"

        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { refreshSession() }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                binding.etMessage.text.clear()
            }
        }

        binding.btnOptions.setOnClickListener {
            showOptionsDialog()
        }

        binding.btnApprovePlan.setOnClickListener {
            approvePlan()
        }

        refreshSession()
    }

    private fun refreshSession() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                // Fetch latest state to update UI
                val session = JulesClient.api?.getSession(sessionName)
                if (session != null) {
                    binding.tvFullState.text = "State: ${session.state}"
                    updateSmartActions(session.state)
                }
                loadActivities()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Error refreshing: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun updateSmartActions(state: String?) {
        if (state == "AWAITING_PLAN_APPROVAL") {
            binding.actionsBar.visibility = View.VISIBLE
        } else {
            binding.actionsBar.visibility = View.GONE
        }
    }

    private suspend fun loadActivities() {
        try {
            val response = JulesClient.api?.listActivities(sessionName)
            val activities = response?.activities ?: emptyList()
            // Filter only messages for this simple view
            val messages = activities.filter { it.userMessaged != null || it.agentMessaged != null }
            adapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.rvChat.scrollToPosition(messages.size - 1)
            }
        } finally {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun showOptionsDialog() {
         val options = arrayOf("Publish Feature Branch", "Refresh")
         androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Options")
            .setItems(options) { _, which ->
                when(which) {
                    0 -> publishBranch()
                    1 -> refreshSession()
                }
            }
            .show()
    }

    private fun publishBranch() {
        branchCounter++
        val branchName = "feature/change$branchCounter"
        val msg = "Please publish the changes to a new branch named $branchName and create a PR."
        sendMessage(msg)
        Toast.makeText(this, "Requested publish to $branchName", Toast.LENGTH_SHORT).show()
    }

    private fun approvePlan() {
        lifecycleScope.launch {
            try {
                JulesClient.api?.approvePlan(sessionName)
                Toast.makeText(this@ChatActivity, "Plan Approved!", Toast.LENGTH_SHORT).show()
                refreshSession()
            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Failed to approve: ${e.message}", Toast.LENGTH_SHORT).show()
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

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageHolder>() {

    private var list: List<ActivityItem> = emptyList()
    // Map to store expansion state: position -> isExpanded
    private val expansionState = mutableMapOf<Int, Boolean>()

    private val TYPE_USER = 1
    private val TYPE_AGENT = 2
    private val MAX_CHARS = 500

    fun submitList(newList: List<ActivityItem>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].userMessaged != null) TYPE_USER else TYPE_AGENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val layoutId = if (viewType == TYPE_USER) R.layout.item_message_user else R.layout.item_message_agent
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageHolder(view)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        val item = list[position]
        val rawText = if (getItemViewType(position) == TYPE_USER) {
            item.userMessaged?.userMessage ?: ""
        } else {
            item.agentMessaged?.agentMessage ?: ""
        }

        val isExpanded = expansionState[position] == true

        if (rawText.length > MAX_CHARS) {
            holder.expandText.visibility = View.VISIBLE
            if (isExpanded) {
                holder.text.text = rawText
                holder.expandText.text = "Show Less"
            } else {
                holder.text.text = rawText.take(MAX_CHARS) + "..."
                holder.expandText.text = "Show More"
            }

            holder.expandText.setOnClickListener {
                expansionState[position] = !isExpanded
                notifyItemChanged(position)
            }
        } else {
            holder.text.text = rawText
            holder.expandText.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size

    class MessageHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.tvMessage)
        val expandText: TextView = view.findViewById(R.id.tvExpand)
    }
}
