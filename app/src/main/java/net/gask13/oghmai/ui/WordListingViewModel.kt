package net.gask13.oghmai.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.WordItem
import net.gask13.oghmai.model.WordStatus
import net.gask13.oghmai.network.RetrofitInstance

class WordListingViewModel(application: Application) : AndroidViewModel(application) {
    private val _words = MutableStateFlow<List<WordItem>>(emptyList())
    val words: StateFlow<List<WordItem>> = _words

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedStatuses = MutableStateFlow<Set<WordStatus>>(emptySet())
    val selectedStatuses: StateFlow<Set<WordStatus>> = _selectedStatuses

    private val _failedLastTest = MutableStateFlow(false)
    val failedLastTest: StateFlow<Boolean> = _failedLastTest

    init {
        fetchWords()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedStatuses(statuses: Set<WordStatus>) {
        _selectedStatuses.value = statuses
    }

    fun updateFailedLastTest(failed: Boolean) {
        _failedLastTest.value = failed
    }

    fun applyFilters() {
        fetchWords()
    }

    private fun fetchWords() {
        viewModelScope.launch {
            _isLoading.value = true

            // Prepare status filter
            val statusFilter = if (_selectedStatuses.value.isNotEmpty()) {
                _selectedStatuses.value.joinToString(",") { it.name }
            } else {
                null
            }

            // Prepare failed last test filter
            val failedLastTestFilter = if (_failedLastTest.value) {
                true
            } else {
                null
            }

            // Prepare search query filter
            val containsFilter = if (_searchQuery.value.isNotEmpty()) {
                _searchQuery.value
            } else {
                null
            }

            val response = RetrofitInstance.apiService.getWords(
                status = statusFilter,
                failedLastTest = failedLastTestFilter,
                contains = containsFilter
            )
            _words.value = response.words
            _isLoading.value = false
        }
    }

    fun deleteWord(word: WordItem) {
        viewModelScope.launch {
            try {
                RetrofitInstance.apiService.deleteWord(word.word)
                // Update the list locally after successful deletion
                _words.value = _words.value.filter { it != word }
            } catch (e: Exception) {
                Log.e("WordListingViewModel", "Error deleting word: ${e.message}")
            }
        }
    }

    fun undoDeleteWord(word: WordItem, position: Int) {
        viewModelScope.launch {
            try {
                RetrofitInstance.apiService.undoDeleteWord(word.word)
                // Re-add the word to the list
                val currentList = _words.value.toMutableList()
                currentList.add(position, word)
                _words.value = currentList
            } catch (e: Exception) {
                Log.e("WordListingViewModel", "Error undoing delete for word: ${e.message}")
            }
        }
    }
}
