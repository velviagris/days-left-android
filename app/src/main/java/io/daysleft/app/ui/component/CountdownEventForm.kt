@file:OptIn(ExperimentalMaterial3Api::class)

package io.daysleft.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import io.daysleft.app.R
import io.daysleft.app.ui.theme.DaysLeftTheme
import io.daysleft.app.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
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
    isWithoutYear: Boolean, onIsWithoutYearChange: (Boolean) -> Unit,
    selectedMonth: Int, onSelectedMonthChange: (Int) -> Unit,
    selectedDay: Int, onSelectedDayChange: (Int) -> Unit,
    isRepeatEnabled: Boolean, onRepeatEnabledChange: (Boolean) -> Unit,
    repeatInterval: String, onRepeatIntervalChange: (String) -> Unit,
    notifyDaysInAdvance: Float, onNotifyDaysChange: (Float) -> Unit,
    notifyTimeHour: Int, onNotifyTimeHourChange: (Int) -> Unit,
    notifyTimeMinute: Int, onNotifyTimeMinuteChange: (Int) -> Unit,
    syncToSystemCalendar: Boolean, onSyncChange: (Boolean) -> Unit,
    useCalendarNotification: Boolean, onUseCalendarNotificationChange: (Boolean) -> Unit,
    lunarMonths: Array<String> = emptyArray(),
    lunarDays: Array<String> = emptyArray(),
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val solarMonths = remember {
        (1..12).map { month ->
            LocalDate.of(2000, month, 1).format(DateTimeFormatter.ofPattern("LLLL", Locale.getDefault()))
        }.toTypedArray()
    }
    val solarDays = remember(selectedMonth) {
        val maxDays = try {
            YearMonth.of(2000, selectedMonth).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
        (1..maxDays).map { day ->
            if (Locale.getDefault().language == "zh") "${day}日" else "$day"
        }.toTypedArray()
    }

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
            label = { Text(stringResource(R.string.event_title_label)) },
            placeholder = { Text(stringResource(R.string.event_title_placeholder)) },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )

        // 2. 公农历切换
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isLunar,
                onClick = { onIsLunarChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.solar)) }
            SegmentedButton(
                selected = isLunar,
                onClick = { onIsLunarChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.lunar)) }
        }

        // 3. 子模式选项 (年月日 vs 月日)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isWithoutYear,
                onClick = { onIsWithoutYearChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text(stringResource(R.string.by_year), style = MaterialTheme.typography.labelMedium) }
            SegmentedButton(
                selected = isWithoutYear,
                onClick = {
                    onIsWithoutYearChange(true)
                    onRepeatEnabledChange(true)
                    onRepeatIntervalChange("YEARLY")
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text(stringResource(R.string.by_month_day), style = MaterialTheme.typography.labelMedium) }
        }

        // 4. 日期/月日选择
        if (isWithoutYear) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val monthsOptions = if (isLunar) lunarMonths else solarMonths
                val daysOptions = if (isLunar) lunarDays else solarDays
                val safeMonthIndex = (selectedMonth - 1).coerceIn(0, monthsOptions.lastIndex)
                val safeDayIndex = (selectedDay - 1).coerceIn(0, daysOptions.lastIndex)

                LunarDropdown(
                    label = if (isLunar) stringResource(R.string.lunar_month_label) else stringResource(R.string.month_label),
                    options = monthsOptions,
                    selectedIndex = safeMonthIndex,
                    onSelect = { onSelectedMonthChange(it + 1) },
                    modifier = Modifier.weight(1f)
                )
                LunarDropdown(
                    label = if (isLunar) stringResource(R.string.lunar_day_label) else stringResource(R.string.day_label),
                    options = daysOptions,
                    selectedIndex = safeDayIndex,
                    onSelect = { onSelectedDayChange(it + 1) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            val solarDateStr = targetDate.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_format_full)))
            val displayText = if (isLunar) {
                "$solarDateStr (${stringResource(R.string.lunar_prefix, DateUtils.getLunarDescription(targetDate))})"
            } else {
                solarDateStr
            }
            OutlinedTextField(
                value = displayText,
                onValueChange = {}, label = { Text(if (isLunar) stringResource(R.string.original_solar_date) else stringResource(R.string.target_date)) },
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
        val forceRepeat = isWithoutYear
        ListItem(
            headlineContent = { Text(stringResource(R.string.enable_repeat)) },
            trailingContent = { Switch(checked = if (forceRepeat) true else isRepeatEnabled, onCheckedChange = onRepeatEnabledChange, enabled = !forceRepeat) }
        )

        if (isRepeatEnabled || forceRepeat) {
            RepeatIntervalDropdown(selectedInterval = repeatInterval, onSelect = onRepeatIntervalChange, enabled = !forceRepeat)
        }

        Column {
            Text(stringResource(R.string.days_in_advance, notifyDaysInAdvance.toInt()), style = MaterialTheme.typography.bodyLarge)
            Slider(value = notifyDaysInAdvance, onValueChange = onNotifyDaysChange, valueRange = 0f..30f, steps = 29)
        }

        ListItem(
            headlineContent = { Text(stringResource(R.string.notify_time)) },
            supportingContent = { Text(stringResource(R.string.notify_time_desc)) },
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
            headlineContent = { Text(stringResource(R.string.sync_to_calendar)) },
            trailingContent = { Switch(checked = syncToSystemCalendar, onCheckedChange = onSyncChange) }
        )

        if (syncToSystemCalendar) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.enable_calendar_notify)) },
                supportingContent = { Text(stringResource(R.string.calendar_notify_desc)) },
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
            }) { Text(stringResource(R.string.confirm)) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            if (isLunar) {
                LunarDatePicker(state = datePickerState)
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
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
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
    val options = listOf(
        "DAILY" to stringResource(R.string.daily),
        "WEEKLY" to stringResource(R.string.weekly),
        "MONTHLY" to stringResource(R.string.monthly),
        "YEARLY" to stringResource(R.string.yearly)
    )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if(enabled) expanded = !expanded }) {
        OutlinedTextField(
            value = options.find { it.first == selectedInterval }?.second ?: "", onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.repeat_interval)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), enabled = enabled
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(key); expanded = false }) }
        }
    }
}

@Composable
fun LunarDatePicker(state: DatePickerState) {
    val weekdays = stringArrayResource(R.array.weekdays_short)
    
    var viewedMonth by remember {
        val initial = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: LocalDate.now()
        mutableStateOf(YearMonth.from(initial))
    }
    var isInputMode by remember { mutableStateOf(false) }
    var isYearPickerVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Headline Section
        val selectedDate = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            Text(text = stringResource(R.string.select_date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedDate?.format(DateTimeFormatter.ofPattern(stringResource(R.string.date_format_picker_headline))) ?: stringResource(R.string.select_date),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // ... (I'll fix the "未选择" below)
                IconButton(onClick = { isInputMode = !isInputMode }) {
                    Icon(if (isInputMode) Icons.Default.DateRange else Icons.Default.Edit, contentDescription = "切换输入模式")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        if (isInputMode) {
            // Input Mode - Material 3 Aesthetic Design
            var inputText by remember(selectedDate) {
                mutableStateOf(selectedDate?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) ?: "")
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        if (it.length == 10) {
                            try {
                                val date = LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                                state.selectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                                viewedMonth = YearMonth.from(date)
                            } catch (e: Exception) { }
                        }
                    },
                    label = { Text(stringResource(R.string.solar_date_label)) },
                    placeholder = { Text(stringResource(R.string.input_date_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                if (selectedDate != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stringResource(R.string.lunar).take(1),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.lunar_convert),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DateUtils.getLunarDescription(selectedDate),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else if (isYearPickerVisible) {
            // Year Picker (3 columns as in image)
            YearPicker(
                selectedYear = viewedMonth.year,
                onYearSelected = {
                    viewedMonth = YearMonth.of(it, viewedMonth.monthValue)
                    isYearPickerVisible = false
                }
            )
        } else {
            // Calendar Mode
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 根据选中日期或当前月份第一天显示农历月
                val lunarMonthDesc = DateUtils.getLunarMonth(selectedDate ?: viewedMonth.atDay(1))
                
                Row(
                    modifier = Modifier.clip(CircleShape).clickable { isYearPickerVisible = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val yearLabel = viewedMonth.year.toString()
                    val monthLabel = viewedMonth.monthValue.toString()
                    val yearMonthDisplay = if (Locale.getDefault().language == "zh") {
                        "${yearLabel}年${monthLabel}月"
                    } else {
                        viewedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy"))
                    }
                    
                    Text(
                        text = "$yearMonthDisplay ($lunarMonthDesc)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Row {
                    IconButton(onClick = { viewedMonth = viewedMonth.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
                    }
                    IconButton(onClick = { viewedMonth = viewedMonth.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
                    }
                }
            }

            // Weekday Header
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Days Grid
            val firstDayOfMonth = viewedMonth.atDay(1)
            val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
            val daysInMonth = viewedMonth.lengthOfMonth()

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(260.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(firstDayOfWeek) { Box(modifier = Modifier.aspectRatio(1f)) }
                items(daysInMonth) { dayIndex ->
                    val date = viewedMonth.atDay(dayIndex + 1)
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()

                    LunarDayCell(
                        date = date,
                        isSelected = isSelected,
                        isToday = isToday,
                        onClick = { state.selectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                    )
                }
            }
        }
    }
}

@Composable
fun YearPicker(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    val currentYear = LocalDate.now().year
    val years = remember { (currentYear - 100..currentYear + 100).toList() }
    val gridState = rememberLazyGridState()

    // 自动滚动到选中年份的位置，并使其居中显示
    LaunchedEffect(Unit) {
        val index = years.indexOf(selectedYear)
        if (index != -1) {
            // 计算偏移量，尝试让选中的行出现在中间
            gridState.scrollToItem(maxOf(0, index - 6))
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(300.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(years.size) { index ->
            val year = years[index]
            val isCurrentSelection = year == selectedYear
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isCurrentSelection) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onYearSelected(year) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrentSelection) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LunarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .let {
                if (isToday && !isSelected) it.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else it
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = DateUtils.getLunarDay(date),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 9.sp),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "表单组件预览")
@Composable
fun FormPreview() {
    DaysLeftTheme {
        CountdownEventForm(
            title = "示例文案", onTitleChange = {},
            targetDate = LocalDate.now(), onTargetDateChange = {},
            isLunar = true, onIsLunarChange = {},
            isWithoutYear = true, onIsWithoutYearChange = {},
            selectedMonth = 1, onSelectedMonthChange = {},
            selectedDay = 1, onSelectedDayChange = {},
            isRepeatEnabled = true, onRepeatEnabledChange = {},
            repeatInterval = "YEARLY", onRepeatIntervalChange = {},
            notifyDaysInAdvance = 5f, onNotifyDaysChange = {},
            notifyTimeHour = 9, onNotifyTimeHourChange = {},
            notifyTimeMinute = 30, onNotifyTimeMinuteChange = {},
            syncToSystemCalendar = false, onSyncChange = {},
            useCalendarNotification = false, onUseCalendarNotificationChange = {},
            lunarMonths = stringArrayResource(R.array.lunar_months),
            lunarDays = stringArrayResource(R.array.lunar_days)
        )
    }
}