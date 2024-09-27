package com.example.bands.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bands.BandsViewModel
import com.example.bands.CommonDivider
import com.example.bands.CommonImage
import com.example.bands.DestinationScreen
import com.example.bands.data.Message
import com.example.bands.navigateTo

@Composable
fun SingleChatScreen(navController: NavController, viewModel: BandsViewModel, chatId: String) {
    var reply by rememberSaveable {
        mutableStateOf("")
    }
    val onSendReply = {
        viewModel.onSendReply(chatId, reply)
        reply = ""
    }
    val mainUser = viewModel.userData.value
    val currentChat = viewModel.chats.value.first { it.chatId == chatId }

    val chatUser =
        if (mainUser?.userId == currentChat.user1.userId) currentChat.user2 else currentChat.user1
    val chatMessages = viewModel.chatMessages

    LaunchedEffect(key1 = Unit) {
        viewModel.loadMessages(chatId)

    }
    BackHandler {
        navigateTo(navController, DestinationScreen.ChatList.route)
        viewModel.releaseMessages()

    }
    Column {
        ChatHeader(name = chatUser.name ?: "", imageUrl = chatUser.imageUrl ?: "") {
            navController.popBackStack()
            viewModel.releaseMessages()
        }
        MessageBox(
            modifier = Modifier.weight(1f),
            chatMessages = chatMessages.value,
            currentUserId = mainUser?.userId ?: ""
        )
        ReplyBox(reply = reply, onReplyChange = { reply = it }, onSendReply = onSendReply)
    }
}

@Composable
fun MessageBox(modifier: Modifier, chatMessages: List<Message>, currentUserId: String) {
    LazyColumn(modifier) {
        items(chatMessages) { Message ->
            val alignment = if (Message.sendBy == currentUserId) Alignment.End else Alignment.Start
            val color =
                if (Message.sendBy == currentUserId) Color(0xff68c400) else Color(0xffffc0c0)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = alignment
            ) {
                Text(
                    text = Message.message ?: "",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .padding(12.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }


        }
    }

}

@Composable
fun ReplyBox(reply: String, onReplyChange: (String) -> Unit, onSendReply: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
    ) {
        CommonDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextField(
                value = reply,
                onValueChange = onReplyChange,
                maxLines = 3,
                shape = RoundedCornerShape(8.dp)
            )
            Button(onClick = onSendReply) {
                Text(text = "Send")
            }
        }

    }
}

@Composable
fun ChatHeader(name: String, imageUrl: String, onBacKClicked: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.KeyboardArrowLeft,
            contentDescription = "back",
            Modifier
                .clickable { onBacKClicked.invoke() }
                .padding(4.dp))
        CommonImage(
            data = imageUrl, modifier = Modifier
                .padding(4.dp)
                .size(50.dp)
                .clip(CircleShape)
        )
        Text(text = name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
    }
}