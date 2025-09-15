package com.app001.virtualcamera

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app001.virtualcamera.components.BottomNavigationBar
import com.app001.virtualcamera.navigation.Screen
import com.app001.virtualcamera.screens.AdvancedSetupScreen
import com.app001.virtualcamera.screens.HomeScreen
import com.app001.virtualcamera.screens.PreviewScreen
import com.app001.virtualcamera.screens.SettingsScreen
import com.app001.virtualcamera.ui.theme.VirtualCameraTheme
import com.app001.virtualcamera.utils.VideoPathManager

class MainActivity : ComponentActivity() {
    private val videoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        _selectedVideoUri.value = uri
    }

    private val _selectedVideoUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VideoPathManager.initialize(this)

        setContent {
            VirtualCameraTheme {
                MainScreen(
                    videoPickerLauncher = videoPickerLauncher,
                    selectedVideoUri = _selectedVideoUri.value
                )
            }
        }
    }



    @Composable
    fun MainScreen(
        videoPickerLauncher: ActivityResultLauncher<String>,
        selectedVideoUri: Uri?
    ) {
        val navController = rememberNavController()

        Scaffold(
            bottomBar = {
                BottomNavigationBar(navController = navController)
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToPreview = {
                            navController.navigate(Screen.Preview.route)
                        },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                composable(Screen.Preview.route) {
                    PreviewScreen(
                        videoPickerLauncher = videoPickerLauncher,
                        selectedVideoUri = selectedVideoUri
                    )
                }

                composable(Screen.Advanced.route) {
                    AdvancedSetupScreen(selectedVideoUri = selectedVideoUri)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen()
                }
            }
        }
    }

}

