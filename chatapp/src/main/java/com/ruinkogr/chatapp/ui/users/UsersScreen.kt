package com.ruinkogr.chatapp.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    usersViewModel: UsersViewModel,
    authViewModel: AuthViewModel,
    onUserClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutSuccess: () -> Unit
) {
    val usersState by usersViewModel.usersState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    // Logout listener
    val loginState by authViewModel.loginState.collectAsState()
    LaunchedEffect(Unit) {
        authViewModel.logoutEvent.collect {
            onLogoutSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options Menu"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Settings"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            }
                        )

                        HorizontalDivider() // Divider

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Logout",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                authViewModel.logout()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ServerStatusBanner()
            when (val state = usersState) {
                is UsersUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UsersUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.users,
                            key = { user -> user.id } // !!! key for navigation
                        ) { user ->
                            Text(
                                text = user.username,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserClick(user.id) }
                                    .padding(16.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }

                is UsersUiState.Error -> Text(text = state.message, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
