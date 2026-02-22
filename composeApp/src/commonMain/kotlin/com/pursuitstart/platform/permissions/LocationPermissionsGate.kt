package com.pursuitstart.platform.permissions

import androidx.compose.runtime.Composable

/**
 * Calls content(hasPermission).
 * Android actual will request runtime permission as needed.
 */
@Composable
expect fun LocationPermissionGate(
    content: @Composable (hasPermission: Boolean) -> Unit
)