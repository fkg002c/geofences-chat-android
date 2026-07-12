package com.ruinkogr.chatapp.ui.settings

import com.ruinkogr.chatapp.R

data class AppLanguage(val code: String, val nameRes: Int)

val supportedLanguages = listOf(
    AppLanguage("", R.string.system_language),
    AppLanguage("ru", R.string.russian_language),
    AppLanguage("en", R.string.english_language)
)
