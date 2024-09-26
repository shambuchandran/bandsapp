package com.example.bands.data

data class UserData(
    var userId: String? = null,
    var name: String? = null,
    var phoneNumber: String? = null,
    var imageUrl: String? = null,
) {
    fun toMap() = mapOf(
        "userid" to userId,
        "name" to name,
        "phoneNumber" to phoneNumber,
        "imageUrl" to imageUrl
    )
}
data class ChatData(
    val chatId :String? ="",
    val user1:ChatUser =ChatUser(),
    val user2:ChatUser =ChatUser()
)
data class ChatUser(
    val chatUserId: String? = "",
    val chatName :String? = "",
    val chatPhoneNumber :String? = "",
    val chatImageUrl: String? = "",
)
data class Message(
    val sendBy: String? = "",
    val message :String? = "",
    val timeStamp :String? = "",

)
