@file:OptIn(ExperimentalMaterial3Api::class)

package io.daysleft.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import io.daysleft.app.R
import io.daysleft.app.ui.component.CountdownEventForm
import io.daysleft.app.ui.viewmodel.CountdownViewModel
import java.time.LocalDate

@Composable
fun CreateBottomSheet(
    viewModel: CountdownViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val lunarMonths = stringArrayResource(R.array.lunar_months)
    val lunarDays = stringArrayResource(R.array.lunar_days)

    // 表单局部状态
    var title by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf(LocalDate.now()) }
    var isLunar by remember { mutableStateOf(false) }
    var isLunarWithoutYear by remember { mutableStateOf(false) }
    var lunarMonth by remember { mutableStateOf(1) }
    var lunarDay by remember { mutableStateOf(1) }
    var isRepeatEnabled by remember { mutableStateOf(false) }
    var repeatInterval by remember { mutableStateOf("YEARLY") }
    var notifyDaysInAdvance by remember { mutableStateOf(0f) }
    var notifyTimeHour by remember { mutableStateOf(9) }     // 新增：默认 9 点
    var notifyTimeMinute by remember { mutableStateOf(0) }   // 新增：默认 0 分
    var syncToSystemCalendar by remember { mutableStateOf(false) }
    var useCalendarNotification by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.add_event), style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = {
                        val baseDate = if (isLunar && isLunarWithoutYear) {
                            val solar = com.nlf.calendar.Lunar.fromYmd(LocalDate.now().year, lunarMonth, lunarDay).solar
                            LocalDate.of(solar.year, solar.month, solar.day)
                        } else targetDate

                        viewModel.insertEvent(io.daysleft.app.data.local.CountdownEventEntity(
                            title = title, targetDate = baseDate, isLunar = isLunar,
                            isLunarWithoutYear = isLunarWithoutYear,
                            lunarMonth = if (isLunarWithoutYear) lunarMonth else null,
                            lunarDay = if (isLunarWithoutYear) lunarDay else null,
                            isRepeatEnabled = if (isLunarWithoutYear) true else isRepeatEnabled,
                            repeatInterval = if (isLunarWithoutYear) "YEARLY" else repeatInterval,
                            notifyDaysInAdvance = notifyDaysInAdvance.toInt(),
                            notifyTimeHour = notifyTimeHour,       // 存入数据库
                            notifyTimeMinute = notifyTimeMinute,   // 存入数据库
                            syncToSystemCalendar = syncToSystemCalendar,
                            useCalendarNotification = useCalendarNotification,
                            calendarEventId = null
                        ))
                        onDismiss()
                    },
                    enabled = title.isNotBlank()
                ) { Text(stringResource(R.string.save)) }
            }

            CountdownEventForm(
                title = title, onTitleChange = { title = it },
                targetDate = targetDate, onTargetDateChange = { targetDate = it },
                isLunar = isLunar, onIsLunarChange = { isLunar = it },
                isLunarWithoutYear = isLunarWithoutYear, onIsLunarWithoutYearChange = { isLunarWithoutYear = it },
                lunarMonth = lunarMonth, onLunarMonthChange = { lunarMonth = it },
                lunarDay = lunarDay, onLunarDayChange = { lunarDay = it },
                isRepeatEnabled = isRepeatEnabled, onRepeatEnabledChange = { isRepeatEnabled = it },
                repeatInterval = repeatInterval, onRepeatIntervalChange = { repeatInterval = it },
                notifyDaysInAdvance = notifyDaysInAdvance, onNotifyDaysChange = { notifyDaysInAdvance = it },
                notifyTimeHour = notifyTimeHour, onNotifyTimeHourChange = { notifyTimeHour = it },
                notifyTimeMinute = notifyTimeMinute, onNotifyTimeMinuteChange = { notifyTimeMinute = it },
                syncToSystemCalendar = syncToSystemCalendar, onSyncChange = { syncToSystemCalendar = it },
                useCalendarNotification = useCalendarNotification, onUseCalendarNotificationChange = { useCalendarNotification = it },
                lunarMonths = lunarMonths,
                lunarDays = lunarDays
            )
        }
    }
}