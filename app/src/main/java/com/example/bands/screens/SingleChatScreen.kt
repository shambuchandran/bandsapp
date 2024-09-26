package com.example.bands.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bands.BandsViewModel
import com.example.bands.CommonDivider

@Composable
fun SingleChatScreen(navController: NavController, viewModel: BandsViewModel,chatId:String) {
    var reply by rememberSaveable {
        mutableStateOf("")
    }
    val onSendReply ={
        viewModel.onSendReply(chatId,reply)
        reply=""
    }
    ReplyBox(reply = reply, onReplyChange ={reply = it} ,onSendReply=onSendReply)
    
}

@Composable
fun ReplyBox(reply :String, onReplyChange :(String) -> Unit, onSendReply:()->Unit) {
    Column(
        Modifier.fillMaxWidth()) {
        CommonDivider()
        Row (
            Modifier
                .fillMaxWidth()
                .padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween){
                TextField(value = reply, onValueChange = onReplyChange, maxLines = 3, shape = RoundedCornerShape(8.dp))
            Button(onClick = onSendReply) {
                Text(text = "Send")
            }
        }

    }
}