package com.android.capstone.sereluna.ui.article

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.api.ArticleApiService
import com.android.capstone.sereluna.data.model.ArticleResponse
import com.android.capstone.sereluna.data.model.GuardianArticle
import com.android.capstone.sereluna.databinding.FragmentArticleBinding
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ArticleFragment : Fragment() {

    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!
    private val apiKey = "5600de1f-7117-4da8-87d9-6153fb72e905" // API Key dari The Guardian
    private val articles = mutableListOf<GuardianArticle>()
    private lateinit var adapter: ArticleAdapter
    private var currentPage = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentArticleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadArticles(currentPage) // Mengambil artikel dari API

        // Fungsi tombol kembali
        binding.ivBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = ArticleAdapter(articles) { article ->
            showArticleDetails(article) // Fungsi yang akan dijalankan ketika "Show more" diklik
        }
        binding.recyclerViewArticles.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewArticles.adapter = adapter

        binding.recyclerViewArticles.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    currentPage++
                    loadArticles(currentPage)
                }
            }
        })
    }

    private fun loadArticles(page: Int) {
        // Logging interceptor untuk debugging
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://content.guardianapis.com/") // Base URL untuk The Guardian API
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ArticleApiService::class.java)
        val call = service.getArticles(apiKey, "mental health")

        call.enqueue(object : Callback<ArticleResponse> {
            override fun onResponse(call: Call<ArticleResponse>, response: Response<ArticleResponse>) {
                if (response.isSuccessful) {
                    val newArticles = response.body()?.response?.results ?: emptyList()
                    if (newArticles.isNotEmpty()) {
                        articles.addAll(newArticles)
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(requireContext(), "No more articles found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load articles: ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e("ArticleFragment", "Error response code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ArticleResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "Failed to connect to API: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("ArticleFragment", "API call failed", t)
            }
        })
    }

    private fun showArticleDetails(article: GuardianArticle) {
        // Gunakan `trailText` jika tersedia atau `bodyText` jika tidak
        var displayContent = article.fields?.trailText ?: "No Content"
        if (displayContent.isEmpty()) {
            displayContent = article.fields?.bodyText ?: "For full content, visit: ${article.webUrl}"
        }

        // Navigasikan ke ArticleDetailActivity untuk menampilkan detail artikel
        val detailIntent = Intent(requireContext(), ArticleDetailActivity::class.java).apply {
            putExtra("title", article.webTitle)
            putExtra("date", article.webPublicationDate)
            putExtra("content", displayContent) // Kirim konten penuh
            putExtra("imageUrl", article.fields?.thumbnail ?: "") // Pastikan tidak null
            putExtra("articleUrl", article.webUrl) // Kirim URL artikel juga
        }
        startActivity(detailIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
