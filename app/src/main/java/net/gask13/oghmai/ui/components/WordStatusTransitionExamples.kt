package net.gask13.oghmai.ui.components

/**
 * Example usage of Word Status Transition Animations
 *
 * This file contains examples and documentation for the various status transition animations
 * available in the application.
 *
 * ## Available Components:
 *
 * ### 1. AnimatedWordStatusBadge
 * The primary component for showing word status with smooth transitions.
 *
 * Usage:
 * ```kotlin
 * var currentStatus by remember { mutableStateOf(WordStatus.NEW) }
 * var previousStatus by remember { mutableStateOf<WordStatus?>(null) }
 *
 * AnimatedWordStatusBadge(
 *     wordStatus = currentStatus,
 *     previousStatus = previousStatus,
 *     showCelebration = true  // Shows particles for improvements
 * )
 * ```
 *
 * Features:
 * - Automatically animates when wordStatus changes
 * - Shows celebratory particle effects for status improvements
 * - Smooth slide-in/slide-out transitions
 * - Works with all WordStatus types
 *
 * ### 2. StatusTransitionOverlay
 * A full-screen overlay that shows a more dramatic transition between statuses.
 *
 * Usage:
 * ```kotlin
 * var showOverlay by remember { mutableStateOf(true) }
 *
 * if (showOverlay) {
 *     StatusTransitionOverlay(
 *         oldStatus = WordStatus.LEARNED,
 *         newStatus = WordStatus.KNOWN,
 *         onAnimationComplete = { showOverlay = false }
 *     )
 * }
 * ```
 *
 * Features:
 * - Three-phase animation (fade out old, transition, fade in new)
 * - Celebratory effects for improvements
 * - Bouncy scale animation for the new status
 * - Auto-dismisses after completion
 *
 * ### 3. CompactStatusTransition
 * A compact inline transition showing old → new status.
 *
 * Usage:
 * ```kotlin
 * CompactStatusTransition(
 *     oldStatus = WordStatus.NEW,
 *     newStatus = WordStatus.LEARNED
 * )
 * ```
 *
 * Features:
 * - Shows both statuses side-by-side with arrow
 * - Auto-dismisses after 2 seconds
 * - Emphasis on the new status
 * - Perfect for inline status changes
 *
 * ## Implementation in Screens:
 *
 * ### ChallengeScreen
 * Shows animated status after submitting a test answer:
 * - Uses AnimatedWordStatusBadge with celebration effects
 * - Transitions from oldStatus (from TestResult) to newStatus
 * - Smooth fade and slide animations
 *
 * ### WordDetailScreen
 * Shows animated status when resetting a word:
 * - Uses AnimatedWordStatusBadge without celebration (reset is not an improvement)
 * - Tracks previousStatus before reset operation
 * - Animates from old status to NEW
 *
 * ### WordDiscoveryScreen (via WordResultCard)
 * Shows animated status when saving a new word:
 * - Uses AnimatedWordStatusBadge with celebration effects
 * - Transitions from UNSAVED to NEW
 * - Tracks per-word status in previousStatusMap
 *
 * ## Animation Details:
 *
 * ### Transition Types:
 * 1. **Slide Animation**: Vertical slide out (old) and slide in (new)
 * 2. **Fade Animation**: Smooth opacity transitions
 * 3. **Scale Animation**: Bouncy scale effect for new status
 * 4. **Celebration Effect**: Particle burst with rotating star
 *
 * ### Timing:
 * - Transition duration: 800ms
 * - Fade in/out: 400ms
 * - Celebration particles: 1000ms
 * - Star rotation: 2000ms loop
 *
 * ### Status Hierarchy (for determining improvements):
 * 1. UNSAVED (lowest)
 * 2. NEW
 * 3. LEARNED
 * 4. KNOWN
 * 5. MASTERED (highest)
 *
 * An improvement is any transition that moves up this hierarchy.
 *
 * ## Customization:
 *
 * To modify animation behavior:
 * - Adjust timing constants in WordStatusTransition.kt
 * - Modify celebration particle count and distribution
 * - Change animation specs (easing, spring damping, etc.)
 * - Customize colors and effects in CelebrationEffect()
 *
 * ## Performance Notes:
 *
 * - Animations use Compose's animation APIs for optimal performance
 * - LaunchedEffect ensures animations don't block the UI thread
 * - Remember and derivedStateOf minimize recompositions
 * - Particle effects are lightweight and GPU-accelerated
 */
