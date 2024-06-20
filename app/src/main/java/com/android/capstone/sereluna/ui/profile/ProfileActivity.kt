package com.android.capstone.sereluna.ui.profile

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.android.capstone.sereluna.databinding.ActivityProfileBinding
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var userViewModel: UserViewModel

    private var uploadedPhotoUrl: String? = null
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        userViewModel = ViewModelProvider(this).get(UserViewModel::class.java)

        loadProfileData()

        binding.changePhotoButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.saveButton.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun loadProfileData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""
                        val photoUrl = document.getString("photoUrl") ?: ""

                        binding.nameEditText.setText(name)
                        binding.emailEditText.setText(email)
                        if (photoUrl.isNotEmpty()) {
                            Picasso.get().load(photoUrl).into(binding.profileImageView)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    showError("Failed to load profile: ${exception.message}")
                }
        }
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmation")
            .setMessage("Are you sure you want to save changes?")
            .setPositiveButton("Yes") { _, _ -> saveProfileData() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveProfileData() {
        val name = binding.nameEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val user = auth.currentUser

        if (user != null) {
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)

            firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty || documents.documents[0].id == userId) {
                        if (selectedImageUri != null) {
                            uploadPhotoAndSave(userRef, name, email)
                        } else {
                            updateProfileData(userRef, name, email)
                        }
                    } else {
                        showError("Email is already in use by another account.")
                    }
                }
        }
    }

    private fun uploadPhotoAndSave(userRef: DocumentReference, name: String, email: String) {
        val storageRef = storage.reference.child("profile_images/${auth.currentUser?.uid}")
        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnProgressListener { snapshot ->
                    val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount)
                    Log.d("UploadProgress", "Upload is $progress% done")
                }
                .addOnFailureListener { e ->
                    Log.e("UploadError", "Failed to upload photo: ${e.message}")
                    showError("Failed to upload photo: ${e.message}")
                }
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    storageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        uploadedPhotoUrl = task.result.toString()
                        updateProfileData(userRef, name, email)
                    } else {
                        showError("Failed to upload photo: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun updateProfileData(userRef: DocumentReference, name: String, email: String) {
        // Assuming updatedData is a HashMap<String, String?>
        val updatedData: HashMap<String, String?> = HashMap<String, String?>().apply {
            put("name", name)
            put("email", email)
            put("photoUrl", uploadedPhotoUrl ?: "")
        }

        userRef.update(updatedData as Map<String, Any>)
            .addOnSuccessListener {
                uploadedPhotoUrl = null // Reset uploaded photo URL after update
                showToast("Profile updated successfully")
                loadProfileData() // Refresh displayed profile data
            }
            .addOnFailureListener { e ->
                showError("Failed to update profile: ${e.message}")
            }
    }


    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showPermissionExplanation()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
            }
        } else {
            pickImageFromGallery()
        }
    }

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs access to your external storage to allow you to pick a profile photo.")
            .setPositiveButton("Allow") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
            }
            .setNegativeButton("Deny", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImageFromGallery()
            } else {
                showError("Permission denied to read your External storage")
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            binding.profileImageView.setImageURI(selectedImageUri)
        }
    }

    companion object {
        private const val REQUEST_READ_EXTERNAL_STORAGE = 101
        private const val PICK_IMAGE_REQUEST = 102
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
