package com.app001.virtualcamera.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    
    object Preview : Screen(
        route = "preview",
        title = "Preview",
        icon = Icons.Default.PlayArrow
    )
    
    object Advanced : Screen(
        route = "advanced",
        title = "Advanced",
        icon = Icons.Default.Build
    )
    
    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )
    
    object VirtualCameraHack : Screen(
        route = "virtual_camera_hack",
        title = "Virtual Camera Hack",
        icon = Icons.Default.Security
    )
    
    object GhostCam : Screen(
        route = "ghostcam",
        title = "GhostCam Style",
        icon = Icons.Default.Videocam
    )
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Preview,
    Screen.Advanced,
    Screen.Settings
)
