package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AppDiscoveryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AppClonerTheme
import com.example.ui.viewmodel.AppClonerViewModel
import com.example.ui.viewmodel.AppDiscoveryViewModel
import kotlinx.coroutines.flow.collectLatest

enum class AppScreen {
    DASHBOARD,
    DISCOVERY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppClonerTheme {
                val context = LocalContext.current
                val clonerViewModel: AppClonerViewModel = viewModel()
                val discoveryViewModel: AppDiscoveryViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }

                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }

                // Observe toast notifications from ViewModels
                LaunchedEffect(Unit) {
                    clonerViewModel.toastEvents.collectLatest { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    discoveryViewModel.messages.collectLatest { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition",
                        modifier = Modifier.padding(innerPadding)
                    ) { screen ->
                        when (screen) {
                            AppScreen.DASHBOARD -> {
                                DashboardScreen(
                                    viewModel = clonerViewModel,
                                    onNavigateToDiscovery = { currentScreen = AppScreen.DISCOVERY },
                                    onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
                                )
                            }
                            AppScreen.DISCOVERY -> {
                                AppDiscoveryScreen(
                                    viewModel = discoveryViewModel,
                                    onNavigateBack = {
                                        currentScreen = AppScreen.DASHBOARD
                                        clonerViewModel.refreshStats()
                                    }
                                )
                            }
                            AppScreen.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = clonerViewModel,
                                    onNavigateBack = { currentScreen = AppScreen.DASHBOARD }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
