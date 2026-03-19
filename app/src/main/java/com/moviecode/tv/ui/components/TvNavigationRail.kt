package com.moviecode.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviecode.tv.ui.theme.*

enum class NavigationItem(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    MOVIES("Movies", Icons.Filled.Movie),
    TV_SHOWS("TV Shows", Icons.Filled.Tv),
    ANIME("Anime", Icons.Filled.VideoLibrary),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun TvNavigationRail(
    selectedItem: NavigationItem,
    onItemSelected: (NavigationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(200.dp),
        color = TvBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Text(
                text = "MovieCode",
                color = Primary,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Navigation items
            NavigationItem.entries.forEach { item ->
                NavigationItem(
                    item = item,
                    isSelected = item == selectedItem,
                    onSelect = { onItemSelected(item) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .focusable(),
        color = if (isSelected) Primary.copy(alpha = 0.2f) else TvBackground,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = if (isSelected) Primary else TextSecondary,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = item.title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 18.sp
            )
        }
    }
}
