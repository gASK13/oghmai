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

enum class WordActionEnum {
    UNDELETE,
    RESET
}

enum class WordTypeEnum {
    NOUN,
    VERB,
    PRONOUN,
    ADJECTIVE,
    OTHER // Catch-all for any word type that doesn't fit into the above categories
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

data class WordDefinition(
    val translation: String,
    val definition: String,
    val examples: List<String>,
    val type: WordTypeEnum
)

data class WordResult(
    val word: String,
    val meanings: List<WordDefinition>,
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

data class ExplanationResponse(
    val word: String,
    val type: WordTypeEnum,
    val explanations: Map<String, List<String>>
)