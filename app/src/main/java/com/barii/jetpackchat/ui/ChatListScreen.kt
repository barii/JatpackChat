package com.barii.jetpackchat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.barii.jetpackchat.ChatAppViewModel
import com.barii.jetpackchat.CommonProgressSpinner

@Composable
fun ChatListScreen(navController: NavController, viewModel: ChatAppViewModel) {
    val inProgress = viewModel.inProgressChats.value
    if (inProgress)
        CommonProgressSpinner()
    else {
        val chats = viewModel.chats.value
        val userData = viewModel.userData.value

        val showDialog = remember { mutableStateOf(false) }
        val onFabClick: () -> Unit = { showDialog.value = true }
        val onDismiss: () -> Unit = { showDialog.value = false }
        val onAddChat: (String) -> Unit = {
            viewModel.addChat(it)
            showDialog.value = false
        }

        Scaffold(
            floatingActionButton = { FAB(showDialog.value, onFabClick, onDismiss, onAddChat) },
            content = {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.headlineMedium, // Use headline styles for titles
                        textAlign = TextAlign.Center
                    )

                    if (chats.isEmpty())
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "No chats available")
                        }
                    else {
//                        LazyColumn(modifier = Modifier.weight(1f)) {
//                            items(chats) { chat ->
//                                val chatUser = if (chat.user1.userId == userData?.userId) chat.user2
//                                else chat.user1
//                                CommonRow(
//                                    imageUrl = chatUser.imageUrl ?: "",
//                                    name = chatUser.name ?: "---"
//                                ) {
//                                    chat.chatId?.let {id ->
//                                        navigateTo(
//                                            navController,
//                                            DestinationScreen.SingleChat.createRoute(id)
//                                        )
//                                    }
//                                }
//                            }
//                        }
                    }

                    BottomNavigationMenu(
                        selectedItem = BottomNavigationItem.CHATLIST,
                        navController = navController
                    )
                }
            }
        )
    }
}


@Composable
fun FAB(
    showDialog: Boolean,
    onFabClick: () -> Unit,
    onDismiss: () -> Unit,
    onAddChat: (String) -> Unit
) {

    val addChatNumber = remember { mutableStateOf("") }

    if (showDialog)
        AlertDialog(
            onDismissRequest = {
                onDismiss.invoke()
                addChatNumber.value = ""
            },
            confirmButton = {
                Button (onClick = {
                    onAddChat(addChatNumber.value)
                    addChatNumber.value = ""
                }) {
                    Text(text = "Add chat")
                }
            },
            title = { Text(text = "Add chat") },
            text = {
                OutlinedTextField(
                    value = addChatNumber.value,
                    onValueChange = { addChatNumber.value = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        )

    androidx.compose.material3.FloatingActionButton(
        onClick = onFabClick,
        containerColor = MaterialTheme.colorScheme.secondary,
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 40.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add chat",
            tint = Color.White,
        )
    }
}