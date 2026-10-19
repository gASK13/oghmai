package net.gask13.oghmai.model

import androidx.annotation.DrawableRes
import net.gask13.oghmai.R
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val locale: Locale,
    val speechTag: String,
    val isoLanguages: Set<String>,
    val testPhrases: List<String>,
    @DrawableRes val flagRes: Int,
    val joke: String
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
        ),
        flagRes = R.drawable.ic_flag_it,
        joke = "Chi dorme non piglia pesci... ma almeno non si bagna."
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
        ),
        flagRes = R.drawable.ic_flag_de,
        joke = "Was ist grün und klopft an die Tür? Ein Klopfsalat."
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
        ),
        flagRes = R.drawable.ic_flag_el,
        joke = "Η καλή μέρα από το πρωί φαίνεται... αλλά το καλό μεσημεράκι θέλει ύπνο."
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
