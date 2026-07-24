package com.ruinkogr.chatapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MvvmSearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    // Сохраняем тот же класс состояния SearchState для удобства UI
    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    // Ссылка на текущую корутину поиска для её отмены при новом вводе
    private var searchJob: Job? = null

    // Публичный метод, который View будет вызывать напрямую
    fun onQueryChanged(newQuery: String) {
        // Мгновенно обновляем текст в поле ввода и включаем загрузку
        _uiState.update { it.copy(query = newQuery, isLoading = true, errorMessage = null) }

        // Отменяем предыдущий фоновый поиск, если он еще не завершился
        searchJob?.cancel()

        // Запускаем новый поиск
        searchJob = viewModelScope.launch {
            try {
                val data = searchRepository.performSearch(newQuery)
                // Успех: обновляем состояние напрямую
                _uiState.update { it.copy(results = data, isLoading = false) }
            } catch (e: Exception) {
                // Ошибка: фиксируем текст ошибки напрямую
                _uiState.update {
                    it.copy(
                        results = emptyList(),
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Unknown Error"
                    )
                }
            }
        }
    }

    fun onClearSearch() {
        searchJob?.cancel()
        _uiState.update { SearchState() } // Сбрасываем в начальное состояние
    }
}
