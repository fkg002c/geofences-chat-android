package com.ruinkogr.chatapp.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

@Composable
fun SettingsScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Выберите язык / Select Language", modifier = Modifier.padding(bottom = 8.dp))

        Row {
            Button(onClick = { changeLanguage("ru") }) {
                Text("Русский")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { changeLanguage("en") }) {
                Text("English")
            }
        }
    }
}

fun changeLanguage(languageCode: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}