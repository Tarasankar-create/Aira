package com.example.aira

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.aira.ui.AiraViewModel
import com.example.aira.ui.navigation.AiraApp
import com.example.aira.ui.theme.AiraTheme
import com.example.aira.ui.theme.BackgroundDark

class MainActivity : ComponentActivity() {

    private val viewModel: AiraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AiraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    val requiredPermissions = buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        add(Manifest.permission.READ_CONTACTS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
                        val notifGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                (permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false)

                        android.util.Log.d(
                            "MainActivity",
                            "Permissions result - Audio: $audioGranted, Notification: $notifGranted"
                        )
                    }

                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(requiredPermissions)
                    }

                    AiraApp(
                        viewModel = viewModel,
                        onRequestPermissions = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    )
                }
            }
        }
    }
}
