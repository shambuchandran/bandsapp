package com.example.bands.data

data class UserData(
    var userId: String? = null,
    var name: String? = null,
    var phoneNumber: String? = null,
    var imageUrl: String? = null,
) {
//    fun toMap() = mapOf(
//        "userid" to userId,
//        "name" to name,
//        "phoneNumber" to phoneNumber,
//        "imageUrl" to imageUrl
//    )
}
data class ChatData(
    val chatId :String? ="",
    val user1:ChatUser =ChatUser(),
    val user2:ChatUser =ChatUser()
)
data class ChatUser(
    val userId: String? = "",
    val name :String? = "",
    val phoneNumber :String? = "",
    val imageUrl: String? = "",
)
data class Message(
    val sendBy: String? = "",
    val message :String? = "",
    val timeStamp :String? = "",
)
data class Status(
    val user: ChatUser = ChatUser(),
    val imageUrl: String? = null,
    val timeStamp :Long? = null,
)
