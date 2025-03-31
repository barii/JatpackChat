package com.barii.jetpackchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.barii.jetpackchat.ui.ChatListScreen
import com.barii.jetpackchat.ui.ChatScreen
import com.barii.jetpackchat.ui.LoginScreen
import com.barii.jetpackchat.ui.ProfileScreen
import com.barii.jetpackchat.ui.SignupScreen
import com.barii.jetpackchat.ui.StatusListScreen
import com.barii.jetpackchat.ui.StatusScreen
import com.barii.jetpackchat.ui.theme.JetpackChatTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class DestinationScreen(val route: String) {
    object Signup: DestinationScreen("signup")
    object Login: DestinationScreen("login")
    object Profile: DestinationScreen("profile")
    object ChatList: DestinationScreen("chatList")
    object Chat: DestinationScreen("chat/{chatId}") {
        fun CreateRoute(id :String) = "chat/$id"
    }
    object StatusList: DestinationScreen("statusList")
    object Status: DestinationScreen("status/{statusId") {
        fun CreateRoute(id :String) = "status/$id"
    }

}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackChatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatAppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ChatAppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel = hiltViewModel<ChatAppViewModel>()

    NotificationMessage(viewModel = viewModel)

    NavHost(navController = navController, startDestination = DestinationScreen.Signup.route, modifier = modifier) {
        composable(DestinationScreen.Signup.route) {
            SignupScreen(navController, viewModel)
        }
        composable(DestinationScreen.Login.route) {
            LoginScreen(navController, viewModel)
        }
        composable(DestinationScreen.Profile.route) {
            ProfileScreen(navController, viewModel)
        }
        composable(DestinationScreen.StatusList.route) {
            StatusListScreen(navController)
        }
        composable(DestinationScreen.Status.route) {
            StatusScreen(statusId = "123")
        }
        composable(DestinationScreen.ChatList.route) {
            ChatListScreen(navController, viewModel)
        }
        composable(DestinationScreen.Chat.route) {
            ChatScreen(chatId = "123")
        }

    }
}