package net.gask13.oghmai.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.gask13.oghmai.model.MatchChallenge
import net.gask13.oghmai.model.WordTranslationPair
import net.gask13.oghmai.network.RetrofitInstance
import net.gask13.oghmai.ui.components.ScaffoldWithTopBar
import net.gask13.oghmai.util.SnackbarManager
import net.gask13.oghmai.util.SoundEffectsManager

// Data class to represent a card in the match game
data class MatchCard(
    val id: String,
    val text: String,
    val isWord: Boolean,
    val pairId: String,        // Unique identifier for the specific pair this card belongs to
    var isMatched: Boolean = false,
    var isSelected: Boolean = false,
    var isVisible: Boolean = true,
    var isFadingOut: Boolean = false,
    var isFadingIn: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchChallengeScreen(navController: NavController, soundEffectsManager: SoundEffectsManager, textToSpeech: net.gask13.oghmai.services.TextToSpeechWrapper) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Game state
    var isLoading by remember { mutableStateOf(true) }
    var matchChallenge by remember { mutableStateOf<MatchChallenge?>(null) }
    var gameCards by remember { mutableStateOf<List<MatchCard>>(emptyList()) }
    var selectedCard by remember { mutableStateOf<MatchCard?>(null) }
    var matchCount by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var gameWon by remember { mutableStateOf(false) }
    var remainingPairs by remember { mutableStateOf(0) }
    var matchedPairsSinceLastAdd by remember { mutableStateOf(0) }

    // Timer state
    val gameDurationSeconds = 60 // 1 minute timer
    var remainingTimeSeconds by remember { mutableStateOf(gameDurationSeconds) }
    val timerProgress = animateFloatAsState(
        targetValue = remainingTimeSeconds.toFloat() / gameDurationSeconds.toFloat(),
        animationSpec = tween(durationMillis = 1000)
    )

    // Start timer when game starts
    LaunchedEffect(matchChallenge) {
        if (matchChallenge != null) {
            while (remainingTimeSeconds > 0 && !gameOver) {
                delay(1000)
                remainingTimeSeconds--

                // Check if time's up
                if (remainingTimeSeconds <= 0) {
                    gameOver = true
                }
            }
        }
    }

    // Load match challenge
    LaunchedEffect(true) {
        loadMatchChallenge(coroutineScope, snackbarHostState) { challenge ->
            matchChallenge = challenge
            if (challenge != null) {
                // Initialize game with first 6 pairs (or fewer if not enough)
                val initialPairs = challenge.pairs.take(6)
                gameCards = createGameCards(initialPairs)
                remainingPairs = challenge.pairs.size - initialPairs.size
            } else {
                // If data failed to load, go back to menu
                navController.navigate("main") {
                    popUpTo("main") { inclusive = false }
                }
            }
            isLoading = false
        }
    }

    // Function to handle card selection
    fun onCardSelected(card: MatchCard) {
        // Only prevent selection when game is over or card is invisible
        if (gameOver || !card.isVisible) return

        // If card is already selected, deselect it
        if (card.isSelected) {
            gameCards = gameCards.map {
                if (it.id == card.id) it.copy(isSelected = false) else it
            }
            selectedCard = null
            return
        }

        // If there's no current selection, start fresh
        if (selectedCard == null) {
            // Deselect any previously selected cards (cleanup from fast clicking)
            gameCards = gameCards.map { it.copy(isSelected = false) }

            // Select the new card
            gameCards = gameCards.map {
                if (it.id == card.id) it.copy(isSelected = true) else it
            }
            selectedCard = card

            // Read Italian word aloud when selected
            if (card.isWord) {
                textToSpeech.speak(card.text, "match_card_${card.id}")
            }
            return
        }

        // There's already a selected card - handle second selection
        val firstCard = selectedCard!!

        // If selecting another card of the same type, replace the previous selection
        if (firstCard.isWord == card.isWord) {
            gameCards = gameCards.map {
                when {
                    it.id == firstCard.id -> it.copy(isSelected = false)
                    it.id == card.id -> it.copy(isSelected = true)
                    else -> it
                }
            }
            selectedCard = card

            // Read Italian word aloud when selected
            if (card.isWord) {
                textToSpeech.speak(card.text, "match_card_${card.id}")
            }
            return
        }

        // Two cards of different types selected - check for match
        // Check if cards match by looking up valid pairs in the challenge
        val isMatch = if (firstCard.isWord != card.isWord && matchChallenge != null) {
            val wordText = if (firstCard.isWord) firstCard.text else card.text
            val translationText = if (firstCard.isWord) card.text else firstCard.text

            // Check if this word-translation pair exists in the challenge
            matchChallenge!!.pairs.any { pair ->
                pair.word == wordText && pair.translation == translationText
            }
        } else {
            false
        }

        // Mark the second card as selected
        gameCards = gameCards.map {
            if (it.id == card.id) it.copy(isSelected = true) else it
        }

        // Read Italian word aloud when selected
        if (card.isWord) {
            textToSpeech.speak(card.text, "match_card_${card.id}")
        }

        // Immediately clear the selection state to allow new selections
        selectedCard = null

        if (isMatch) {

            // Cards match - mark them as matched and start fade-out animation
            gameCards = gameCards.map {
                when (it.id) {
                    firstCard.id, card.id -> it.copy(isMatched = true, isSelected = false, isFadingOut = true)
                    else -> it
                }
            }

            // Increment match count and matched pairs since last add
            matchCount++
            matchedPairsSinceLastAdd++

            // Launch coroutine to handle fadeout and card management asynchronously
            coroutineScope.launch {
                // After animation completes, make cards invisible
                delay(500) // Wait for fade-out animation to complete
                gameCards = gameCards.map {
                    when (it.id) {
                        firstCard.id, card.id -> it.copy(isVisible = false, isFadingOut = false)
                        else -> it
                    }
                }

                // Add new cards if there are remaining pairs and we've matched 3 pairs
                if (remainingPairs > 0 && matchedPairsSinceLastAdd >= 3) {
                    // Reset counter
                    matchedPairsSinceLastAdd = 0

                    // Add new pairs (up to 3 or what's remaining)
                    val newPairsCount = minOf(3, remainingPairs)
                    val startIndex = matchChallenge!!.pairs.size - remainingPairs
                    val newPairs = matchChallenge!!.pairs.subList(startIndex, startIndex + newPairsCount)

                    // Create new cards with fade-in animation
                    val newCards = createGameCards(newPairs)

                    // Replace invisible cards with new ones in their positions
                    val mutableCards = gameCards.toMutableList()
                    var newCardIndex = 0

                    for (i in mutableCards.indices) {
                        if (!mutableCards[i].isVisible && newCardIndex < newCards.size) {
                            mutableCards[i] = newCards[newCardIndex]
                            newCardIndex++
                        }
                    }

                    gameCards = mutableCards
                    remainingPairs -= newPairsCount
                }

                // Check if all visible pairs are matched
                if ((gameCards.count { it.isVisible && !it.isMatched } == 0) && remainingPairs == 0) {
                    gameOver = true
                    gameWon = true
                }
            }
        } else {
            // Launch coroutine to handle the incorrect match delay asynchronously
            coroutineScope.launch {
                // Cards don't match - briefly show them, then deselect
                delay(300) // Reduced delay for faster feedback
                gameCards = gameCards.map {
                    if (it.id == firstCard.id || it.id == card.id) {
                        it.copy(isSelected = false)
                    } else {
                        it
                    }
                }
            }
        }
    }

    // Function to restart the game
    fun restartGame() {
        isLoading = true
        gameOver = false
        gameWon = false
        matchCount = 0
        matchedPairsSinceLastAdd = 0
        selectedCard = null
        remainingTimeSeconds = gameDurationSeconds

        coroutineScope.launch {
            loadMatchChallenge(coroutineScope, snackbarHostState) { challenge ->
                matchChallenge = challenge
                if (challenge != null) {
                    val initialPairs = challenge.pairs.take(6)
                    gameCards = createGameCards(initialPairs)
                    remainingPairs = challenge.pairs.size - initialPairs.size
                }
                isLoading = false
            }
        }
    }

    ScaffoldWithTopBar(
        title = "Match Challenge",
        isMainScreen = false,
        onBackClick = { navController.navigateUp() },
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Loading state
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (gameOver) {
                // Game over screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (gameWon) "Congratulations!" else "Sorry, time ended!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (gameWon) Color(0xFF4CAF50) else Color(0xFFE57373)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (gameWon) 
                            "You matched all the words successfully!" 
                        else 
                            "You matched $matchCount pairs before time ran out.",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { restartGame() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Play Again")
                    }
                }
            } else {
                // Active game screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Timer bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(Color.LightGray)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(timerProgress.value)
                                .fillMaxHeight()
                                .background(
                                    when {
                                        remainingTimeSeconds > 30 -> Color(0xFF4CAF50) // Green
                                        remainingTimeSeconds > 10 -> Color(0xFFFFA726) // Orange
                                        else -> Color(0xFFE57373) // Red
                                    }
                                )
                        )
                        Text(
                            text = "$remainingTimeSeconds",
                            modifier = Modifier
                                .align(Alignment.Center),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Match count
                    Text(
                        text = "Matches: $matchCount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Game cards grid - show all cards including invisible ones to maintain layout
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = gameCards,
                            key = { card -> card.id }
                        ) { card ->
                            MatchCardItem(
                                card = card,
                                onCardClick = { onCardSelected(card) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCardItem(card: MatchCard, onCardClick: () -> Unit) {
    // State to track if this specific card instance is fading in
    var isFadingIn by remember { mutableStateOf(card.isFadingIn) }

    // Effect to reset fading in state after animation completes
    LaunchedEffect(card.id) {
        if (isFadingIn) {
            delay(500) // Match the animation duration
            isFadingIn = false
        }
    }

    // If card is invisible (not fading), render transparent placeholder
    if (!card.isVisible && !card.isFadingOut) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        return
    }

    val backgroundColor = when {
        card.isMatched -> Color(0xFFE8F5E9) // Light green for matched
        card.isSelected -> if (card.isWord) Color(0xFFE3F2FD) else Color(0xFFFFF3E0) // Light blue for words, light orange for translations
        else -> if (card.isWord) Color(0xFFBBDEFB) else Color(0xFFFFE0B2) // Darker blue for words, darker orange for translations
    }

    val borderColor = when {
        card.isMatched -> Color(0xFF4CAF50) // Green for matched
        card.isSelected -> Color(0xFF2196F3) // Blue for selected
        else -> Color.Gray
    }

    // Animation for fade-in and fade-out
    val alpha by animateFloatAsState(
        targetValue = when {
            card.isFadingOut -> 0f // Fade out when matched
            else -> 1f // Normal state or fade in
        },
        animationSpec = tween(
            durationMillis = 500,
            easing = LinearEasing
        ),
        label = "cardAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onCardClick() }
            .border(width = 2.dp, color = borderColor)
            .graphicsLayer(alpha = alpha),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = card.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

// Helper function to create game cards from word-translation pairs
private fun createGameCards(pairs: List<WordTranslationPair>): List<MatchCard> {
    val cards = mutableListOf<MatchCard>()

    pairs.forEachIndexed { index, pair ->
        // Generate a unique pair ID using index and a timestamp-like value to ensure uniqueness
        val pairId = "pair_${index}_${System.currentTimeMillis()}_${pair.word}_${pair.translation}"

        // Create word card with unique ID
        cards.add(
            MatchCard(
                id = "word_$pairId",
                text = pair.word,
                isWord = true,
                pairId = pairId,
                isFadingIn = true
            )
        )

        // Create translation card with unique ID
        cards.add(
            MatchCard(
                id = "translation_$pairId",
                text = pair.translation,
                isWord = false,
                pairId = pairId,
                isFadingIn = true
            )
        )
    }

    // Shuffle the cards
    return cards.shuffled()
}

// Function to load match challenge from API
private fun loadMatchChallenge(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onResult: (MatchChallenge?) -> Unit
) {
    coroutineScope.launch {
        try {
            val challenge = RetrofitInstance.apiService.getNextMatchTest()
            onResult(challenge)
        } catch (e: Exception) {
            SnackbarManager.showOperationError(
                snackbarHostState = snackbarHostState,
                operation = "load",
                exception = e
            )
            onResult(null)
        }
    }
}
