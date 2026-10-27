package net.gask13.oghmai.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import net.gask13.oghmai.model.AppLanguage

private val GrayscaleFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
fun LanguageFlagRow(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == selected
            Image(
                painter = painterResource(lang.flagRes),
                contentDescription = lang.displayName,
                contentScale = ContentScale.Crop,
                colorFilter = if (isSelected) null else GrayscaleFilter,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(3f / 2f)
                    .clip(shape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = shape
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(lang) }
                    )
            )
        }
    }
}
