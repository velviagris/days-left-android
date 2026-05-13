package io.materialdaysleft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.materialdaysleft.app.ui.MainScreen
import io.materialdaysleft.app.ui.theme.MaterialDaysLeftTheme
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModel
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModelFactory

class MainActivity : ComponentActivity() {

    // 采用懒加载和自定义 Factory 获取 ViewModel
    private val viewModel: CountdownViewModel by viewModels {
        val app = application as MaterialDaysLeftApp
        CountdownViewModelFactory(
            repository = app.repository,
            alarmScheduler = app.alarmScheduler,
            calendarSyncManager = app.calendarSyncManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 开启边到边沉浸式支持
        setContent {
            MaterialDaysLeftTheme {
                // 加载包含 NavGraph 的主屏幕
                MainScreen(viewModel = viewModel)
            }
        }
    }
}