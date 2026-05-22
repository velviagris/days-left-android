@file:OptIn(ExperimentalMaterial3Api::class)

package io.materialdaysleft.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.gson.reflect.TypeToken
import io.materialdaysleft.app.data.local.CountdownEventEntity
import io.materialdaysleft.app.receiver.NotificationReceiver
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModel
import io.materialdaysleft.app.util.BackupUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun SettingsScreen(
    viewModel: CountdownViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 获取当前数据库的所有数据（用于导出）
    val allEvents by viewModel.allEvents.collectAsState()

    // ==========================================
    // 权限状态检查与启动器
    // ==========================================

    // 1. 通知权限 (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true // 低于 Android 13 默认拥有通知权限
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        coroutineScope.launch {
            snackbarHostState.showSnackbar(if (isGranted) "已获得通知权限" else "已拒绝通知权限，倒数日提醒将无法工作")
        }
    }

    // 2. 日历读写权限 (同时申请 Read 和 Write)
    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        )
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] ?: false
        hasCalendarPermission = readGranted && writeGranted

        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                if (hasCalendarPermission) "已获得系统日历读写权限" else "已拒绝日历权限，将无法自动同步日程"
            )
        }
    }

    // ==========================================
    // 导出数据 Launcher (创建文档)
    // ==========================================
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { targetUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val jsonString = BackupUtils.gson.toJson(allEvents)
                    context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("备份导出成功！")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("导出失败: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    // ==========================================
    // 导入数据 Launcher (打开文档)
    // ==========================================
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(sourceUri)
                    val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

                    val listType = object : TypeToken<List<CountdownEventEntity>>() {}.type
                    val importedEvents: List<CountdownEventEntity> = BackupUtils.gson.fromJson(jsonString, listType)

                    // 为了防止主键冲突，将所有导入的 ID 置为 0（让 Room 重新自增分配），并清除原有的日历绑定ID（防止冲突）
                    val safeEvents = importedEvents.map { it.copy(id = 0, calendarEventId = null) }

                    withContext(Dispatchers.Main) {
                        viewModel.insertMultipleEvents(safeEvents) {
                            coroutineScope.launch { snackbarHostState.showSnackbar("成功恢复 ${safeEvents.size} 条记录！") }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("导入失败，文件格式可能不正确")
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("设置") })
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // --- 权限设置组 ---
            Text(
                text = "系统权限",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // 通知权限入口
            ListItem(
                headlineContent = { Text("通知与提醒权限") },
                supportingContent = { Text(if (hasNotificationPermission) "已授权，可以正常接收倒数日提醒" else "未授权，点击申请权限") },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (hasNotificationPermission) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("通知权限已配置妥当") }
                    }
                }
            )

            // 发送测试通知按钮
            if (hasNotificationPermission) {
                ListItem(
                    headlineContent = { Text("发送测试通知", color = MaterialTheme.colorScheme.primary) },
                    supportingContent = { Text("点击测试通知功能是否正常") },
                    leadingContent = { Icon(Icons.Filled.Build, contentDescription = null) },
                    modifier = Modifier.clickable {
                        NotificationReceiver.sendTestNotification(context)
                    }
                )
            }

            // 日历权限入口
            ListItem(
                headlineContent = { Text("系统日历同步权限") },
                supportingContent = { Text(if (hasCalendarPermission) "已授权，可以自动将倒数日写入日历" else "未授权，点击申请日历读写权限") },
                leadingContent = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (!hasCalendarPermission) {
                        // 一次性申请读取和写入权限
                        calendarPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    } else {
                        coroutineScope.launch { snackbarHostState.showSnackbar("日历权限已配置妥当") }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- 数据备份组 ---
            Text(
                text = "数据备份与恢复",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("导出备份") },
                supportingContent = { Text("将所有倒数日保存为 JSON 文件") },
                modifier = Modifier.clickable {
                    val fileName = "MaterialDaysLeft_Backup_${System.currentTimeMillis()}.json"
                    exportLauncher.launch(fileName)
                }
            )

            ListItem(
                headlineContent = { Text("导入恢复") },
                supportingContent = { Text("从本地的 JSON 文件中合并数据") },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        }
    }
}