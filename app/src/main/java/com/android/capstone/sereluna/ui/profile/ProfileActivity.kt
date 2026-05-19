package com.android.capstone.sereluna.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.android.capstone.sereluna.databinding.ActivityProfileBinding
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var userViewModel: UserViewModel
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.profileImageView.setImageURI(selectedImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        setupObservers()
        setupClickListeners()

        // Initial data load
        userViewModel.loadUserData()
    }

    private fun setupClickListeners() {
        binding.changePhotoButton.setOnClickListener {
            pickImageFromGallery()
        }

        binding.saveButton.setOnClickListener {
            showConfirmationDialog()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        // Observer for user data loading
        userViewModel.userData.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val userData = state.data
                    binding.nameEditText.setText(userData["name"] as? String ?: "")
                    binding.emailTextView.setText(userData["email"] as? String ?: "")
                    val photoUrl = userData["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get().load(photoUrl).into(binding.profileImageView)
                    }
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast("Error: ${state.message}")
                }
            }
        }

        // Observer for profile update status
        userViewModel.updateState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    showToast("Profile updated successfully!")
                    setResult(Activity.RESULT_OK)
                    // Reload data to show the latest changes
                    userViewModel.loadUserData(true)
                    selectedImageUri = null // Reset after successful upload
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    showToast("Update Failed: ${state.message}")
                }
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Save Changes")
            .setMessage("Are you sure you want to save these changes?")
            .setPositiveButton("Yes") { _, _ ->
                val name = binding.nameEditText.text.toString().trim()
                if (name.isNotEmpty()) {
                    userViewModel.saveUserProfile(name, selectedImageUri)
                } else {
                    showToast("Name cannot be empty.")
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
