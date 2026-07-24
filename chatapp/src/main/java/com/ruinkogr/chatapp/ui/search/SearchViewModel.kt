package com.ruinkogr.chatapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()
    private val intentFlow = MutableSharedFlow<SearchIntent>()

    init {
        viewModelScope.launch {
            intentFlow.collectLatest { intent ->
                handleIntent(intent)
            }
        }
    }

    fun sendIntent(intent: SearchIntent) {
        viewModelScope.launch {
            intentFlow.emit(intent)
        }
    }

    private suspend fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.ChangeQuery -> {
                triggerReduce(SearchMutation.QueryChanged(intent.text))
                triggerReduce(SearchMutation.Loading)

                try {
                    val data = searchRepository.performSearch(intent.text)
                    triggerReduce(SearchMutation.Success(data))
                } catch (e: Exception) {
                    triggerReduce(SearchMutation.Failure(e.localizedMessage ?: "Unknown Error"))
                }
            }

            is SearchIntent.ClearSearch -> {
                triggerReduce(SearchMutation.QueryChanged(""))
                triggerReduce(SearchMutation.Success(emptyList()))
            }
        }
    }

    private fun triggerReduce(mutation: SearchMutation) {
        val currentState = _uiState.value
        val newState = reduce(currentState, mutation)
        _uiState.value = newState
    }

    private fun reduce(state: SearchState, mutation: SearchMutation): SearchState {
        return when (mutation) {
            is SearchMutation.QueryChanged -> state.copy(
                query = mutation.text,
                errorMessage = null
            )

            is SearchMutation.Loading -> state.copy(
                isLoading = true,
                errorMessage = null
            )

            is SearchMutation.Success -> state.copy(
                isLoading = false,
                results = mutation.results
            )

            is SearchMutation.Failure -> state.copy(
                isLoading = false,
                results = emptyList(),
                errorMessage = mutation.message
            )
        }
    }
}
