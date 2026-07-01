package com.ruinkogr.chatapp.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ruinkogr.chatapp.data.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onSuccessRegister: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val registerState by viewModel.registerState.collectAsState()

    LaunchedEffect(registerState) {
        if (registerState is Resource.Success) {
            viewModel.resetRegisterState()
            onSuccessRegister() // Return to Login screen
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Register") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ChatApp registration", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("User name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (registerState is Resource.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.register(username.trim(), email.trim(), password.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
                ) {
                    Text("Register")
                }
            }

            if (registerState is Resource.Error) {
                Text(
                    text = (registerState as Resource.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Registered already? Log in",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }
    }
}
