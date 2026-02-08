package com.example.bikerepairapp.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.example.bikerepairapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChatBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.ThemeOverlay_QuickFix_BottomSheet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId = requireArguments().getString(ARG_CHAT_ID) ?: return

        if (savedInstanceState == null) {
            val frag = ChatFragment.newInstance(chatId)
            childFragmentManager.beginTransaction()
                .replace(R.id.chatContainer, frag)
                .commit()
        }
    }

    companion object {
        private const val ARG_CHAT_ID = "chatId"

        fun newInstance(chatId: String): ChatBottomSheet {
            return ChatBottomSheet().apply {
                arguments = bundleOf(ARG_CHAT_ID to chatId)
            }
        }
    }
}
