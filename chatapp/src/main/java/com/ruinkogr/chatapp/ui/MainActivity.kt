package com.ruinkogr.chatapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ruinkogr.chatapp.data.remote.retrofit.SessionManager
import com.ruinkogr.chatapp.data.storage.EncryptedPrefsTokenStorage
import com.ruinkogr.chatapp.data.storage.TokenStorage
import com.ruinkogr.chatapp.ui.auth.AuthViewModel
import com.ruinkogr.chatapp.ui.auth.LoginScreen
import com.ruinkogr.chatapp.ui.auth.RegisterScreen
import com.ruinkogr.chatapp.ui.chat.ChatViewModel
import com.ruinkogr.chatapp.ui.chat.FullChatScreen
import com.ruinkogr.chatapp.ui.theme.ChatAppTheme
import com.ruinkogr.chatapp.ui.users.UsersScreen
import com.ruinkogr.chatapp.ui.users.UsersViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var tokenStorage: TokenStorage

    private val authViewModel: AuthViewModel by viewModels()
    private val usersViewModel: UsersViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChatAppTheme {
                val navController = rememberNavController()

                val startScreen = remember {
                    // read current user ID sync one time before Compose effects
                    // Use runBlocking if suspend
                    val storage = tokenStorage as EncryptedPrefsTokenStorage
                    val id = storage.getCurrentUserIdSync()
                    if (id == -1) "login_screen" else "users_list"
                }


                NavHost(
                    navController = navController,
                    startDestination = startScreen
                ) {
                    // Login screen
                    composable("login_screen") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onSuccessLogin = {
                                navController.navigate("users_list") {
                                    popUpTo("login_screen") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                // Navigate to Registration screen
                                navController.navigate("register_screen")
                            }
                        )
                    }

                    // Register Screen
                    composable("register_screen") {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onSuccessRegister = {
                                // Return to Login screen
                                navController.popBackStack()
                            },
                            onBackToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Users screen
                    composable("users_list") {
                        val usersViewModel: UsersViewModel = hiltViewModel()
                        val authViewModel: AuthViewModel = hiltViewModel()

                        UsersScreen(
                            usersViewModel = usersViewModel,
                            authViewModel = authViewModel,
                            onUserClick = { selectedUserId ->
                                navController.navigate("chat_screen/$selectedUserId")
                            },
                            onLogoutSuccess = {
                                //TODO
                            }
                        )
                    }

                    // Chat screen
                    composable("chat_screen/{chatWithUserId}") { backStackEntry ->
                        val chatViewModel: ChatViewModel = hiltViewModel()
                        FullChatScreen(viewModel = chatViewModel)
                    }
                }
            }
        }
    }
}