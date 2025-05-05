package net.gask13.oghmai.model

import java.sql.Timestamp

enum class WordStatus {
    UNSAVED,
    NEW,
    LEARNED,
    KNOWN,
    MASTERED
}

enum class ResultEnum {
    INCORRECT,
    CORRECT,
    PARTIAL
}

data class TestChallenge(
    val description: String,
    val id: String
)

data class TestResult(
    val result: ResultEnum,
    val word: String,
    val suggestion: String,
    val newStatus: WordStatus,
    val oldStatus: WordStatus,
)

data class TestStatistics(
    val available: Map<WordStatus, Int>,
)
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

data class WordItem(
    val word: String,
    val testResults: List<Boolean>,
    var status: WordStatus,
)

data class DescriptionRequest(
    val description: String,
    val exclusions: List<String>? = null
)

data class WordList(
    val words: List<WordItem>
)