package com.ruinkogr.chatapp.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MvvmSearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchState())
    val uiState: StateFlow<SearchState> = _uiState.asStateFlow()

    // Труба (поток), куда мы будем закидывать поисковые запросы пользователя
    private val queryFlow = MutableSharedFlow<String>()

    init {
        // Настраиваем конвейер обработки ввода в блоке инициализации
        queryFlow
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                flow {
                    val data = searchRepository.performSearch(query)
                    emit(Result.success(data))
                }
                    .catch { exception ->
                        emit(Result.failure(exception))
                    }
            }
            .onEach { result ->
                result.fold(
                    onSuccess = { data ->
                        // Успешно получили данные
                        _uiState.update {
                            it.copy(results = data, isLoading = false, errorMessage = null)
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                results = emptyList(),
                                isLoading = false,
                                errorMessage = exception.localizedMessage ?: "Сетевая ошибка"
                            )
                        }
                    }
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            queryFlow.emit(newQuery)
        }
    }

    fun onClearSearch() {
        _uiState.update { SearchState() }
        viewModelScope.launch { queryFlow.emit("") }
    }
}
