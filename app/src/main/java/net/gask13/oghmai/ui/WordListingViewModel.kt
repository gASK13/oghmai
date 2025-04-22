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
import net.gask13.oghmai.network.RetrofitInstance

class WordListingViewModel(application: Application) : AndroidViewModel(application) {
    private val _words = MutableStateFlow<List<String>>(emptyList())
    val words: StateFlow<List<String>> = _words

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchWords()
    }

    private fun fetchWords() {
        viewModelScope.launch {
            _isLoading.value = true
            val response = RetrofitInstance.apiService.getWords()
            _words.value = response.words
            _isLoading.value = false
        }
    }

    fun deleteWord(word: String) {
        viewModelScope.launch {
            try {
                RetrofitInstance.apiService.deleteWord(word)
                // Update the list locally after successful deletion
                _words.value = _words.value.filter { it != word }
            } catch (e: Exception) {
                Log.e("WordListingViewModel", "Error deleting word: ${e.message}")
            }
        }
    }

    fun undoDeleteWord(word: String, position: Int) {
        viewModelScope.launch {
            try {
                RetrofitInstance.apiService.undoDeleteWord(word)
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