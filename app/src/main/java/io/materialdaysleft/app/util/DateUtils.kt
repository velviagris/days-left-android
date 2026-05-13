package io.materialdaysleft.app.util

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import io.materialdaysleft.app.data.local.CountdownEventEntity
import java.time.LocalDate

object DateUtils {
    /**
     * 计算事件的下一次发生日期
     */
    fun calculateNextOccurrence(event: CountdownEventEntity): LocalDate {
        val today = LocalDate.now()
        if (!event.isRepeatEnabled) return event.targetDate

        // 如果初始日期还没到，下一次就是它本身
        if (!event.targetDate.isBefore(today)) return event.targetDate

        if (event.isLunar && event.repeatInterval == "YEARLY") {
            // 【修复核心】提取目标农历月和日
            val targetLunarMonth: Int
            val targetLunarDay: Int

            if (event.lunarMonth != null && event.lunarDay != null) {
                // 1. 如果是直接指定月日的，直接使用数据库里的值
                targetLunarMonth = event.lunarMonth
                targetLunarDay = event.lunarDay
            } else {
                // 2. 如果是年份推算的，先将公历的 targetDate 转为 Solar 对象，再获取对应的真实农历月日
                val solarOfTarget = Solar.fromYmd(
                    event.targetDate.year,
                    event.targetDate.monthValue,
                    event.targetDate.dayOfMonth
                )
                targetLunarMonth = solarOfTarget.lunar.month
                targetLunarDay = solarOfTarget.lunar.day
            }

            // 构造今年的该农历日期
            val lunarThisYear = Lunar.fromYmd(today.year, targetLunarMonth, targetLunarDay)
            val solarThisYear = lunarThisYear.solar
            val dateThisYear = LocalDate.of(solarThisYear.year, solarThisYear.month, solarThisYear.day)

            // 如果今年的农历日期已经过去，则取明年的
            return if (dateThisYear.isBefore(today)) {
                val lunarNextYear = Lunar.fromYmd(today.year + 1, targetLunarMonth, targetLunarDay)
                val solarNextYear = lunarNextYear.solar
                LocalDate.of(solarNextYear.year, solarNextYear.month, solarNextYear.day)
            } else {
                dateThisYear
            }
        } else {
            // 公历重复处理 (简化的按年重复逻辑，可按需扩展 WEEKLY/MONTHLY)
            if (event.repeatInterval == "YEARLY") {
                var nextDate = event.targetDate.withYear(today.year)
                if (nextDate.isBefore(today)) {
                    nextDate = nextDate.plusYears(1)
                }
                return nextDate
            }
            return event.targetDate
        }
    }
}