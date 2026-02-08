package com.example.bikerepairapp.ui.messages

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ChatFragment : Fragment(R.layout.fragment_chat) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rv: RecyclerView
    private lateinit var et: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvTitle: TextView

    private val adapter = ChatMessagesAdapter()

    private var chatId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatId = requireArguments().getString(ARG_CHAT_ID)
        if (chatId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing chat id", Toast.LENGTH_SHORT).show()
            dismissSelf()
            return
        }

        rv = view.findViewById(R.id.rvChatMessages)
        et = view.findViewById(R.id.etChatMessage)
        btnSend = view.findViewById(R.id.btnSendMessage)
        tvTitle = view.findViewById(R.id.tvChatTitle)

        rv.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rv.adapter = adapter

        tvTitle.text = "Chat"

        listenMessages(chatId!!)

        btnSend.setOnClickListener {
            val text = et.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener
            sendMessage(chatId!!, text)
            et.setText("")
        }
    }

    private fun listenMessages(chatId: String) {
        firestore.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val items = snap.documents.mapNotNull { d ->
                    val msg = d.getString("text") ?: return@mapNotNull null
                    val senderId = d.getString("senderId") ?: ""
                    val t = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    ChatMessageUi(
                        id = d.id,
                        text = msg,
                        senderId = senderId,
                        createdAtMillis = t,
                        isMine = senderId == auth.currentUser?.uid
                    )
                }
                adapter.submit(items)
                if (items.isNotEmpty()) rv.scrollToPosition(items.size - 1)
            }
    }

    private fun sendMessage(chatId: String, text: String) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val msgRef = firestore.collection("chats").document(chatId)
            .collection("messages")
            .document()

        val chatRef = firestore.collection("chats").document(chatId)

        firestore.runBatch { batch ->
            batch.set(msgRef, mapOf(
                "text" to text,
                "senderId" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            ))

            batch.update(chatRef, mapOf(
                "lastMessage" to text,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderId" to uid
            ))
        }
    }

    private fun dismissSelf() {
        // If hosted inside ChatBottomSheet, it will handle dismissal.
        parentFragment?.let {
            if (it is ChatBottomSheet) it.dismiss()
        }
    }

    companion object {
        private const val ARG_CHAT_ID = "chatId"

        fun newInstance(chatId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply { putString(ARG_CHAT_ID, chatId) }
            }
        }
    }
}

data class ChatMessageUi(
    val id: String,
    val text: String,
    val senderId: String,
    val createdAtMillis: Long,
    val isMine: Boolean
)
