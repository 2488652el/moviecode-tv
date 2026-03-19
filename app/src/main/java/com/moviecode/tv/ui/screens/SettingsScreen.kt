package com.moviecode.tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviecode.tv.ui.components.TvNavigationRail
import com.moviecode.tv.ui.components.NavigationItem
import com.moviecode.tv.ui.theme.*
import com.moviecode.tv.data.repository.MediaScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val mediaScannerRepository: MediaScannerRepository
) : ViewModel() {
    
    var isScanning by mutableStateOf(false)
    var scanProgress by mutableStateOf("")
    
    fun scanMedia() {
        viewModelScope.launch {
            isScanning = true
            scanProgress = "Scanning..."
            val result = mediaScannerRepository.scanMediaLibrary()
            scanProgress = if (result.isSuccess) {
                val scanResult = result.getOrNull()
                "Found: ${scanResult?.movies?.size ?: 0} movies, " +
                "${scanResult?.tvShows?.size ?: 0} TV shows, " +
                "${scanResult?.anime?.size ?: 0} anime"
            } else {
                "Scan failed: ${result.exceptionOrNull()?.message}"
            }
            isScanning = false
        }
    }
}

@Composable
fun SettingsScreen(
    selectedNavItem: NavigationItem,
    onNavItemSelected: (NavigationItem) -> Unit,
    viewModel: SettingsViewModel
) {
    var nasHost by remember { mutableStateOf("") }
    var nasUsername by remember { mutableStateOf("") }
    var nasPassword by remember { mutableStateOf("") }
    var isNasConnected by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxSize().background(TvBackground)) {
        TvNavigationRail(
            selectedItem = selectedNavItem,
            onItemSelected = onNavItemSelected
        )
        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    color = TextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // NAS Connection Section
            item {
                SettingsSection(
                    title = "NAS Connection",
                    icon = Icons.Default.Cloud
                ) {
                    SettingsTextField(
                        value = nasHost,
                        onValueChange = { nasHost = it },
                        label = "IP Address",
                        placeholder = "192.168.1.100"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingsTextField(
                        value = nasUsername,
                        onValueChange = { nasUsername = it },
                        label = "Username"
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingsTextField(
                        value = nasPassword,
                        onValueChange = { nasPassword = it },
                        label = "Password",
                        isPassword = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { isNasConnected = true },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isNasConnected) Green else Primary)
                        ) {
                            Text(if (isNasConnected) "Connected" else "Connect")
                        }
                        
                        if (isNasConnected) {
                            OutlinedButton(
                                onClick = { isNasConnected = false }
                            ) {
                                Text("Disconnect")
                            }
                        }
                    }
                }
            }
            
            // Media Library Section
            item {
                SettingsSection(
                    title = "Media Library",
                    icon = Icons.Default.VideoLibrary
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Scan Media Files",
                                color = TextPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Scan local storage and NAS for media",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                        
                        Button(
                            onClick = { viewModel.scanMedia() },
                            enabled = !viewModel.isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (viewModel.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (viewModel.isScanning) "Scanning..." else "Scan")
                        }
                    }
                    
                    if (viewModel.scanProgress.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = viewModel.scanProgress,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Display Section
            item {
                SettingsSection(
                    title = "Display",
                    icon = Icons.Default.Tv
                ) {
                    var selectedResolution by remember { mutableStateOf("1080p") }
                    var selectedAspect by remember { mutableStateOf("Auto") }
                    
                    SettingsOption(
                        title = "Resolution",
                        options = listOf("720p", "1080p", "4K"),
                        selected = selectedResolution,
                        onSelect = { selectedResolution = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    SettingsOption(
                        title = "Aspect Ratio",
                        options = listOf("Auto", "16:9", "21:9"),
                        selected = selectedAspect,
                        onSelect = { selectedAspect = it }
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Default.Info
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Version", color = TextPrimary)
                        Text(text = "1.0.0", color = TextSecondary)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Build", color = TextPrimary)
                        Text(text = "2026.03.18", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            content()
        }
    }
}

@Composable
fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isPassword: Boolean = false
) {
    Column {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Divider,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusable(),
            visualTransformation = if (isPassword) {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            }
        )
    }
}

@Composable
fun SettingsOption(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(option) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = TextPrimary,
                        containerColor = CardBackgroundSelected,
                        labelColor = TextSecondary
                    )
                )
            }
        }
    }
}
