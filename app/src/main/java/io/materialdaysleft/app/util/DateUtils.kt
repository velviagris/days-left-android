package io.materialdaysleft.app.util

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import io.materialdaysleft.app.data.local.CountdownEventEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DateUtils {
    /**
     * 计算事件的下一次发生日期
     */
    fun calculateNextOccurrence(event: CountdownEventEntity): LocalDate {
        val today = LocalDate.now()
        if (!event.isRepeatEnabled) return event.targetDate

        if (event.isLunar && event.repeatInterval == "YEARLY") {
            // ... (农历计算逻辑保持不变，直接复用您现有的农历逻辑) ...
            val targetLunarMonth: Int
            val targetLunarDay: Int

            if (event.lunarMonth != null && event.lunarDay != null) {
                targetLunarMonth = event.lunarMonth
                targetLunarDay = event.lunarDay
            } else {
                val solarOfTarget = Solar.fromYmd(event.targetDate.year, event.targetDate.monthValue, event.targetDate.dayOfMonth)
                targetLunarMonth = solarOfTarget.lunar.month
                targetLunarDay = solarOfTarget.lunar.day
            }

            val lunarThisYear = Lunar.fromYmd(today.year, targetLunarMonth, targetLunarDay)
            val solarThisYear = lunarThisYear.solar
            val dateThisYear = LocalDate.of(solarThisYear.year, solarThisYear.month, solarThisYear.day)

            return if (dateThisYear.isBefore(today)) {
                val lunarNextYear = Lunar.fromYmd(today.year + 1, targetLunarMonth, targetLunarDay)
                val solarNextYear = lunarNextYear.solar
                LocalDate.of(solarNextYear.year, solarNextYear.month, solarNextYear.day)
            } else {
                dateThisYear
            }
        } else {
            // 【修复】完整支持公历的 日/周/月/年 重复
            var nextDate = event.targetDate
            if (nextDate.isBefore(today) || nextDate.isEqual(today)) {
                when (event.repeatInterval) {
                    "DAILY" -> nextDate = today // 每天重复，基准就是今天
                    "WEEKLY" -> {
                        val daysBetween = ChronoUnit.DAYS.between(nextDate, today)
                        val weeksToAdd = (daysBetween / 7) + 1
                        nextDate = nextDate.plusWeeks(weeksToAdd)
                    }
                    "MONTHLY" -> {
                        val monthsBetween = ChronoUnit.MONTHS.between(nextDate, today)
                        nextDate = nextDate.plusMonths(monthsBetween + 1)
                    }
                    "YEARLY" -> {
                        nextDate = nextDate.withYear(today.year)
                        if (nextDate.isBefore(today)) nextDate = nextDate.plusYears(1)
                    }
                }
            }
            return nextDate
        }
    }

    /**
     * 获取日期的农历描述，例如 "十月初一"
     */
    fun getLunarDescription(date: LocalDate): String {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        val lunar = solar.lunar
        return "${lunar.monthInChinese}月${lunar.dayInChinese}"
    }

    /**
     * 获取农历月份描述，如 "正月", "十二月"
     */
    fun getLunarMonth(date: LocalDate): String {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        return "${solar.lunar.monthInChinese}月"
    }

    /**
     * 获取农历日描述，如 "初一", "三十"
     */
    fun getLunarDay(date: LocalDate): String {
        val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
        return solar.lunar.dayInChinese
    }
}