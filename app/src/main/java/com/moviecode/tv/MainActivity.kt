package com.moviecode.tv

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moviecode.tv.ui.components.NavigationItem
import com.moviecode.tv.ui.screens.*
import com.moviecode.tv.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieCodeTVTheme {
                MovieCodeTVApp()
            }
        }
    }
}

@Composable
fun MovieCodeTVApp() {
    val navController = rememberNavController()
    var selectedNavItem by remember { mutableStateOf(NavigationItem.HOME) }
    
    Box(modifier = Modifier.fillMaxSize().background(TvBackground)) {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                selectedNavItem = NavigationItem.HOME
                HomeScreen(
                    onMediaSelected = { mediaItem ->
                        navController.navigate(
                            "detail/${mediaItem.tmdbId}/${mediaItem.type.name}"
                        )
                    },
                    selectedNavItem = selectedNavItem,
                    onNavItemSelected = { item ->
                        selectedNavItem = item
                        when (item) {
                            NavigationItem.MOVIES -> { /* Filter to movies */ }
                            NavigationItem.TV_SHOWS -> { /* Filter to TV */ }
                            NavigationItem.ANIME -> { /* Filter to anime */ }
                            NavigationItem.SETTINGS -> navController.navigate("settings")
                            else -> { }
                        }
                    }
                )
            }
            
            composable(
                route = "detail/{tmdbId}/{mediaType}"
            ) { backStackEntry ->
                val tmdbId = backStackEntry.arguments?.getString("tmdbId")?.toIntOrNull() ?: 0
                val mediaType = backStackEntry.arguments?.getString("mediaType") ?: "MOVIE"
                
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onPlay = { videoPath ->
                        navController.navigate("player/${java.net.URLEncoder.encode(videoPath, "UTF-8")}")
                    }
                )
            }
            
            composable(
                route = "player/{videoPath}"
            ) { backStackEntry ->
                val videoPath = java.net.URLDecoder.decode(
                    backStackEntry.arguments?.getString("videoPath") ?: "",
                    "UTF-8"
                )
                PlayerScreen(
                    videoPath = videoPath,
                    title = "Now Playing",
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("settings") {
                selectedNavItem = NavigationItem.SETTINGS
                SettingsScreen(
                    selectedNavItem = selectedNavItem,
                    onNavItemSelected = { item ->
                        selectedNavItem = item
                        when (item) {
                            NavigationItem.HOME -> navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                            else -> { }
                        }
                    },
                    viewModel = androidx.hilt.navigation.compose.hiltViewModel()
                )
            }
        }
    }
}
