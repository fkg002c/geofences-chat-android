package com.ruinkogr.chatapp.ui.search

data class SearchState(
    val query: String = "",
    val results: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class SearchIntent {
    data class ChangeQuery(val text: String) : SearchIntent()
    object ClearSearch : SearchIntent()
}

sealed class SearchMutation {
    data class QueryChanged(val text: String) : SearchMutation()
    object Loading : SearchMutation()
    data class Success(val results: List<String>) : SearchMutation()
    data class Failure(val message: String) : SearchMutation()
}
