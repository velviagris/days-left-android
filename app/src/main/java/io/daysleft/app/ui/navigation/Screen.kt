package io.daysleft.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import io.daysleft.app.R

// 定义强类型的屏幕路由及底部导航栏对应的图标和标签
sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector? = null) {
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings)
    object Create : Screen("create", R.string.add_event) // Create 不需要底部图标
    object Edit : Screen("edit/{eventId}", R.string.edit_details) // Edit 不需要底部图标
}