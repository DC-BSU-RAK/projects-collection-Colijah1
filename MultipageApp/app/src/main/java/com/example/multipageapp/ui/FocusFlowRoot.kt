package com.example.multipageapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.activity.ComponentActivity
import com.example.multipageapp.FocusFlowApplication
import com.example.multipageapp.ui.focus.FocusViewModel
import com.example.multipageapp.ui.home.HomeViewModel
import com.example.multipageapp.ui.insights.InsightsViewModel
import com.example.multipageapp.ui.settings.SettingsViewModel
import com.example.multipageapp.ui.screens.FocusScreen
import com.example.multipageapp.ui.screens.HomeScreen
import com.example.multipageapp.ui.screens.InsightsRoute
import com.example.multipageapp.ui.screens.SettingsRoute
import com.example.multipageapp.ui.theme.FocusFlowTheme

private data class BarItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val match: (String?) -> Boolean
)

@Composable
fun FocusFlowRoot(
    activity: ComponentActivity,
    app: FocusFlowApplication
) {
    val repository = app.repository
    val homeFactory = remember(activity) { FocusFlowViewModelFactory(activity, app) }
    val homeVm: HomeViewModel = viewModel(viewModelStoreOwner = activity, factory = homeFactory)
    val homeState by homeVm.uiState.collectAsStateWithLifecycle()
    val feedback by repository.sessionFeedback.collectAsStateWithLifecycle()

    FocusFlowTheme(homeState.preferences) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val route = backStack?.destination?.route

        feedback?.let { f ->
            AlertDialog(
                onDismissRequest = { repository.consumeSessionFeedback() },
                title = { Text("${f.scorePercent}% focus score", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column {
                        Text(f.headline, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(f.detail, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { repository.consumeSessionFeedback() }) {
                        Text("Continue")
                    }
                }
            )
        }

        val items = listOf(
            BarItem("home", "Home", Icons.Filled.Home) { it == "home" },
            BarItem("focus", "Focus", Icons.Filled.Timer) { r -> r?.startsWith("focus/") == true },
            BarItem("insights", "Insights", Icons.Outlined.AutoGraph) { it == "insights" },
            BarItem("settings", "Settings", Icons.Filled.Settings) { it == "settings" }
        )
        val selectedIndex = items.indexOfFirst { it.match(route) }.coerceAtLeast(0)

        Scaffold(
            bottomBar = {
                NavigationBar {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = {
                                when (item.route) {
                                    "focus" -> {
                                        val m = homeState.preferences.defaultSessionMinutes.coerceIn(5, 120)
                                        navController.navigate("focus/$m") {
                                            launchSingleTop = true
                                            restoreState = true
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                        }
                                    }
                                    else -> navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        state = homeState,
                        onStartFocus = { minutes ->
                            val m = minutes.coerceIn(5, 120)
                            navController.navigate("focus/$m") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = "focus/{minutes}",
                    arguments = listOf(
                        navArgument(FocusViewModel.MINUTES_ARG) {
                            type = NavType.IntType
                            defaultValue = FocusViewModel.DEFAULT_MINUTES
                        }
                    )
                ) { entry ->
                    val vm: FocusViewModel = viewModel(
                        viewModelStoreOwner = entry,
                        factory = FocusFlowViewModelFactory(entry, app)
                    )
                    FocusScreen(
                        vm = vm,
                        repository = repository,
                        onBack = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable("insights") { entry ->
                    val vm: InsightsViewModel = viewModel(
                        viewModelStoreOwner = entry,
                        factory = FocusFlowViewModelFactory(entry, app)
                    )
                    InsightsRoute(vm)
                }
                composable("settings") { entry ->
                    val vm: SettingsViewModel = viewModel(
                        viewModelStoreOwner = entry,
                        factory = FocusFlowViewModelFactory(entry, app)
                    )
                    SettingsRoute(vm)
                }
            }
        }
    }
}
