package io.daysleft.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.daysleft.app.R
import io.daysleft.app.data.local.LunarInfo
import io.daysleft.app.data.local.RepeatInterval
import io.daysleft.app.ui.component.CountdownEventForm
import io.daysleft.app.ui.viewmodel.CountdownViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    eventId: Long,
    viewModel: CountdownViewModel,
    onNavigateBack: () -> Unit,
    onEventDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var eventToEdit by remember { mutableStateOf<io.daysleft.app.data.local.CountdownEventEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val lunarMonths = stringArrayResource(R.array.lunar_months)
    val lunarDays = stringArrayResource(R.array.lunar_days)

    LaunchedEffect(eventId) {
        eventToEdit = viewModel.getEventById(eventId)
    }

    eventToEdit?.let { original ->
        var title by remember { mutableStateOf(original.title) }
        var targetDate by remember { mutableStateOf(original.targetDate) }
        var isLunar by remember { mutableStateOf(original.isLunar) }
        var isWithoutYear by remember(original) { mutableStateOf(original.isWithoutYear) }
        var selectedMonth by remember(original) {
            mutableStateOf(
                if (original.isLunar && original.isWithoutYear) {
                    original.lunarMonth ?: 1
                } else {
                    original.targetDate.monthValue
                }
            )
        }
        var selectedDay by remember(original) {
            mutableStateOf(
                if (original.isLunar && original.isWithoutYear) {
                    original.lunarDay ?: 1
                } else {
                    original.targetDate.dayOfMonth
                }
            )
        }
        var isRepeatEnabled by remember { mutableStateOf(original.isRepeatEnabled) }
        var repeatInterval by remember { mutableStateOf(original.repeatInterval?.name ?: "YEARLY") }
        var notifyDaysInAdvance by remember { mutableStateOf(original.notifyDaysInAdvance.toFloat()) }
        var notifyTimeHour by remember { mutableStateOf(original.notifyTimeHour) }
        var notifyTimeMinute by remember { mutableStateOf(original.notifyTimeMinute) }
        var syncToSystemCalendar by remember { mutableStateOf(original.syncToSystemCalendar) }
        var useCalendarNotification by remember { mutableStateOf(original.useCalendarNotification) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.edit_details)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(
                            onClick = {
                                val baseDate = if (isWithoutYear) {
                                    if (isLunar) {
                                        val solar = com.nlf.calendar.Lunar.fromYmd(LocalDate.now().year, selectedMonth, selectedDay).solar
                                        LocalDate.of(solar.year, solar.month, solar.day)
                                    } else {
                                        LocalDate.of(2000, selectedMonth, selectedDay)
                                    }
                                } else targetDate

                                viewModel.updateEvent(original.copy(
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
                                    isWithoutYear = isWithoutYear
                                ))
                                onNavigateBack()
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.update))
                        }
                    }
                )
            },
            modifier = modifier
        ) { paddingValues ->
            CountdownEventForm(
                title = title,
                onTitleChange = { title = it },
                targetDate = targetDate,
                onTargetDateChange = { targetDate = it },
                isLunar = isLunar,
                onIsLunarChange = { isLunar = it },
                isWithoutYear = isWithoutYear,
                onIsWithoutYearChange = { isWithoutYear = it },
                selectedMonth = selectedMonth,
                onSelectedMonthChange = { selectedMonth = it },
                selectedDay = selectedDay,
                onSelectedDayChange = { selectedDay = it },
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
                onSyncChange = { syncToSystemCalendar = it },
                useCalendarNotification = useCalendarNotification,
                onUseCalendarNotificationChange = { useCalendarNotification = it },
                lunarMonths = lunarMonths,
                lunarDays = lunarDays,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(stringResource(R.string.delete_confirm_title)) },
                text = { Text(stringResource(R.string.delete_confirm_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteEvent(original)
                            showDeleteConfirm = false
                            onEventDeleted()
                        }
                    ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}