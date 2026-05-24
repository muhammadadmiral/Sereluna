package com.android.capstone.sereluna.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.DoctorAdapter
import com.android.capstone.sereluna.data.api.DoctorDto
import com.android.capstone.sereluna.databinding.FragmentDoctorBinding
import com.android.capstone.sereluna.ui.viewmodel.DoctorViewModel
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DoctorFragment : Fragment() {

    private var _binding: FragmentDoctorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DoctorViewModel by viewModels()
    private lateinit var doctorAdapter: DoctorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDoctorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        viewModel.loadDoctors()
    }

    private fun setupRecyclerView() {
        doctorAdapter = DoctorAdapter(emptyList()) { doctor ->
            showWhatsAppConfirmation(doctor)
        }
        binding.rvDoctors.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = doctorAdapter
        }
    }

    private fun setupObservers() {
        viewModel.doctors.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvDoctors.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    if (state.data.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvDoctors.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvDoctors.visibility = View.VISIBLE
                        doctorAdapter.updateData(state.data)
                    }
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = state.message
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showWhatsAppConfirmation(doctor: DoctorDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Konsultasi Psikolog")
            .setMessage("Kamu akan diarahkan ke WhatsApp untuk konsultasi dengan ${doctor.name}.")
            .setPositiveButton("Lanjutkan") { _, _ ->
                openWhatsApp(requireContext(), doctor.whatsappNumber, doctor.name)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openWhatsApp(context: Context, number: String, doctorName: String) {
        val cleanNumber = number.filter { it.isDigit() }
        val message = Uri.encode(
            "Halo, saya ingin konsultasi melalui Sereluna dengan $doctorName."
        )
        val url = "https://wa.me/$cleanNumber?text=$message"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}