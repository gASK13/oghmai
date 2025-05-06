package net.gask13.oghmai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Composable that displays a row of dots representing test results.
 * Green dots represent correct answers, red dots represent incorrect answers.
 *
 * @param testResults List of boolean values representing test results (true = correct, false = incorrect)
 * @param modifier Modifier to be applied to the component
 * @param dotSize Size of each dot in dp (default: 12.dp)
 * @param dotSpacing Spacing between dots in dp (default: 4.dp)
 */
@Composable
fun TestResultDots(
    testResults: List<Boolean>,
    modifier: Modifier = Modifier,
    dotSize: Int = 12,
    dotSpacing: Int = 4
) {
    Row(modifier = modifier) {
        testResults.forEach { result ->
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .padding(end = dotSpacing.dp)
                    .background(
                        color = if (result) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}