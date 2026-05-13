@file:OptIn(ExperimentalMaterial3Api::class)

package io.materialdaysleft.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.materialdaysleft.app.ui.component.CountdownEventForm
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModel
import java.time.LocalDate

/**
 * 农历月份的中文字典
 */
val LUNAR_MONTHS = arrayOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")

/**
 * 农历日期的中文字典
 */
val LUNAR_DAYS = arrayOf(
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)

@Composable
fun CreateBottomSheet(
    viewModel: CountdownViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                Text("添加倒数日", style = MaterialTheme.typography.titleLarge)
                TextButton(
                    onClick = {
                        val baseDate = if (isLunar && isLunarWithoutYear) {
                            val solar = com.nlf.calendar.Lunar.fromYmd(LocalDate.now().year, lunarMonth, lunarDay).solar
                            LocalDate.of(solar.year, solar.month, solar.day)
                        } else targetDate

                        viewModel.insertEvent(io.materialdaysleft.app.data.local.CountdownEventEntity(
                            title = title, targetDate = baseDate, isLunar = isLunar,
                            isLunarWithoutYear = isLunarWithoutYear,
                            lunarMonth = if (isLunarWithoutYear) lunarMonth else null,
                            lunarDay = if (isLunarWithoutYear) lunarDay else null,
                            isRepeatEnabled = if (isLunarWithoutYear) true else isRepeatEnabled,
                            repeatInterval = if (isLunarWithoutYear) "YEARLY" else repeatInterval,
                            notifyDaysInAdvance = notifyDaysInAdvance.toInt(),
                            notifyTimeHour = notifyTimeHour,       // 存入数据库
                            notifyTimeMinute = notifyTimeMinute,   // 存入数据库
                            syncToSystemCalendar = syncToSystemCalendar, calendarEventId = null
                        ))
                        onDismiss()
                    },
                    enabled = title.isNotBlank()
                ) { Text("保存") }
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
                syncToSystemCalendar = syncToSystemCalendar, onSyncChange = { syncToSystemCalendar = it }
            )
        }
    }
}