package com.android.capstone.sereluna.ui.article

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.ArticleAdapter
import com.android.capstone.sereluna.data.api.ArticleTopicDto
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.databinding.FragmentArticleBinding
import com.android.capstone.sereluna.ui.viewmodel.ArticleViewModel
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.google.android.material.chip.Chip

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    private lateinit var articleAdapter: ArticleAdapter
    private val articleViewModel: ArticleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupObservers()
        articleViewModel.loadInitial()
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter { article -> navigateToDetail(article) }
        binding.recyclerViewArticles.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = articleAdapter
        }
    }

    private fun setupSearch() {
        binding.searchInputLayout.setEndIconOnClickListener { submitSearch() }
        binding.etArticleSearch.setOnEditorActionListener { _, actionId, event ->
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (actionId == EditorInfo.IME_ACTION_SEARCH || isEnter) {
                submitSearch()
                true
            } else {
                false
            }
        }
    }

    private fun submitSearch() {
        val query = binding.etArticleSearch.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) return
        binding.chipGroupTopics.clearCheck()
        articleViewModel.loadRecommendations(query = query)
    }

    private fun setupObservers() {
        articleViewModel.topics.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> renderTopicChips(state.data)
                is UiState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }

        articleViewModel.meta.observe(viewLifecycleOwner) { meta ->
            binding.tvArticleMeta.text = listOf(meta.source, meta.topicLabel)
                .filter { it.isNotBlank() }
                .joinToString(" | ")
                .ifBlank { "Rekomendasi artikel wellbeing" }
            binding.tvArticleWarning.text = meta.disclaimer.ifBlank {
                "Artikel hanya untuk edukasi ringan, bukan diagnosis medis."
            }
        }

        articleViewModel.articles.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.articleProgress.visibility = View.VISIBLE
                    binding.tvArticleEmpty.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.articleProgress.visibility = View.GONE
                    articleAdapter.submitList(state.data)
                    binding.tvArticleEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerViewArticles.visibility = if (state.data.isEmpty()) View.GONE else View.VISIBLE
                }
                is UiState.Error -> {
                    binding.articleProgress.visibility = View.GONE
                    binding.tvArticleEmpty.visibility = View.VISIBLE
                    binding.tvArticleEmpty.text = state.message
                }
            }
        }
    }

    private fun renderTopicChips(topics: List<ArticleTopicDto>) {
        binding.chipGroupTopics.removeAllViews()
        topics.forEachIndexed { index, topic ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = topic.label.ifBlank { topic.key }
                tag = topic.key
                isCheckable = true
                isChecked = index == 0
                setOnClickListener {
                    binding.etArticleSearch.setText("")
                    articleViewModel.loadRecommendations(topic = tag as String)
                }
            }
            binding.chipGroupTopics.addView(chip)
        }
    }

    private fun navigateToDetail(article: Article) {
        val intent = Intent(requireActivity(), ArticleDetailActivity::class.java).apply {
            putExtra(ArticleDetailActivity.EXTRA_ARTICLE, article)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
