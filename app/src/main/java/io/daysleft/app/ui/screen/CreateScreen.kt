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
import io.daysleft.app.data.local.LunarInfo
import io.daysleft.app.data.local.RepeatInterval
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
    var isWithoutYear by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(1) }
    var selectedDay by remember { mutableStateOf(1) }
    var isRepeatEnabled by remember { mutableStateOf(false) }
    var repeatInterval by remember { mutableStateOf("YEARLY") }
    var notifyDaysInAdvance by remember { mutableStateOf(0f) }
    var notifyTimeHour by remember { mutableStateOf(9) }
    var notifyTimeMinute by remember { mutableStateOf(0) }
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
                        val baseDate = if (isWithoutYear) {
                            if (isLunar) {
                                val solar = com.nlf.calendar.Lunar.fromYmd(LocalDate.now().year, selectedMonth, selectedDay).solar
                                LocalDate.of(solar.year, solar.month, solar.day)
                            } else {
                                LocalDate.of(2000, selectedMonth, selectedDay)
                            }
                        } else targetDate
 
                        viewModel.insertEvent(io.daysleft.app.data.local.CountdownEventEntity(
                            title = title,
                            targetDate = baseDate,
                            lunarInfo = LunarInfo(
                                isLunar = isLunar,
                                isLunarWithoutYear = isLunar && isWithoutYear,
                                lunarMonth = if (isLunar && isWithoutYear) selectedMonth else null,
                                lunarDay = if (isLunar && isWithoutYear) selectedDay else null
                            ),
                            repeatInterval = if (isRepeatEnabled || isWithoutYear) {
                                RepeatInterval.valueOf(if (isWithoutYear) "YEARLY" else repeatInterval)
                            } else null,
                            notifyDaysInAdvance = notifyDaysInAdvance.toInt(),
                            notifyTimeHour = notifyTimeHour,
                            notifyTimeMinute = notifyTimeMinute,
                            syncToSystemCalendar = syncToSystemCalendar,
                            useCalendarNotification = useCalendarNotification,
                            calendarEventId = null,
                            isWithoutYear = isWithoutYear
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
                isWithoutYear = isWithoutYear, onIsWithoutYearChange = { isWithoutYear = it },
                selectedMonth = selectedMonth, onSelectedMonthChange = { selectedMonth = it },
                selectedDay = selectedDay, onSelectedDayChange = { selectedDay = it },
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