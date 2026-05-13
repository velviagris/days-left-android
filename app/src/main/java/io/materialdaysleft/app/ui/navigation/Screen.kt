package io.materialdaysleft.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// 定义强类型的屏幕路由及底部导航栏对应的图标和标签
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "首页", Icons.Filled.Home)
    object Settings : Screen("settings", "设置", Icons.Filled.Settings)
    object Create : Screen("create", "添加倒数日") // Create 不需要底部图标
    object Edit : Screen("edit/{eventId}", "编辑倒数日") // Edit 不需要底部图标
}