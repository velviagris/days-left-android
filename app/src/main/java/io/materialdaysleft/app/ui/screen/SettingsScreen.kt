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
import androidx.compose.ui.res.stringResource
import io.materialdaysleft.app.R
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
            snackbarHostState.showSnackbar(
                if (isGranted) context.getString(R.string.notification_permission_granted)
                else context.getString(R.string.notification_permission_refused)
            )
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
                if (hasCalendarPermission) context.getString(R.string.calendar_permission_granted_toast)
                else context.getString(R.string.calendar_permission_refused_toast)
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
                        snackbarHostState.showSnackbar(context.getString(R.string.export_success))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(context.getString(R.string.export_failed, e.localizedMessage))
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
                            coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.import_success, safeEvents.size)) }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings)) })
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
                text = stringResource(R.string.system_permissions),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            // 通知权限入口
            ListItem(
                headlineContent = { Text(stringResource(R.string.notification_permission)) },
                supportingContent = { Text(if (hasNotificationPermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied)) },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                modifier = Modifier.clickable {
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (hasNotificationPermission) {
                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.notification_permission_ready)) }
                    }
                }
            )

            // 发送测试通知按钮
            if (hasNotificationPermission) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.test_notification), color = MaterialTheme.colorScheme.primary) },
                    supportingContent = { Text(stringResource(R.string.test_notification_desc)) },
                    leadingContent = { Icon(Icons.Filled.Build, contentDescription = null) },
                    modifier = Modifier.clickable {
                        NotificationReceiver.sendTestNotification(context)
                    }
                )
            }

            // 日历权限入口
            ListItem(
                headlineContent = { Text(stringResource(R.string.calendar_sync_permission)) },
                supportingContent = { Text(if (hasCalendarPermission) stringResource(R.string.calendar_permission_granted_hint) else stringResource(R.string.calendar_permission_denied_hint)) },
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
                        coroutineScope.launch { snackbarHostState.showSnackbar(context.getString(R.string.calendar_permission_ready)) }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- 数据备份组 ---
            Text(
                text = stringResource(R.string.data_backup_restore),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.export_backup)) },
                supportingContent = { Text(stringResource(R.string.export_backup_desc)) },
                modifier = Modifier.clickable {
                    val fileName = "MaterialDaysLeft_Backup_${System.currentTimeMillis()}.json"
                    exportLauncher.launch(fileName)
                }
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.import_restore)) },
                supportingContent = { Text(stringResource(R.string.import_restore_desc)) },
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        }
    }
}