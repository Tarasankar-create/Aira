package com.example.aira.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.aira.ui.AiraViewModel
import com.example.aira.ui.conversation.ConversationScreen
import com.example.aira.ui.home.HomeScreen
import com.example.aira.ui.settings.SettingsScreen
import com.example.aira.ui.theme.AiraCyan
import com.example.aira.ui.theme.BackgroundDark
import com.example.aira.ui.theme.SurfaceDark
import com.example.aira.ui.theme.TextPrimary
import com.example.aira.ui.theme.TextSecondary

enum class AiraScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    HISTORY("History", Icons.Default.Forum),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AiraApp(
    viewModel: AiraViewModel,
    onRequestPermissions: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf(AiraScreen.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                AiraScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(text = screen.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = AiraCyan,
                            indicatorColor = AiraCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AiraScreen.HOME -> HomeScreen(
                viewModel = viewModel,
                onRequestPermissions = onRequestPermissions,
                modifier = Modifier.padding(innerPadding)
            )
            AiraScreen.HISTORY -> ConversationScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AiraScreen.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onRequestPermissions = onRequestPermissions,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
