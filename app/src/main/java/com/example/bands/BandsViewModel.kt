package com.example.bands

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import com.example.bands.data.CHATS
import com.example.bands.data.ChatData
import com.example.bands.data.ChatUser
import com.example.bands.data.Event
import com.example.bands.data.MESSAGE
import com.example.bands.data.Message
import com.example.bands.data.STATUS
import com.example.bands.data.Status
import com.example.bands.data.USER_NODE
import com.example.bands.data.UserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BandsViewModel @Inject constructor(
    val auth: FirebaseAuth,
    var db: FirebaseFirestore,
    var storage: FirebaseStorage
) : ViewModel() {
    var inProgress = mutableStateOf(false)
    var inProgressChats = mutableStateOf(false)
    var eventMutableState = mutableStateOf<Event<String>?>(null)
    var signIn = mutableStateOf(false)
    val userData = mutableStateOf<UserData?>(null)
    val chats = mutableStateOf<List<ChatData>>(listOf())
    val chatMessages = mutableStateOf<List<Message>>(listOf())
    var inProgressChatMessages = mutableStateOf(false)
    var currentChatListener: ListenerRegistration? = null

    //val status = mutableStateOf<List<Status>>(listOf())
    private val _status = MutableStateFlow<List<Status>>(emptyList())
    val status: StateFlow<List<Status>> = _status.asStateFlow()

    //var inProgressStatus = mutableStateOf(false)
    var inProgressStatus = MutableStateFlow(false)

    init {
        val currentUser = auth.currentUser
        signIn.value = currentUser != null
        currentUser?.uid.let {
            if (it != null) {
                getUserData(it)
            }
        }
    }

    fun signUp(name: String, phoneNumber: String, email: String, password: String) {
        inProgress.value = true
        if (name.isEmpty() or phoneNumber.isEmpty() or email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please fill all fields")
            return
        }
        inProgress.value = true
        db.collection(USER_NODE).whereEqualTo("phoneNumber", phoneNumber).get()
            .addOnSuccessListener {
                if (it.isEmpty) {
                    auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            signIn.value = true
                            createOrUpdateProfile(name, phoneNumber)
                            Log.d("Auth", "user logged IN")
                        } else {
                            handleException(it.exception, customMessage = "SignUp Failed")
                        }
                    }
                } else {
                    handleException(customMessage = "Number already exists")
                    inProgress.value = false
                }
            }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() or password.isEmpty()) {
            handleException(customMessage = "Please fill all fields")
            return
        } else {
            inProgress.value = true
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener {
                if (it.isSuccessful) {
                    signIn.value = true
                    inProgress.value = false
                    auth.currentUser?.uid.let {
                        if (it != null) {
                            getUserData(it)
                        }
                    }
                } else {
                    handleException(exception = it.exception, customMessage = "Login failed")
                }
            }
        }

    }

    fun uploadProfileImage(uri: Uri) {
        uploadImage(uri) {
            createOrUpdateProfile(imageUrl = it.toString())
        }
    }

    private fun uploadImage(uri: Uri, onSuccess: (Uri) -> Unit) {
        inProgress.value = true
        val storageRef = storage.reference
        val uuid = UUID.randomUUID()
        val imageRef = storageRef.child("images/$uuid")
        val uploadTask = imageRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            val result = it.metadata?.reference?.downloadUrl
            result?.addOnSuccessListener(onSuccess)
            inProgress.value = false
        }.addOnFailureListener {
            handleException(it)
        }
    }

    fun createOrUpdateProfile(
        name: String? = null,
        phoneNumber: String? = null,
        imageUrl: String? = null
    ) {
        val uid = auth.currentUser?.uid
        val userData = UserData(
            userId = uid,
            name = name ?: this.userData.value?.name,
            phoneNumber = phoneNumber ?: this.userData.value?.phoneNumber,
            imageUrl = imageUrl ?: this.userData.value?.imageUrl
        )
        uid.let {
            inProgress.value = true
            db.collection(USER_NODE).document(uid!!).get().addOnSuccessListener {
                if (it.exists()) {
                    db.collection(USER_NODE).document(uid).set(userData).addOnSuccessListener {
                        inProgress.value = false
                        getUserData(uid)
                    }.addOnFailureListener {
                        handleException(it, customMessage = "Cannot update user")
                        inProgress.value = false
                    }
                } else {
                    db.collection(USER_NODE).document(uid).set(userData)
                    inProgress.value = false
                    getUserData(uid)
                }
            }.addOnFailureListener {
                handleException(it, "Cannot retrieve User")
                inProgress.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
        signIn.value = false
        userData.value = null
        releaseMessages()
        currentChatListener = null
        eventMutableState.value = Event("logout")
    }

    fun onAddChat(phoneNumber: String) {
        if (phoneNumber.isEmpty() || !phoneNumber.isDigitsOnly()) {
            handleException(customMessage = "Enter numbers only")
            return
        } else {
            db.collection(CHATS).where(
                Filter.or(
                    Filter.and(
                        (Filter.equalTo("user1.phoneNumber", phoneNumber)),
                        (Filter.equalTo("user2.phoneNumber", userData.value?.phoneNumber))
                    ),
                    Filter.and(
                        (Filter.equalTo("user1.phoneNumber", userData.value?.phoneNumber)),
                        (Filter.equalTo("user2.phoneNumber", phoneNumber))
                    )
                )
            ).get().addOnSuccessListener {
                if (it.isEmpty) {
                    db.collection(USER_NODE).whereEqualTo("phoneNumber", phoneNumber).get()
                        .addOnSuccessListener {
                            if (it.isEmpty) {
                                handleException(customMessage = "Number not found")
                            } else {
                                val chatPartner = it.toObjects<UserData>()[0]
                                val id = db.collection(CHATS).document().id

                                val chatData = ChatData(
                                    chatId = id,
                                    user1 = ChatUser(
                                        userData.value?.userId,
                                        userData.value?.name,
                                        userData.value?.phoneNumber,
                                        userData.value?.imageUrl
                                    ),
                                    user2 = ChatUser(
                                        chatPartner.userId,
                                        chatPartner.name,
                                        chatPartner.phoneNumber,
                                        chatPartner.imageUrl
                                    )
                                )
                                db.collection(CHATS).document(id).set(chatData)
                            }
                        }.addOnFailureListener {
                            handleException(it)
                        }
                } else {
                    handleException(customMessage = "Contact already exists")
                }
            }.addOnFailureListener {
                handleException(it)
            }
        }

    }

    fun loadChat() {
        inProgressChats.value = true
        db.collection(CHATS).where(
            Filter.or(
                Filter.equalTo("user1.userId", userData.value?.userId),
                Filter.equalTo("user2.userId", userData.value?.userId)
            )
        ).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error)
            }
            if (value != null) {
                chats.value = value.documents.mapNotNull {
                    it.toObject<ChatData>()
                }
                inProgressChats.value = false
            }

        }
    }

    private fun getUserData(uid: String) {
        inProgress.value = true
        db.collection(USER_NODE).document(uid).addSnapshotListener { value, error ->
            if (error != null) {
                handleException(error, "Cannot retrieve User")
            }
            if (value != null) {
                val user = value.toObject<UserData>()
                userData.value = user
                inProgress.value = false
                loadChat()
                loadStatuses()
            }
        }
    }

    fun onSendReply(chatId: String, message: String) {
        val time = Calendar.getInstance().time.toString()
        val chatMessage = Message(userData.value?.userId, message, time)
        db.collection(CHATS).document(chatId).collection(MESSAGE).document().set(chatMessage)

    }

    fun loadMessages(chatId: String) {
        inProgressChatMessages.value = true
        currentChatListener = db.collection(CHATS).document(chatId).collection(MESSAGE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    handleException(error)
                }
                if (value != null) {
                    chatMessages.value = value.documents.mapNotNull {
                        it.toObject<Message>()
                    }.sortedBy { it.timeStamp }
                    inProgressChatMessages.value = false
                }
            }
    }

    fun releaseMessages() {
        chatMessages.value = listOf()
        currentChatListener = null
    }

    fun handleException(exception: Exception? = null, customMessage: String? = null) {
        Log.e("Bandsapp", "exception", exception)
        exception?.printStackTrace()
        val errorMsg = exception?.localizedMessage ?: ""
        val message = if (customMessage.isNullOrEmpty()) errorMsg else customMessage
        eventMutableState.value = Event(message)
        inProgress.value = false
    }

    fun uploadStatus(uri: Uri) {
        uploadImage(uri) {
            createStatus(it.toString())
        }
    }

    fun createStatus(imageUrl: String?) {
        val newStatus = Status(
            ChatUser(
                userData.value?.userId,
                userData.value?.name,
                userData.value?.imageUrl,
                userData.value?.phoneNumber,
            ),
            imageUrl,
            System.currentTimeMillis()
        )
        db.collection(STATUS).document().set(newStatus)
    }

    fun loadStatuses() {
        val statusShowTime = 24L * 60 * 60 * 1000
        val timeFrame = System.currentTimeMillis() - statusShowTime
        inProgressStatus.value = true
        db.collection(CHATS)
            .where(
                Filter.or(
                    Filter.equalTo("user1.userId", userData.value?.userId),
                    Filter.equalTo("user2.userId", userData.value?.userId)
                )
            ).addSnapshotListener { value, error ->
                if (error != null) {
                    handleException(error)
                    inProgressStatus.value = false
                    return@addSnapshotListener
                }
                if (value != null) {
                    val currentConnection = arrayListOf(userData.value?.userId)
                    val chats = value.toObjects<ChatData>()
                    chats.forEach { chat ->
                        if (chat.user1.userId == userData.value?.userId) {
                            currentConnection.add(chat.user2.userId)
                        } else {
                            currentConnection.add(chat.user1.userId)
                        }
                        db.collection(STATUS).whereGreaterThan("timeStamp", timeFrame)
                            .whereIn("user.userId", currentConnection)
                            .addSnapshotListener { value, error ->
                                if (error != null) {
                                    handleException(error)
                                    inProgressStatus.value = false
                                }
                                if (value != null) {
                                    //status.value = value.toObjects()
                                    _status.update { value.toObjects() }
                                    inProgressStatus.value = false
                                }
                            }
                    }
                } else {
                    inProgressStatus.value = false
                }
            }

    }

    fun removeStatus(index: Int) {
        if (index in _status.value.indices) {
            val selectedStatus = _status.value[index]
            deleteStatusFromFirebase(selectedStatus)
            _status.value = _status.value.toMutableList().apply {
                removeAt(index)
            }
        }
    }

    private fun deleteStatusFromFirebase(selectedStatus: Status) {
        val statusCollection = db.collection(STATUS)
        statusCollection.whereEqualTo("imageUrl", selectedStatus.imageUrl)
            .whereEqualTo("user.userId", selectedStatus.user.userId)
            .whereEqualTo("timeStamp", selectedStatus.timeStamp)
            .get().addOnSuccessListener { documents ->
                for (doc in documents) {
                    statusCollection.document(doc.id).delete().addOnSuccessListener {
                        Log.d("BandsViewModel", "Status successfully deleted!")
                    }.addOnFailureListener {
                        handleException(it)
                        Log.w("BandsViewModel", "Error deleting status", it)
                    }
                }
            }.addOnFailureListener {
                handleException(it)
                Log.w("BandsViewModel", "Error finding status to delete", it)
            }

    }
}
