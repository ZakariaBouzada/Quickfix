package com.example.bikerepairapp.ui.messages

data class ChatUi(
    val chatId: String,
    val requestId: String,
    val customerId: String,
    val mechanicId: String,
    val lastMessage: String,
    val lastMessageAtMillis: Long
)
