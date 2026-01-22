package net.gask13.oghmai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.gask13.oghmai.model.WordStatus

/**
 * Animated word status badge that smoothly transitions between status changes.
 *
 * @param wordStatus Current status of the word
 * @param previousStatus Previous status (if null, no animation is shown)
 * @param modifier Modifier to be applied to the component
 * @param showCelebration Whether to show celebratory effects for level ups
 */
@Composable
fun AnimatedWordStatusBadge(
    wordStatus: WordStatus,
    previousStatus: WordStatus? = null,
    modifier: Modifier = Modifier,
    showCelebration: Boolean = true
) {
    var currentStatus by remember(wordStatus) { mutableStateOf(previousStatus ?: wordStatus) }
    var showTransition by remember(wordStatus) { mutableStateOf(previousStatus != null && previousStatus != wordStatus) }

    LaunchedEffect(wordStatus) {
        if (previousStatus != null && previousStatus != wordStatus) {
            showTransition = true
            delay(800) // Duration of transition
            currentStatus = wordStatus
            delay(200)
            showTransition = false
        } else {
            currentStatus = wordStatus
            showTransition = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Show celebration effects for improvements
        if (showTransition && showCelebration && isImprovement(previousStatus, wordStatus)) {
            CelebrationEffect()
        }

        // Animated transition between old and new status
        AnimatedContent(
            targetState = currentStatus,
            transitionSpec = {
                if (showTransition) {
                    // Slide out old, slide in new
                    (slideInVertically { height -> -height } + fadeIn(
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        slideOutVertically { height -> height } + fadeOut(
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        )
                    )
                } else {
                    // No animation for initial display
                    EnterTransition.None.togetherWith(ExitTransition.None)
                }
            },
            label = "statusTransition"
        ) { status ->
            WordStatusBadge(wordStatus = status)
        }
    }
}

/**
 * Full-screen status transition animation that shows both old and new status
 * with a clear visual transition. Useful for prominent status changes.
 *
 * @param oldStatus The previous word status
 * @param newStatus The new word status
 * @param onAnimationComplete Callback when animation completes
 */
@Composable
fun StatusTransitionOverlay(
    oldStatus: WordStatus,
    newStatus: WordStatus,
    onAnimationComplete: () -> Unit = {}
) {
    var animationPhase by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Phase 0: Show old status (0-500ms)
        delay(500)
        animationPhase = 1

        // Phase 1: Transition (500-1500ms)
        delay(1000)
        animationPhase = 2

        // Phase 2: Show new status (1500-2000ms)
        delay(500)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (animationPhase) {
            0 -> {
                // Show old status fading out
                val alpha by animateFloatAsState(
                    targetValue = 0f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "oldStatusAlpha"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(alpha)
                ) {
                    WordStatusBadge(wordStatus = oldStatus)
                }
            }
            1 -> {
                // Transition phase with arrow or celebration
                if (isImprovement(oldStatus, newStatus)) {
                    CelebrationEffect()
                }
            }
            2 -> {
                // Show new status fading in
                val alpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "newStatusAlpha"
                )

                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "newStatusScale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .alpha(alpha)
                        .scale(scale)
                ) {
                    WordStatusBadge(wordStatus = newStatus)
                }
            }
        }
    }
}

/**
 * Compact status transition that shows old -> new in a single row.
 * Useful for inline status changes.
 */
@Composable
fun CompactStatusTransition(
    oldStatus: WordStatus,
    newStatus: WordStatus,
    modifier: Modifier = Modifier
) {
    var showAnimation by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000) // Show for 2 seconds
        showAnimation = false
    }

    AnimatedVisibility(
        visible = showAnimation,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
    ) {
        Row(
            modifier = modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Old status with reduced opacity
            Box(modifier = Modifier.alpha(0.5f)) {
                WordStatusBadge(wordStatus = oldStatus)
            }

            // Arrow
            Text(
                text = "→",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // New status with emphasis
            val scale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "newBadgeScale"
            )

            Box(modifier = Modifier.scale(scale)) {
                WordStatusBadge(wordStatus = newStatus)
            }
        }
    }
}

/**
 * Celebration effect for status improvements.
 * Shows particles and scale effects.
 */
@Composable
private fun CelebrationEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")

    // Create multiple particle effects
    repeat(8) { index ->
        val angle = (360f / 8f) * index
        val distance by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particleDistance$index"
        )

        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particleAlpha$index"
        )

        Box(
            modifier = Modifier
                .offset(
                    x = (distance * kotlin.math.cos(Math.toRadians(angle.toDouble()))).dp,
                    y = (distance * kotlin.math.sin(Math.toRadians(angle.toDouble()))).dp
                )
                .size(8.dp)
                .alpha(alpha)
                .background(Color(0xFFFFD700), CircleShape) // Gold color
        )
    }

    // Central starburst
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starRotation"
    )

    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = "Celebration",
        tint = Color(0xFFFFD700),
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .graphicsLayer(rotationZ = rotation)
    )
}

/**
 * Helper function to determine if a status change is an improvement.
 */
private fun isImprovement(oldStatus: WordStatus?, newStatus: WordStatus): Boolean {
    if (oldStatus == null) return false

    val statusOrder = listOf(
        WordStatus.UNSAVED,
        WordStatus.NEW,
        WordStatus.LEARNED,
        WordStatus.KNOWN,
        WordStatus.MASTERED
    )

    val oldIndex = statusOrder.indexOf(oldStatus)
    val newIndex = statusOrder.indexOf(newStatus)

    return newIndex > oldIndex
}
