@file:OptIn(ExperimentalMaterial3Api::class)

package io.materialdaysleft.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModel
import java.time.LocalDate

@Composable
fun EditBottomSheet(
    eventId: Long,
    viewModel: CountdownViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var eventToEdit by remember { mutableStateOf<io.materialdaysleft.app.data.local.CountdownEventEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        eventToEdit = viewModel.getEventById(eventId)
    }

    eventToEdit?.let { original ->
        var title by remember { mutableStateOf(original.title) }
        var targetDate by remember { mutableStateOf(original.targetDate) }
        var isLunar by remember { mutableStateOf(original.isLunar) }
        var isLunarWithoutYear by remember { mutableStateOf(original.isLunarWithoutYear) }
        var lunarMonth by remember { mutableStateOf(original.lunarMonth ?: 1) }
        var lunarDay by remember { mutableStateOf(original.lunarDay ?: 1) }
        var isRepeatEnabled by remember { mutableStateOf(original.isRepeatEnabled) }
        var repeatInterval by remember { mutableStateOf(original.repeatInterval ?: "YEARLY") }
        var notifyDaysInAdvance by remember { mutableStateOf(original.notifyDaysInAdvance.toFloat()) }
        var notifyTimeHour by remember { mutableStateOf(original.notifyTimeHour) }     // 从数据库回显
        var notifyTimeMinute by remember { mutableStateOf(original.notifyTimeMinute) } // 从数据库回显
        var syncToSystemCalendar by remember { mutableStateOf(original.syncToSystemCalendar) }

        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                    Text("编辑详情", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = {
                        val baseDate = if (isLunar && isLunarWithoutYear) {
                            val solar = com.nlf.calendar.Lunar.fromYmd(LocalDate.now().year, lunarMonth, lunarDay).solar
                            LocalDate.of(solar.year, solar.month, solar.day)
                        } else targetDate

                        viewModel.updateEvent(original.copy(
                            title = title, targetDate = baseDate, isLunar = isLunar,
                            isLunarWithoutYear = isLunarWithoutYear,
                            lunarMonth = if (isLunarWithoutYear) lunarMonth else null,
                            lunarDay = if (isLunarWithoutYear) lunarDay else null,
                            isRepeatEnabled = if (isLunarWithoutYear) true else isRepeatEnabled,
                            repeatInterval = if (isLunarWithoutYear) "YEARLY" else repeatInterval,
                            notifyDaysInAdvance = notifyDaysInAdvance.toInt(),
                            notifyTimeHour = notifyTimeHour,       // 更新并保存
                            notifyTimeMinute = notifyTimeMinute,   // 更新并保存
                            syncToSystemCalendar = syncToSystemCalendar
                        ))
                        onDismiss()
                    }) { Text("更新") }
                }

                _root_ide_package_.io.materialdaysleft.app.ui.component.CountdownEventForm(
                    title = title,
                    onTitleChange = { title = it },
                    targetDate = targetDate,
                    onTargetDateChange = { targetDate = it },
                    isLunar = isLunar,
                    onIsLunarChange = { isLunar = it },
                    isLunarWithoutYear = isLunarWithoutYear,
                    onIsLunarWithoutYearChange = { isLunarWithoutYear = it },
                    lunarMonth = lunarMonth,
                    onLunarMonthChange = { lunarMonth = it },
                    lunarDay = lunarDay,
                    onLunarDayChange = { lunarDay = it },
                    isRepeatEnabled = isRepeatEnabled,
                    onRepeatEnabledChange = { isRepeatEnabled = it },
                    repeatInterval = repeatInterval,
                    onRepeatIntervalChange = { repeatInterval = it },
                    notifyDaysInAdvance = notifyDaysInAdvance,
                    onNotifyDaysChange = { notifyDaysInAdvance = it },
                    notifyTimeHour = notifyTimeHour,
                    onNotifyTimeHourChange = { notifyTimeHour = it },
                    notifyTimeMinute = notifyTimeMinute,
                    onNotifyTimeMinuteChange = { notifyTimeMinute = it },
                    syncToSystemCalendar = syncToSystemCalendar,
                    onSyncChange = { syncToSystemCalendar = it }
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("删除确认") },
                text = { Text("确定要删除此倒数日吗？") },
                confirmButton = { TextButton(onClick = { viewModel.deleteEvent(original); onDismiss() }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
            )
        }
    }
}