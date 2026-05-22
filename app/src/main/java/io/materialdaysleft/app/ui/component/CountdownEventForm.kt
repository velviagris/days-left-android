@file:OptIn(ExperimentalMaterial3Api::class)

package io.materialdaysleft.app.ui.component

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
import io.materialdaysleft.app.ui.screen.LUNAR_DAYS
import io.materialdaysleft.app.ui.screen.LUNAR_MONTHS
import io.materialdaysleft.app.ui.theme.MaterialDaysLeftTheme
import io.materialdaysleft.app.util.DateUtils
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

@Composable
fun LunarDatePicker(state: DatePickerState) {
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
            Text(text = "选择日期", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedDate?.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) ?: "未选择",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
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
                    label = { Text("公历日期") },
                    placeholder = { Text("yyyy/MM/dd") },
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
                                        text = "农",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "转换农历",
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
                    Text(
                        text = "${viewedMonth.year}年${viewedMonth.monthValue}月 ($lunarMonthDesc)",
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
                val daysOfWeek = listOf("日", "一", "二", "三", "四", "五", "六")
                daysOfWeek.forEach { day ->
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