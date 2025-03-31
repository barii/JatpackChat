package com.barii.jetpackchat.data

data class UserData(
    val userId: String? = "",
    val name: String? = "",
    val number: String? =  "",
    val email: String? = "",
    val imageUrl: String? = "",
) {
    fun toMap() = mapOf(
        "userId" to userId,
        "name" to name,
        "number" to number,
        "email" to email,
        "imageUrl" to imageUrl,
    )

}

data class ChatData(
    val chatId: String? = "",
    val user1: ChatUser = ChatUser(),
    val user2: ChatUser = ChatUser(),
)

data class ChatUser(
    val userId: String? = "",
    val name: String? = "",
    val imageUrl: String? = "",
    val number: String? = ""
)