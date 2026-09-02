package com.ruinkogr.chatapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ruinkogr.chatapp.ui.settings.SettingsScreen
import com.ruinkogr.chatapp.ui.settings.SettingsViewModel
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

    // State for tracking push taps in the foreground/background (warm start)
    private var openChatUserIdByPush by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check the intent on a cold start of the app
        val initialChatId = intent?.getIntExtra("OPEN_CHAT_WITH_USER_ID", -1)?.takeIf { it != -1 }

        setContent {
            ChatAppTheme {
                val navController = rememberNavController()

                // Determine the start screen ONCE when the activity is created
                val startScreen = remember {
                    val storage = tokenStorage as EncryptedPrefsTokenStorage
                    val currentUserId = storage.getCurrentUserIdSync()
                    when {
                        currentUserId == -1 -> "login_screen"
                        initialChatId != null -> "chat_screen/$initialChatId" // Open the chat directly
                        else -> "users_list"
                    }
                }

                // Listen for logout regardless of which screen is currently shown (explicit
                // Logout tap, or an automatic one from an interceptor-detected invalid session)
                LaunchedEffect(Unit) {
                    authViewModel.logoutEvent.collect {
                        // A burst of concurrent requests can each independently detect the same
                        // dead session and each report a logout; avoid stacking duplicate entries.
                        if (navController.currentDestination?.route == "login_screen") return@collect

                        Log.d("Navigation", "Logout event received, redirecting to Login screen")
                        navController.navigate("login_screen") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                // Listen for new push taps (warm start via onNewIntent)
                openChatUserIdByPush?.let { chatId ->
                    LaunchedEffect(chatId) {
                        navController.navigate("chat_screen/$chatId") {
                            // Clear the stack down to the users list to avoid piling up screens
                            popUpTo("users_list") { saveState = true }
                            launchSingleTop = true
                        }
                        openChatUserIdByPush = null // Reset the state after navigating
                    }
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
//                        val authViewModel: AuthViewModel = hiltViewModel()
                        // 1. Wrap navigation actions in remember
                        val onUserClickRemembered = remember(navController) {
                            { selectedUserId: Int -> navController.navigate("chat_screen/$selectedUserId") }
                        }
                        val onSettingsClickRemembered = remember(navController) {
                            { navController.navigate("settings_screen") }
                        }

                        UsersScreen(
                            usersViewModel = usersViewModel,
                            authViewModel = authViewModel,
                            onUserClick = onUserClickRemembered,
                            onSettingsClick = onSettingsClickRemembered
                        )
                    }

                    // Chat screen
                    composable("chat_screen/{chatWithUserId}") { backStackEntry ->
                        val chatViewModel: ChatViewModel = hiltViewModel()

                        // Intercept the system "Back" button on the phone.
                        // If this chat opened as the start screen, "Back" would close the app.
                        // We need to force-redirect the user to the list instead.
                        androidx.activity.compose.BackHandler {
                            // Check whether the users list screen is in the back stack
                            val hasUsersListInStack = navController.previousBackStackEntry != null
                            if (hasUsersListInStack) {
                                navController.popBackStack()
                            } else {
                                // If the list isn't in the stack (cold start straight into chat),
                                // clear the stack and open the users list manually
                                navController.navigate("users_list") {
                                    popUpTo("chat_screen/{chatWithUserId}") { inclusive = true }
                                }
                            }
                        }
                        FullChatScreen(viewModel = chatViewModel)
                    }
                    // Settings Screen
                    composable("settings_screen") {
                        val settingsViewModel: SettingsViewModel = hiltViewModel()
                        SettingsScreen(settingsViewModel, onClose = { navController.popBackStack() })
                    }
                }
            }
        }

    }

    // This is called when the app is already running and a push notification is tapped
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Make sure to update the activity's current intent

        val chatId = intent.getIntExtra("OPEN_CHAT_WITH_USER_ID", -1)
        if (chatId != -1) {
            // Trigger the LaunchedEffect inside Compose to perform the navigation
            openChatUserIdByPush = chatId
        }
    }
}