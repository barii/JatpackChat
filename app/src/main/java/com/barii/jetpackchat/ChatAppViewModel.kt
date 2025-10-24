package com.barii.jetpackchat

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barii.jetpackchat.data.COLLECTION_CHAT
import com.barii.jetpackchat.data.COLLECTION_USER
import com.barii.jetpackchat.data.Event
import com.barii.jetpackchat.data.ChatData
import com.barii.jetpackchat.data.ChatUser
import com.barii.jetpackchat.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatAppViewModel @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseFirestore,
    val storage: FirebaseStorage
) : ViewModel() {
    val inProgress = mutableStateOf(false)
    val popupNotification = mutableStateOf<Event<String>?>(null)
    val signedIn = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)

    val chats = mutableStateOf<List<ChatData>>(listOf())
    val inProgressChats = mutableStateOf(false)

    init {
        //logOut()
        //handleException(customerMessage = "Firebase auth exception")
        val currentUser = auth.currentUser
        signedIn.value = currentUser != null
        currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                getUserData(uid)
            }
        }
    }

    fun signup(name: String, number: String, email: String, password: String) {
        viewModelScope.launch {
            onSignup(name, number, email, password)
        }
    }

    private suspend fun onSignup(name: String, number: String, email: String, password: String) : Boolean{
        return try {
            if (name.isBlank() || number.isBlank() || email.isBlank() || password.isBlank()) {
                throw Exception("Please fill in all fields")
            }

            inProgress.value = true

            val snapshot = db.collection(COLLECTION_USER)
                .whereEqualTo("number", number)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                throw Exception("User with this number already exists")
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            authResult.user?.let {
                signedIn.value = true
                return createOrUpdateProfile(name, number, email, "").also {
                    if (!it) throw Exception("Profile creation failed")
                }
            } ?: throw Exception("Signup failed")
        } catch (e: Exception) {
            handleException(customerMessage = e.message ?: "Signup failed")
            false
        } finally {
            inProgress.value = false
        }
    }

    private suspend fun createOrUpdateProfile(
        name: String? = null,
        number: String? = null,
        email: String? = null,
        imageUrl: String? = null
    ) : Boolean {
        val uid = auth.currentUser?.uid
        val user = UserData(
            userId = uid,
            name = name ?: userData.value?.name,
            number = number ?: userData.value?.number,
            email = email ?: userData.value?.email,
            imageUrl = imageUrl ?: userData.value?.imageUrl
        )

        inProgress.value = true
        return try {
            uid?.let { uid ->
                val document = db.collection(COLLECTION_USER).document(uid)
                val snapshot = document.get().await()

                if (snapshot.exists()) {
                    snapshot.reference.update(user.toMap()).await()
                } else {
                    document.set(user).await()
                }
                getUserData(uid)
            }
            true
        } catch (e: Exception) {
            handleException(customerMessage = "Failed to create or update profile")
            false
        } finally {
            inProgress.value = false
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            onLogin(email, password)
        }
    }

    private suspend fun onLogin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            handleException(customerMessage = "Please fill in all fields")
            return
        }

        try {
            inProgress.value = true
            val authResult = auth.signInWithEmailAndPassword(email, password).await()

            authResult.user?.uid?.let { uid ->
                getUserData(uid)
            } ?: throw Exception("Login failed")
            signedIn.value = true
        } catch (e: Exception) {
            handleException(e, "Login failed")
        } finally {
            inProgress.value = false
        }

//        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                signedIn.value = true
//                inProgress.value = false
//                auth.currentUser?.uid?.let { uid ->
//                    getUserData(uid)
//                }
//            } else {
//                handleException(task.exception, "Login failed")
//            }
//        }.addOnFailureListener {
//            handleException(it, "Login failed")
//        }
}

    fun updateProfileData(name: String, number: String, status: String) {
        viewModelScope.launch {
            createOrUpdateProfile(name = name, number = number)
        }
    }

    private suspend fun getUserData(uid: String): UserData? {
        inProgress.value = true
        try {
            val user = db.collection(COLLECTION_USER).document(uid).get().await().toObject<UserData>()
            userData.value = user
            populateChat()
            return user
        } catch (e: Exception) {
            handleException(e, "Failed to get user data")
            return null
        } finally {
            inProgress.value = false
        }
    }

//    private fun getUserData(uid: String) {
//        inProgress.value = true
//        db.collection(COLLECTION_USER).document(uid)
//            .addSnapshotListener { value, error ->
//                if (error != null) {
//                    handleException(customerMessage = "Failed to get user data")
//                }
//                if (value != null) {
//                    val user = value.toObject<UserData>()
//                    userData.value = user
//                    inProgress.value = false
//
//                }
//            }
//    }

    fun logOut() {
        auth.signOut()
        signedIn.value = false
        userData.value = null
        popupNotification.value = Event("Logged out successfully")
    }

    private fun handleException(exception: Exception? = null, customerMessage: String = "") {
        Log.e("ChatApplicaiton", "Chat app exception", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customerMessage.isBlank()) errorMsg else "$customerMessage: $errorMsg"
        popupNotification.value = Event(message)
        inProgress.value = false
    }

    private suspend fun uploadImage(uri: Uri): Uri? {
        inProgress.value = true

        try {
            val uuid = UUID.randomUUID().toString()

            val storageRef = storage.reference
            val imageRef = storageRef.child("images/${uuid}")
            val snapshot = imageRef.putFile(uri).await()

            val imageUri = snapshot.metadata?.reference?.downloadUrl?.await()
            return imageUri
        } catch (e: Exception) {
            handleException(e, "Failed to upload image")
            return null
        } finally {
            inProgress.value = false
        }
    }

    public fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            uploadImage(uri).let { imageUri ->
                val uriString = imageUri.toString()
                createOrUpdateProfile(imageUrl = uriString)
            }
        }
    }

    fun addChat(number: String) {
        viewModelScope.launch {
            onAddChat(number)
        }
    }

    suspend fun onAddChat(number: String) {
        if (number.isEmpty() || !number.isDigitsOnly()) {
            handleException(customerMessage = "Invalid number")
            return
        } else {
            val room = db.collection(COLLECTION_CHAT).where(
                Filter.or(
                    Filter.and(
                        Filter.equalTo("user1.number", number),
                        Filter.equalTo("user2.number", userData.value?.number)
                    ),
                    Filter.and(
                        Filter.equalTo("user1.number", userData.value?.number),
                        Filter.equalTo("user2.number", number)
                    )
                )
            ).get().await()

            if(!room.isEmpty) {
                val partner = db.collection(COLLECTION_CHAT).whereEqualTo("number", number)
                    .get()
                    .await()

                if (partner.isEmpty) {
                    handleException(customerMessage = "User not found")
                } else {
                    val chatPartner = partner.toObjects(UserData::class.java).firstOrNull()
                    val id = db.collection(COLLECTION_CHAT).document().id
                    val chat = ChatData(
                        id,
                        ChatUser(
                            userData.value?.userId ?: "",
                            userData.value?.name ?: "",
                            userData.value?.number ?: "",
                            userData.value?.imageUrl ?: ""
                        ),
                        ChatUser(
                            chatPartner?.userId ?: "",
                            chatPartner?.name ?: "",
                            chatPartner?.number ?: "",
                            chatPartner?.imageUrl ?: ""
                        )
                    )

                    db.collection(COLLECTION_CHAT).document(id).set(chat)
                }
            }
        }
    }

    private fun populateChat() {
        viewModelScope.launch(Dispatchers.IO) {
            inProgressChats.value = true

            try {
                val r = db.collection(COLLECTION_CHAT).where(
                    Filter.or(
                        Filter.equalTo("user1.number", userData.value?.number),
                        Filter.equalTo("user2.number", userData.value?.number)
                    )
                )
                    .get()
                    .await()

                chats.value = r.documents.mapNotNull { it.toObject(ChatData::class.java) }
            } catch (e: Exception) {
                handleException(e, "Failed to populate chat")
            } finally {
                inProgressChats.value = false
            }
        }
    }
}