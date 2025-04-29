package net.gask13.oghmai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.gask13.oghmai.model.WordStatus

@Composable
fun WordStatusBadge(wordStatus: WordStatus, modifier: Modifier = Modifier) {
    val (icon, color) = getStatusIconAndColor(wordStatus)
    val label = when (wordStatus) {
        WordStatus.NEW -> "New"
        WordStatus.KNOWN -> "Known"
        WordStatus.LEARNED -> "Learned"
        WordStatus.MASTERED -> "Mastered"
        WordStatus.UNSAVED -> "???"
    }

    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 1f),
                shape = RoundedCornerShape(32.dp) // Changed shape to medium for button-like appearance
            )
            .padding(horizontal = 16.dp, vertical = 12.dp), // Adjusted padding for button size
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface, // Changed to default text color
            modifier = Modifier.size(20.dp) // Slightly increased icon size for better visibility
        )
        Spacer(modifier = Modifier.width(8.dp)) // Adjusted spacing for button-like layout
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium, // Adjusted typography for better readability
            color = MaterialTheme.colorScheme.onSurface // Changed to default text color
        )
    }
}
