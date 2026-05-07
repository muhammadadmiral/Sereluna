package com.android.capstone.sereluna.ui.article

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.ArticleAdapter
import com.android.capstone.sereluna.data.model.Article
import com.android.capstone.sereluna.databinding.FragmentArticleBinding
import com.android.capstone.sereluna.ui.viewmodel.ArticleViewModel

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var articleViewModel: ArticleViewModel

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
        setupViewModel()
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter { article ->
            navigateToDetail(article)
        }
        binding.recyclerViewArticles.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = articleAdapter
        }
    }

    private fun setupViewModel() {
        articleViewModel = ViewModelProvider(this).get(ArticleViewModel::class.java)
        articleViewModel.getArticle().observe(viewLifecycleOwner) { articles ->
            if (articles != null) {
                articleAdapter.submitList(articles)
            }
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
    
    // This function seems to be part of a PagerAdapter interface which is not fully implemented.
    // It's kept here to avoid breaking other parts of the code that might reference it.
    fun onPageSelected(_position: Int) {
        // Handle page selection if needed
    }
}
