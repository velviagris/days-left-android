package io.materialdaysleft.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.materialdaysleft.app.ui.navigation.Screen
import io.materialdaysleft.app.ui.screen.CreateBottomSheet
import io.materialdaysleft.app.ui.screen.EditBottomSheet
import io.materialdaysleft.app.ui.screen.HomeScreen
import io.materialdaysleft.app.ui.screen.SettingsScreen
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModel

/**
 * 应用的主壳组件，负责协调底部导航、FAB 以及 BottomSheet 的显示状态
 */
@Composable
fun MainScreen(viewModel: CountdownViewModel) {
    val navController = rememberNavController()

    // 1. 控制 BottomSheet 显示的状态
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf<Long?>(null) }

    // 2. 底部导航项定义
    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Settings
    )

    // 监听当前路由状态
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            screen.icon?.let { Icon(it, contentDescription = stringResource(screen.titleResId)) }
                        },
                        label = { Text(stringResource(screen.titleResId)) },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                // 避免在返回栈中积累多个目标页面的实例
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            // 只有在首页才显示添加按钮
            if (currentDestination?.route == Screen.Home.route) {
                FloatingActionButton(
                    onClick = { showCreateSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(io.materialdaysleft.app.R.string.add_event))
                }
            }
        }
    ) { innerPadding ->
        // 3. 页面内容区域 (仅保留 Home 和 Settings)
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToEdit = { eventId ->
                        editingEventId = eventId
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // 4. 全局弹出的 BottomSheet 逻辑处理

        // 添加倒数日半屏
        if (showCreateSheet) {
            CreateBottomSheet(
                viewModel = viewModel,
                onDismiss = { showCreateSheet = false }
            )
        }

        // 编辑倒数日半屏
        editingEventId?.let { eventId ->
            EditBottomSheet(
                eventId = eventId,
                viewModel = viewModel,
                onDismiss = { editingEventId = null }
            )
        }
    }
}