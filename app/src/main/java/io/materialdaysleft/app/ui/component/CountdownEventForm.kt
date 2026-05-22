@file:OptIn(ExperimentalMaterial3Api::class)

package io.materialdaysleft.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.materialdaysleft.app.ui.screen.LUNAR_DAYS
import io.materialdaysleft.app.ui.screen.LUNAR_MONTHS
import io.materialdaysleft.app.ui.theme.MaterialDaysLeftTheme
import io.materialdaysleft.app.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 核心表单组件：高度解耦，供创建和编辑 BottomSheet 共用
 */
@Composable
fun CountdownEventForm(
    title: String, onTitleChange: (String) -> Unit,
    targetDate: LocalDate, onTargetDateChange: (LocalDate) -> Unit,
    isLunar: Boolean, onIsLunarChange: (Boolean) -> Unit,
    isLunarWithoutYear: Boolean, onIsLunarWithoutYearChange: (Boolean) -> Unit,
    lunarMonth: Int, onLunarMonthChange: (Int) -> Unit,
    lunarDay: Int, onLunarDayChange: (Int) -> Unit,
    isRepeatEnabled: Boolean, onRepeatEnabledChange: (Boolean) -> Unit,
    repeatInterval: String, onRepeatIntervalChange: (String) -> Unit,
    notifyDaysInAdvance: Float, onNotifyDaysChange: (Float) -> Unit,
    notifyTimeHour: Int, onNotifyTimeHourChange: (Int) -> Unit,
    notifyTimeMinute: Int, onNotifyTimeMinuteChange: (Int) -> Unit,
    syncToSystemCalendar: Boolean, onSyncChange: (Boolean) -> Unit,
    useCalendarNotification: Boolean, onUseCalendarNotificationChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. 标题
        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = { Text("倒数日标题") },
            placeholder = { Text("例如：跨年夜、父母生日") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        // 2. 公农历切换
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isLunar,
                onClick = { onIsLunarChange(false); onIsLunarWithoutYearChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("公历") }
            SegmentedButton(
                selected = isLunar,
                onClick = { onIsLunarChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("农历") }
        }

        // 3. 农历二级选项
        if (isLunar) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isLunarWithoutYear,
                    onClick = { onIsLunarWithoutYearChange(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("年份推算", style = MaterialTheme.typography.labelMedium) }
                SegmentedButton(
                    selected = isLunarWithoutYear,
                    onClick = {
                        onIsLunarWithoutYearChange(true)
                        onRepeatEnabledChange(true)
                        onRepeatIntervalChange("YEARLY")
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("指定月日", style = MaterialTheme.typography.labelMedium) }
            }
        }

        // 4. 日期/月日选择
        if (isLunar && isLunarWithoutYear) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LunarDropdown(label = "农历月", options = LUNAR_MONTHS, selectedIndex = lunarMonth - 1, onSelect = { onLunarMonthChange(it + 1) }, modifier = Modifier.weight(1f))
                LunarDropdown(label = "农历日", options = LUNAR_DAYS, selectedIndex = lunarDay - 1, onSelect = { onLunarDayChange(it + 1) }, modifier = Modifier.weight(1f))
            }
        } else {
            val solarDateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
            val displayText = if (isLunar) {
                "$solarDateStr (农历 ${DateUtils.getLunarDescription(targetDate)})"
            } else {
                solarDateStr
            }
            OutlinedTextField(
                value = displayText,
                onValueChange = {}, label = { Text(if (isLunar) "原始公历日期" else "目标日期") },
                readOnly = true, trailingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // 5. 重复与通知 (Slider/Switch)
        val forceRepeat = isLunar && isLunarWithoutYear
        ListItem(
            headlineContent = { Text("开启重复提醒") },
            trailingContent = { Switch(checked = if (forceRepeat) true else isRepeatEnabled, onCheckedChange = onRepeatEnabledChange, enabled = !forceRepeat) }
        )

        if (isRepeatEnabled || forceRepeat) {
            RepeatIntervalDropdown(selectedInterval = repeatInterval, onSelect = onRepeatIntervalChange, enabled = !forceRepeat)
        }

        Column {
            Text("提前提醒天数: ${notifyDaysInAdvance.toInt()} 天", style = MaterialTheme.typography.bodyLarge)
            Slider(value = notifyDaysInAdvance, onValueChange = onNotifyDaysChange, valueRange = 0f..30f, steps = 29)
        }

        ListItem(
            headlineContent = { Text("通知时间") },
            supportingContent = { Text("到达提醒日时，将在该时间发送通知") },
            trailingContent = {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", notifyTimeHour, notifyTimeMinute),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clickable { showTimePicker = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ListItem(
            headlineContent = { Text("同步到系统日历") },
            trailingContent = { Switch(checked = syncToSystemCalendar, onCheckedChange = onSyncChange) }
        )

        if (syncToSystemCalendar) {
            ListItem(
                headlineContent = { Text("开启日历事件提醒") },
                supportingContent = { Text("在系统日历中设置提醒") },
                trailingContent = { Switch(checked = useCalendarNotification, onCheckedChange = onUseCalendarNotificationChange) }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onTargetDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                showDatePicker = false
            }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) {
            if (isLunar) {
                DatePicker(
                    state = datePickerState,
                    headline = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
                            DatePickerDefaults.DatePickerHeadline(
                                selectedDateMillis = datePickerState.selectedDateMillis,
                                displayMode = datePickerState.displayMode,
                                dateFormatter = DatePickerDefaults.dateFormatter()
                            )
                            if (selectedDateMillis != null) {
                                val date = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                                Text(
                                    text = "农历 ${DateUtils.getLunarDescription(date)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                )
            } else {
                DatePicker(state = datePickerState)
            }
        }
    }

    // 时间选择器 Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = notifyTimeHour,
            initialMinute = notifyTimeMinute,
            is24Hour = true // 采用 24 小时制
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onNotifyTimeHourChange(timePickerState.hour)
                    onNotifyTimeMinuteChange(timePickerState.minute)
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

@Composable
fun LunarDropdown(label: String, options: Array<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" }, onValueChange = {}, readOnly = true,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, s -> DropdownMenuItem(text = { Text(s) }, onClick = { onSelect(index); expanded = false }) }
        }
    }
}

@Composable
fun RepeatIntervalDropdown(selectedInterval: String, onSelect: (String) -> Unit, enabled: Boolean) {
    val options = listOf("DAILY" to "每天", "WEEKLY" to "每周", "MONTHLY" to "每月", "YEARLY" to "每年")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if(enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = options.find { it.first == selectedInterval }?.second ?: "", onValueChange = {}, readOnly = true,
            label = { Text("重复频率") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(key); expanded = false }) }
        }
    }
}

@Preview(showBackground = true, name = "表单组件预览")
@Composable
fun FormPreview() {
    MaterialDaysLeftTheme {
        CountdownEventForm(
            title = "示例文案", onTitleChange = {},
            targetDate = LocalDate.now(), onTargetDateChange = {},
            isLunar = true, onIsLunarChange = {},
            isLunarWithoutYear = true, onIsLunarWithoutYearChange = {},
            lunarMonth = 1, onLunarMonthChange = {},
            lunarDay = 1, onLunarDayChange = {},
            isRepeatEnabled = true, onRepeatEnabledChange = {},
            repeatInterval = "YEARLY", onRepeatIntervalChange = {},
            notifyDaysInAdvance = 5f, onNotifyDaysChange = {},
            notifyTimeHour = 9, onNotifyTimeHourChange = {},
            notifyTimeMinute = 30, onNotifyTimeMinuteChange = {},
            syncToSystemCalendar = false, onSyncChange = {},
            useCalendarNotification = false, onUseCalendarNotificationChange = {}
        )
    }
}