package net.gask13.oghmai.model

import java.sql.Timestamp

enum class WordStatus {
    UNSAVED,
    NEW,
    LEARNED,
    KNOWN,
    MASTERED
}
data class WordResult(
    val word: String,
    val translation: String,
    val definition: String,
    val examples: List<String>,
    val createdAt: Timestamp?,
    val lastTest: Timestamp?,
    val testResults: List<Boolean>,
    var status: WordStatus,
)

data class DescriptionRequest(
    val description: String,
    val exclusions: List<String>? = null
)

data class WordList(
    val words: List<String>
)