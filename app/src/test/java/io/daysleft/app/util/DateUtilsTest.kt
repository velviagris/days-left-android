package io.daysleft.app.util

import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.data.local.LunarInfo
import io.daysleft.app.data.local.RepeatInterval
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun testCalculateEventYears_SolarYearlyRepeat() {
        val event = CountdownEventEntity(
            id = 1,
            title = "Test Birthday",
            targetDate = LocalDate.of(2020, 6, 5),
            repeatInterval = RepeatInterval.YEARLY,
            notifyDaysInAdvance = 0,
            syncToSystemCalendar = false,
            calendarEventId = null
        )

        // 6 years later
        val nextOccurrence = LocalDate.of(2026, 6, 5)
        val years = DateUtils.calculateEventYears(event, nextOccurrence)
        assertEquals(6, years)

        // Same year
        val sameYearOccurrence = LocalDate.of(2020, 6, 5)
        val sameYearYears = DateUtils.calculateEventYears(event, sameYearOccurrence)
        assertEquals(0, sameYearYears)
    }

    @Test
    fun testCalculateEventYears_LunarYearlyRepeatByYear() {
        // Lunar: 庚子年闰四月十四 (which is 2020-06-05)
        // 2026 corresponding lunar occurrence date is 2026-05-30 (丙午年四月十四)
        val event = CountdownEventEntity(
            id = 2,
            title = "Lunar Event",
            targetDate = LocalDate.of(2020, 6, 5),
            lunarInfo = LunarInfo(isLunar = true, isLunarWithoutYear = false),
            repeatInterval = RepeatInterval.YEARLY,
            notifyDaysInAdvance = 0,
            syncToSystemCalendar = false,
            calendarEventId = null
        )

        val nextOccurrence = LocalDate.of(2026, 5, 30) // Lunar 2026-04-14
        val years = DateUtils.calculateEventYears(event, nextOccurrence)
        assertEquals(6, years)
    }

    @Test
    fun testCalculateEventYears_NonRepeatingReturnsZero() {
        val event = CountdownEventEntity(
            id = 3,
            title = "One-time Event",
            targetDate = LocalDate.of(2020, 6, 5),
            repeatInterval = null,
            notifyDaysInAdvance = 0,
            syncToSystemCalendar = false,
            calendarEventId = null
        )

        val nextOccurrence = LocalDate.of(2026, 6, 5)
        val years = DateUtils.calculateEventYears(event, nextOccurrence)
        assertEquals(0, years)
    }

    @Test
    fun testCalculateEventYears_DailyRepeatingReturnsZero() {
        val event = CountdownEventEntity(
            id = 4,
            title = "Daily Repeating Event",
            targetDate = LocalDate.of(2020, 6, 5),
            repeatInterval = RepeatInterval.DAILY,
            notifyDaysInAdvance = 0,
            syncToSystemCalendar = false,
            calendarEventId = null
        )

        val nextOccurrence = LocalDate.of(2026, 6, 5)
        val years = DateUtils.calculateEventYears(event, nextOccurrence)
        assertEquals(0, years)
    }

    @Test
    fun testCalculateEventYears_LunarWithoutYearReturnsZero() {
        val event = CountdownEventEntity(
            id = 5,
            title = "Lunar without year Event",
            targetDate = LocalDate.of(2020, 6, 5),
            lunarInfo = LunarInfo(isLunar = true, isLunarWithoutYear = true, lunarMonth = 4, lunarDay = 14),
            repeatInterval = RepeatInterval.YEARLY,
            notifyDaysInAdvance = 0,
            syncToSystemCalendar = false,
            calendarEventId = null,
            isWithoutYear = true
        )

        val nextOccurrence = LocalDate.of(2026, 5, 30)
        val years = DateUtils.calculateEventYears(event, nextOccurrence)
        assertEquals(0, years)
    }

    @Test
    fun testCalculateEventYears_SolarWithoutYearReturnsZero() {
        val event = CountdownEventEntity(
            id = 6,
            title = "Solar without year Event",
            targetDate = LocalDate.of(2020, 6, 5),
            repeatInterval = RepeatInterval.YEARLY,
            notifyDaysInAdvance = 0,
            syncToSystemCalendar = false,
            calendarEventId = null,
            isWithoutYear = true
        )

        val nextOccurrence = LocalDate.of(2026, 6, 5)
        val years = DateUtils.calculateEventYears(event, nextOccurrence)
        assertEquals(0, years)
    }
}
