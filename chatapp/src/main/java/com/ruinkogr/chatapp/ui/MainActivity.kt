package com.ruinkogr.chatapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ruinkogr.chatapp.data.remote.retrofit.AuthEvent
import com.ruinkogr.chatapp.data.remote.retrofit.SessionManager
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

                //  Logout listening...
                LaunchedEffect(Unit) {
                    sessionManager.authEvents.collect { event ->
                        when (event) {
                            is AuthEvent.Logout -> {
                                tokenStorage.clearTokens()

                                navController.navigate("login_screen") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                }

                // Suspend method call.
                // Null means the value is not read yet
                val isLoggedInState by produceState<Boolean?>(initialValue = null, tokenStorage) {
                    value = tokenStorage.isUserLoggedInSync()
                }

                if (isLoggedInState == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val startScreen = if (isLoggedInState == true) "users_list" else "login_screen"

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
                            UsersScreen(
                                viewModel = usersViewModel,
                                onUserClick = { selectedUserId ->
                                    navController.navigate("chat_screen/$selectedUserId")
                                }
                            )
                        }

                        // Chat screen
                        composable(
                            route = "chat_screen/{chatWithUserId}",
                            arguments = listOf(navArgument("chatWithUserId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val chatWithUserId = backStackEntry.arguments?.getInt("chatWithUserId") ?: 0
                            FullChatScreen(
                                viewModel = chatViewModel,
                                currentUserId = 1 , // TODO login must inform about ID
                                chatWithUserId = chatWithUserId
                            )
                        }
                    }
                }
            }
        }
    }
}