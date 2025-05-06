package net.gask13.oghmai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import net.gask13.oghmai.model.WordStatus

/**
 * Composable that displays an icon representing a word status.
 * This is a simplified version of WordStatusBadge without the text label.
 *
 * @param wordStatus The status of the word
 * @param modifier Modifier to be applied to the component
 * @param tint Color to tint the icon (default: white)
 */
@Composable
fun StatusIcon(
    wordStatus: WordStatus,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val (icon, _) = getStatusIconAndColor(wordStatus)
    
    Icon(
        imageVector = icon,
        contentDescription = wordStatus.name,
        tint = tint,
        modifier = modifier
    )
}

/**
 * Helper function to get the icon and color for a given word status.
 * This is extracted to be reusable between StatusIcon and WordStatusBadge.
 */
@Composable
fun getStatusIconAndColor(wordStatus: WordStatus): Pair<ImageVector, Color> {
    return when (wordStatus) {
        WordStatus.NEW -> Pair(Icons.Default.Star, Color(0xFF4CAF50))
        WordStatus.KNOWN -> Pair(Icons.Default.Check, Color(0xFF4CAF50))
        WordStatus.LEARNED -> Pair(Icons.Default.ThumbUp, Color(0xFF4CAF50))
        WordStatus.MASTERED -> Pair(Icons.Default.Lock, Color(0xFF4CAF50))
        WordStatus.UNSAVED -> Pair(Icons.Default.Info, MaterialTheme.colorScheme.primary)
    }
}