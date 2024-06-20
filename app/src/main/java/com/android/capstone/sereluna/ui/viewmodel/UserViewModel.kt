package com.android.capstone.sereluna.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> get() = _userEmail

    private val _userPhotoUrl = MutableLiveData<String>()
    val userPhotoUrl: LiveData<String> get() = _userPhotoUrl

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        loadUserData()
    }

    fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = firestore.collection("users").document(userId)
            userRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        _userName.value = document.getString("name") ?: ""
                        _userEmail.value = document.getString("email") ?: ""
                        _userPhotoUrl.value = document.getString("photoUrl") ?: ""
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors here, such as logging
                }
        }
    }
}
