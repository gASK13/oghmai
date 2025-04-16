package net.gask13.oghmai.model

data class WordResult(
    val word: String,
    val translation: String,
    val definition: String,
    val examples: List<String>
)

data class DescriptionRequest(
    val description: String,
    val exclusions: List<String>? = null
)

data class WordList(
    val words: List<String>
)