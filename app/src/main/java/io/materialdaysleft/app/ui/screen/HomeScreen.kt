package io.materialdaysleft.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.materialdaysleft.app.data.local.CountdownEventEntity
import io.materialdaysleft.app.ui.theme.MaterialDaysLeftTheme
import io.materialdaysleft.app.ui.viewmodel.CountdownViewModel
import io.materialdaysleft.app.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * 带有状态的 HomeScreen 入口，负责与 ViewModel 交互
 */
@Composable
fun HomeScreen(
    viewModel: CountdownViewModel,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // collectAsState 会监听 Flow 的变化并触发 Compose 自动重组 (Recomposition)
    val events by viewModel.allEvents.collectAsState()

    HomeScreenContent(
        events = events,
        onEventClick = onNavigateToEdit,
        modifier = modifier
    )
}

/**
 * 无状态的 UI 内容组件，便于进行 @Preview 预览和 UI 测试
 */
@Composable
fun HomeScreenContent(
    events: List<CountdownEventEntity>,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        // 空状态 (Empty State) UI
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无倒数日\n点击右下角 + 号添加吧！",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        // 倒数日列表
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp) // 底部留白防止被 FAB 遮挡
        ) {
            items(
                items = events,
                key = { it.id } // 提供稳定的 Key 提升列表重组性能
            ) { event ->
                CountdownEventCard(
                    event = event,
                    onClick = { onEventClick(event.id) },
                )
            }
        }
    }
}

/**
 * 遵守 Material 3 Expressive 规范的单个倒数日卡片
 */
@Composable
fun CountdownEventCard(event: CountdownEventEntity,onClick: () -> Unit) {
    // 计算日期差值
    val today = LocalDate.now()
    val initialDaysDiff = ChronoUnit.DAYS.between(today, event.targetDate)
    val isPast = initialDaysDiff < 0
    val absDays = abs(initialDaysDiff)

    // 计算下一次发生日期
    val nextDate = DateUtils.calculateNextOccurrence(event)
    val nextDaysDiff = ChronoUnit.DAYS.between(today, nextDate)

    // 卡片背景动态取色：过去的变灰，未来的使用主色调
    val cardColors = if (isPast) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = cardColors,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：标题与标签
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                val dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                Text(event.targetDate.format(dateFormatter), style = MaterialTheme.typography.bodyMedium)

                // 如果已过且开启了重复，提示下一次的日期
                if (isPast && event.isRepeatEnabled) {
                    Text(
                        text = "下次: ${nextDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 标签展示区 (Tags)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (event.isLunar) {
                        EventTag(text = "农历")
                    }
                    if (event.isRepeatEnabled) {
                        val intervalText = when (event.repeatInterval) {
                            "DAILY" -> "每天"
                            "WEEKLY" -> "每周"
                            "MONTHLY" -> "每月"
                            "YEARLY" -> "每年"
                            else -> "重复"
                        }
                        EventTag(text = intervalText)
                    }
                    if (event.syncToSystemCalendar) {
                        EventTag(text = "系统日历")
                    }
                }
            }

            // 右侧：大字重的倒数天数
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 16.dp)
            ) {
                if (isPast && event.isRepeatEnabled) {
                    // 如果已过但有重复循环，展示距离下一次的天数
                    Text("${nextDaysDiff}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Text("距下次还有", style = MaterialTheme.typography.labelLarge)
                } else {
                    // 常规显示
                    Text(if (initialDaysDiff == 0L) "今天" else absDays.toString(), style = MaterialTheme.typography.displayMedium)
                    Text(if (isPast) "已过天数" else "剩余天数", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/**
 * 自定义圆角小标签组件
 */
@Composable
fun EventTag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ==========================================
// 预览区域 (Previews)
// ==========================================

@Preview(showBackground = true, name = "有数据的列表状态")
@Composable
fun HomeScreenPreview_WithData() {
    MaterialDaysLeftTheme {
        val dummyData = listOf(
            CountdownEventEntity(
                id = 1,
                title = "跨年夜",
                targetDate = LocalDate.now().plusDays(24),
                isLunar = false,
                isRepeatEnabled = true,
                repeatInterval = "YEARLY",
                notifyDaysInAdvance = 1,
                syncToSystemCalendar = true,
                calendarEventId = null
            ),
            CountdownEventEntity(
                id = 2,
                title = "老妈生日",
                targetDate = LocalDate.now().plusDays(5),
                isLunar = true,
                isRepeatEnabled = true,
                repeatInterval = "YEARLY",
                notifyDaysInAdvance = 3,
                syncToSystemCalendar = false,
                calendarEventId = null
            ),
            CountdownEventEntity(
                id = 3,
                title = "驾照考试",
                targetDate = LocalDate.now(),
                isLunar = false,
                isRepeatEnabled = false,
                repeatInterval = null,
                notifyDaysInAdvance = 0,
                syncToSystemCalendar = true,
                calendarEventId = null
            ),
            CountdownEventEntity(
                id = 4,
                title = "上次旅游",
                targetDate = LocalDate.now().minusDays(128),
                isLunar = false,
                isRepeatEnabled = false,
                repeatInterval = null,
                notifyDaysInAdvance = 0,
                syncToSystemCalendar = false,
                calendarEventId = null
            )
        )
        HomeScreenContent(events = dummyData, onEventClick = {})
    }
}

@Preview(showBackground = true, name = "空列表状态")
@Composable
fun HomeScreenPreview_Empty() {
    MaterialDaysLeftTheme {
        HomeScreenContent(events = emptyList(), onEventClick = {})
    }
}