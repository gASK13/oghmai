package net.gask13.oghmai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.gask13.oghmai.R

/**
 * A reusable TopBar component that can be customized for different screens.
 * 
 * @param title The title to display in the center of the TopBar
 * @param isMainScreen Whether this is the main screen (to show logo instead of a back button)
 * @param onBackClick Callback for when the back button is clicked
 * @param showOptionsMenu Whether to show the options menu (3 dots)
 * @param optionsMenuItems List of menu items to show in the dropdown menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    isMainScreen: Boolean = false,
    onBackClick: () -> Unit = {},
    showOptionsMenu: Boolean = false,
    optionsMenuItems: List<OptionMenuItem> = emptyList()
) {
    var showDropdown by remember { mutableStateOf(false) }

    TopAppBar(
        title = { 
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    textAlign = TextAlign.Center
                )
            }
        },
        navigationIcon = {
            if (isMainScreen) {
                // Show logo for the main screen
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(
                            id = R.drawable.ic_oghmai_foreground
                        ),
                        contentDescription = "App Logo"
                    )
                }
            } else {
                // Show back button for other screens
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            Box(
                modifier = Modifier.wrapContentSize(Alignment.TopEnd)
            ) {
                IconButton(onClick = { showDropdown = true }, enabled = showOptionsMenu && optionsMenuItems.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.alpha(if (showOptionsMenu && optionsMenuItems.isNotEmpty()) 1f else 0f)
                    )
                }

                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    optionsMenuItems.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.text) },
                            onClick = {
                                showDropdown = false
                                item.onClick()
                            },
                            leadingIcon = item.icon?.let { icon ->
                                { Icon(imageVector = icon, contentDescription = null) }
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.height(40.dp), // Make the TopBar narrower
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // Change background color
        )
    )
}

/**
 * A reusable scaffold with a TopBar component that can be used across all screens.
 * 
 * @param title The title to display in the center of the TopBar
 * @param isMainScreen Whether this is the main screen (to show logo instead of the back button)
 * @param onBackClick Callback for when the back button is clicked
 * @param showOptionsMenu Whether to show the options menu (3 dots)
 * @param optionsMenuItems List of menu items to show in the dropdown menu
 * @param snackbarHostState The SnackbarHostState to use for displaying snackbars
 * @param content The content to display in the scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWithTopBar(
    title: String,
    isMainScreen: Boolean = false,
    onBackClick: () -> Unit = {},
    showOptionsMenu: Boolean = false,
    optionsMenuItems: List<OptionMenuItem> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopBar(
                title = title,
                isMainScreen = isMainScreen,
                onBackClick = onBackClick,
                showOptionsMenu = showOptionsMenu,
                optionsMenuItems = optionsMenuItems
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        content(paddingValues)
    }
}

/**
 * Data class representing an item in the options menu.
 */
data class OptionMenuItem(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val onClick: () -> Unit
)
