package com.ruinkogr.chatapp.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    viewModel: UsersViewModel,
    onUserClick: (Int) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    val usersState by viewModel.usersState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Contacts") }) }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (val state = usersState) {
                is UsersUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UsersUiState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.users) { user ->
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
