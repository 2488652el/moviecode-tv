package com.moviecode.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.moviecode.tv.domain.model.MediaItem
import com.moviecode.tv.ui.theme.*

@Composable
fun MediaPosterCard(
    item: MediaItem,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(160.dp)
            .focusable()
            .then(
                if (isSelected) Modifier.padding(horizontal = 8.dp)
                else Modifier
            ),
        colors = CardDefaults.colors(
            containerColor = if (isSelected) CardBackgroundSelected else CardBackground
        ),
        shape = CardDefaults.shape(
            shape = RoundedCornerShape(12.dp)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 16.dp else 4.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Transparent),
                                startY = 200f
                            )
                        )
                )
                
                // Rating badge
                if (item.rating > 0) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format("%.1f", item.rating),
                            color = Yellow,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Title
            Text(
                text = item.title,
                color = if (isSelected) TextPrimary else TextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .then(
                        if (isSelected) Modifier
                            .background(
                                color = Primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp)
                        else Modifier
                    )
            )
        }
    }
}

@Composable
fun MediaRow(
    title: String,
    items: List<MediaItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onItemClicked: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 16.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        
        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                val index = items.indexOf(item)
                MediaPosterCard(
                    item = item,
                    isSelected = index == selectedIndex,
                    onFocus = { onItemSelected(index) },
                    onClick = { onItemClicked(item) }
                )
            }
        }
    }
}
