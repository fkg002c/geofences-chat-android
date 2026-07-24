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
import com.ruinkogr.chatapp.ui.search.SearchScreen
import com.ruinkogr.chatapp.ui.search.SearchViewModel
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

    // Состояние для отслеживания кликов по пушам в фореграунде/бэкграунде (горячий старт)
    private var openChatUserIdByPush by mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Проверяем интент при холодном старте приложения
        val initialChatId = intent?.getIntExtra("OPEN_CHAT_WITH_USER_ID", -1)?.takeIf { it != -1 }

        setContent {
            ChatAppTheme {
                val navController = rememberNavController()

                // Определяем стартовый экран ОДИН РАЗ при создании активности
                val startScreen = remember {
                    val storage = tokenStorage as EncryptedPrefsTokenStorage
                    val currentUserId = storage.getCurrentUserIdSync()
                    when {
                        currentUserId == -1 -> "login_screen"
                        initialChatId != null -> "chat_screen/$initialChatId" // Сразу открываем чат
                        else -> "users_list"
                    }
                }

                // Слушаем новые клики по пушам (горячий старт через onNewIntent)
                openChatUserIdByPush?.let { chatId ->
                    LaunchedEffect(chatId) {
                        navController.navigate("chat_screen/$chatId") {
                            // Очищаем стек до списка пользователей, чтобы не плодить экраны
                            popUpTo("users_list") { saveState = true }
                            launchSingleTop = true
                        }
                        openChatUserIdByPush = null // Сбрасываем состояние после перехода
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
                        // 1. Оборачиваем навигационные действия в remember
                        val onUserClickRemembered = remember(navController) {
                            { selectedUserId: Int -> navController.navigate("chat_screen/$selectedUserId") }
                        }
                        val onSettingsClickRemembered = remember(navController) {
                            { navController.navigate("settings_screen") }
                        }
                        val onSearchClickRemembered = remember(navController) {
                            { navController.navigate("manual_search_screen") }
                        }
                        val onLogoutSuccessRemembered = remember(navController) {
                            {
                                Log.d("Navigation", "Redirecting to Login screen...onLogoutSuccessRemembered")
                                navController.navigate("login_screen") {
                                    popUpTo("users_list") { inclusive = true }
                                }
                            }
                        }

                        UsersScreen(
                            usersViewModel = usersViewModel,
                            authViewModel = authViewModel,
                            onUserClick = onUserClickRemembered,
                            onSettingsClick = onSettingsClickRemembered,
                            onSearchClick = onSearchClickRemembered,
                            onLogoutSuccess = onLogoutSuccessRemembered
                        )
                    }

                    // Chat screen
                    composable("chat_screen/{chatWithUserId}") { backStackEntry ->
                        val chatViewModel: ChatViewModel = hiltViewModel()

                        // Перехватываем системную кнопку "Назад" на телефоне.
                        // Если этот чат открылся как стартовый экран, кнопка "Назад" закроет приложение.
                        // Нам нужно принудительно перенаправить пользователя на список.
                        androidx.activity.compose.BackHandler {
                            // Проверяем, есть ли в стек-треке экран списка пользователей
                            val hasUsersListInStack = navController.previousBackStackEntry != null
                            if (hasUsersListInStack) {
                                navController.popBackStack()
                            } else {
                                // Если списка в стеке нет (был холодный старт сразу в чат),
                                // мы очищаем стек и открываем список пользователей вручную
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
                    // ManualSearchScreen
                    composable("manual_search_screen") {
                        val searchViewModel: SearchViewModel = hiltViewModel()
                        SearchScreen(searchViewModel, onClose = { navController.popBackStack() })
                    }

                }
            }
        }

    }

    // Этот метод вызывается, если приложение уже запущено, и мы кликаем по пушу
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Обязательно обновляем текущий интент активности

        val chatId = intent.getIntExtra("OPEN_CHAT_WITH_USER_ID", -1)
        if (chatId != -1) {
            // Триггерим LaunchedEffect внутри Compose для выполнения перехода
            openChatUserIdByPush = chatId
        }
    }
}