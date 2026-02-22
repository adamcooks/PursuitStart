package com.pursuitstart.platform.permissions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun LocationPermissionGate(
    content: @Composable (hasPermission: Boolean) -> Unit
) {
    val context = LocalContext.current

    fun hasFine(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasFine()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        granted = isGranted
    }

    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    content(granted)
}