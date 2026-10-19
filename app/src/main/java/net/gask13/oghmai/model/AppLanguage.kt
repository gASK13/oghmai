package net.gask13.oghmai.model

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val locale: Locale,
    val speechTag: String,
    val isoLanguages: Set<String>,
    val testPhrases: List<String>
) {
    ITALIAN(
        code = "IT",
        displayName = "Italian",
        locale = Locale.ITALIAN,
        speechTag = "it-IT",
        isoLanguages = setOf("it", "ita"),
        testPhrases = listOf(
            "Mamma mia!",
            "Squadra azzurra",
            "Bella Italia",
            "Dolce vita",
            "Arrivederci",
            "Che bella giornata!",
            "Ciao bella!",
            "Molto bene"
        )
    ),
    GERMAN(
        code = "DE",
        displayName = "German",
        locale = Locale.GERMAN,
        speechTag = "de-DE",
        isoLanguages = setOf("de", "deu", "ger"),
        testPhrases = listOf(
            "Guten Tag!",
            "Wie geht's?",
            "Alles klar!",
            "Auf Wiedersehen!",
            "Das ist wunderbar!",
            "Vielen Dank!",
            "Schönen Tag noch!"
        )
    ),
    GREEK(
        code = "EL",
        displayName = "Greek",
        locale = Locale("el", "GR"),
        speechTag = "el-GR",
        isoLanguages = setOf("el", "ell", "gre"),
        testPhrases = listOf(
            "Καλημέρα!",
            "Τι κάνεις;",
            "Όλα καλά!",
            "Ευχαριστώ πολύ!",
            "Τα λέμε!",
            "Γεια σου!",
            "Όμορφη μέρα!"
        )
    );

    companion object {
        val DEFAULT = ITALIAN

        fun fromCode(value: String?): AppLanguage {
            if (value.isNullOrBlank()) return DEFAULT
            val trimmed = value.trim()
            return entries.firstOrNull {
                it.code.equals(trimmed, ignoreCase = true) ||
                it.displayName.equals(trimmed, ignoreCase = true)
            } ?: DEFAULT
        }
    }
}
