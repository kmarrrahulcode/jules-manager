package com.example.julesmanager

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.julesmanager.api.JulesClient
import com.example.julesmanager.api.Session
import com.example.julesmanager.api.Source
import com.example.julesmanager.databinding.ActivityDashboardBinding
import com.example.julesmanager.databinding.FragmentListBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Jules Dashboard"

        binding.viewPager.adapter = DashboardPagerAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) "Recent Sessions" else "Codebases"
        }.attach()

        // Ensure API key is loaded if process was killed
        if (JulesClient.api == null) {
            val prefs = getSharedPreferences("jules_prefs", MODE_PRIVATE)
            val savedKey = prefs.getString("api_key", null)
            if (savedKey.isNullOrEmpty()) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                JulesClient.init(savedKey)
            }
        }
    }

    class DashboardPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2
        override fun createFragment(position: Int): Fragment {
            return if (position == 0) RecentSessionsFragment() else CodebasesFragment()
        }
    }
}

// --- Fragments ---

class RecentSessionsFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val adapter = SessionCardAdapter { session ->
        val intent = Intent(requireContext(), ChatActivity::class.java)
        intent.putExtra("SESSION_NAME", session.name)
        intent.putExtra("SESSION_TITLE", session.title)
        intent.putExtra("SESSION_STATE", session.state)
        startActivity(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadSessions() }
        loadSessions()
    }

    private fun loadSessions() {
        binding.swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = JulesClient.api?.listSessions()
                val sessions = response?.sessions ?: emptyList()
                withContext(Dispatchers.Main) {
                    adapter.submitList(sessions)
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CodebasesFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private val adapter = CodebaseAdapter { source ->
        val intent = Intent(requireContext(), CodebaseDetailActivity::class.java)
        intent.putExtra("SOURCE_NAME", source.name)
        intent.putExtra("REPO_OWNER", source.githubRepo?.owner)
        intent.putExtra("REPO_NAME", source.githubRepo?.repo)
        startActivity(intent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadSources() }
        loadSources()
    }

    private fun loadSources() {
        binding.swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = JulesClient.api?.listSources()
                val sources = response?.sources ?: emptyList()
                withContext(Dispatchers.Main) {
                    adapter.submitList(sources)
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// --- Adapters ---

class SessionCardAdapter(private val onClick: (Session) -> Unit) : RecyclerView.Adapter<SessionCardAdapter.ViewHolder>() {
    private var list: List<Session> = emptyList()

    fun submitList(newList: List<Session>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session_card, parent, false)
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
        private val repo: TextView = view.findViewById(R.id.tvSessionRepo)
        private val icon: TextView = view.findViewById(R.id.tvStateIcon)

        fun bind(session: Session) {
            title.text = session.title ?: "Untitled"
            repo.text = session.sourceContext?.source?.substringAfterLast("/") ?: "Unknown Source"

            icon.text = when(session.state) {
                "COMPLETED" -> "✅"
                "FAILED" -> "❌"
                "AWAITING_USER_FEEDBACK" -> "🟠"
                "AWAITING_PLAN_APPROVAL" -> "📋"
                else -> "⏳"
            }
        }
    }
}

class CodebaseAdapter(private val onClick: (Source) -> Unit) : RecyclerView.Adapter<CodebaseAdapter.ViewHolder>() {
    private var list: List<Source> = emptyList()

    fun submitList(newList: List<Source>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_codebase_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val source = list[position]
        holder.bind(source)
        holder.itemView.setOnClickListener { onClick(source) }
    }

    override fun getItemCount() = list.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val repoName: TextView = view.findViewById(R.id.tvRepoName)
        private val branch: TextView = view.findViewById(R.id.tvBranch)

        fun bind(source: Source) {
            if (source.githubRepo != null) {
                repoName.text = "${source.githubRepo.owner}/${source.githubRepo.repo}"
                branch.text = "Branch: ${source.githubRepo.defaultBranch?.displayName ?: "default"}"
            } else {
                repoName.text = source.name.substringAfterLast("/")
                branch.text = "Source ID: ${source.name}"
            }
        }
    }
}
