package com.example.bikerepairapp.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MessagesBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.ThemeOverlay_QuickFix_BottomSheet

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatsListener: ListenerRegistration? = null

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    private val adapter = ChatsAdapter { chat ->
        // Open the chat UI (bottom sheet on top)
        ChatBottomSheet.newInstance(chat.chatId).show(parentFragmentManager, "chat")
    }

    override fun onDestroyView() {
        chatsListener?.remove()
        chatsListener = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("customerLastSeenNotificationsAt", FieldValue.serverTimestamp())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottomsheet_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvChats)
        tvEmpty = view.findViewById(R.id.tvEmptyChats)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        val uid = auth.currentUser?.uid ?: run {
            showEmpty("Not logged in.")
            return
        }

        chatsListener?.remove()
        chatsListener = firestore.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener

                val chats = snap.documents.map { d ->
                    ChatUi(
                        chatId = d.id,
                        requestId = d.getString("requestId") ?: "",
                        customerId = d.getString("customerId") ?: "",
                        mechanicId = d.getString("mechanicId") ?: "",
                        lastMessage = d.getString("lastMessage") ?: "Open chat",
                        lastMessageAtMillis = d.getTimestamp("lastMessageAt")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.lastMessageAtMillis }

                if (chats.isEmpty()) showEmpty("No chats yet.")
                else {
                    tvEmpty.visibility = View.GONE
                    rv.visibility = View.VISIBLE
                    adapter.submit(chats)
                }
            }
    }



    private fun showEmpty(msg: String) {
        rv.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = msg
    }
}
