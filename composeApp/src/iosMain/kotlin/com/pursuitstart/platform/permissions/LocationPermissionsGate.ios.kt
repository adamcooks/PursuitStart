package com.pursuitstart.platform.permissions

import androidx.compose.runtime.Composable

@Composable
actual fun LocationPermissionGate(
    content: @Composable (hasPermission: Boolean) -> Unit
) {
    content(false)
}