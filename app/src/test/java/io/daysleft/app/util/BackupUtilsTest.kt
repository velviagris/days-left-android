package io.daysleft.app.util

import com.google.gson.reflect.TypeToken
import io.daysleft.app.data.local.CountdownEventEntity
import io.daysleft.app.data.local.RepeatInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class BackupUtilsTest {

    @Test
    fun testDeserializeObfuscatedBackupData() {
        val obfuscatedJson = """
        [
            {
                "a": 1,
                "b": "我生日",
                "c": "1997-01-15",
                "d":
                {
                    "a": false,
                    "b": false
                },
                "e": "YEARLY",
                "f": 7,
                "g": 9,
                "h": 0,
                "i": true,
                "j": true,
                "l": false
            },
            {
                "a": 2,
                "b": "Ingress",
                "c": "2016-06-27",
                "d":
                {
                    "a": false,
                    "b": false
                },
                "e": "YEARLY",
                "f": 0,
                "g": 9,
                "h": 0,
                "i": false,
                "j": false,
                "l": false
            },
            {
                "a": 3,
                "b": "皮蛋生日",
                "c": "2019-06-26",
                "d":
                {
                    "a": false,
                    "b": false
                },
                "e": "YEARLY",
                "f": 0,
                "g": 9,
                "h": 0,
                "i": true,
                "j": true,
                "l": false
            },
            {
                "a": 4,
                "b": "妈妈生日",
                "c": "2026-06-06",
                "d":
                {
                    "a": false,
                    "b": false
                },
                "e": "YEARLY",
                "f": 7,
                "g": 9,
                "h": 0,
                "i": true,
                "j": true,
                "l": true
            }
        ]
        """.trimIndent()

        val listType = object : TypeToken<List<CountdownEventEntity>>() {}.type
        val events: List<CountdownEventEntity> = BackupUtils.gson.fromJson(obfuscatedJson, listType)

        assertNotNull(events)
        assertEquals(4, events.size)

        // 验证第一个：我生日
        val first = events[0]
        assertEquals(1L, first.id)
        assertEquals("我生日", first.title)
        assertEquals(LocalDate.of(1997, 1, 15), first.targetDate)
        assertEquals(false, first.lunarInfo.isLunar)
        assertEquals(false, first.lunarInfo.isLunarWithoutYear)
        assertEquals(RepeatInterval.YEARLY, first.repeatInterval)
        assertEquals(7, first.notifyDaysInAdvance)
        assertEquals(9, first.notifyTimeHour)
        assertEquals(0, first.notifyTimeMinute)
        assertEquals(true, first.syncToSystemCalendar)
        assertEquals(true, first.useCalendarNotification)
        assertEquals(false, first.isWithoutYear)

        // 验证第四个：妈妈生日
        val fourth = events[3]
        assertEquals(4L, fourth.id)
        assertEquals("妈妈生日", fourth.title)
        assertEquals(LocalDate.of(2026, 6, 6), fourth.targetDate)
        assertEquals(false, fourth.lunarInfo.isLunar)
        assertEquals(false, fourth.lunarInfo.isLunarWithoutYear)
        assertEquals(RepeatInterval.YEARLY, fourth.repeatInterval)
        assertEquals(7, fourth.notifyDaysInAdvance)
        assertEquals(9, fourth.notifyTimeHour)
        assertEquals(0, fourth.notifyTimeMinute)
        assertEquals(true, fourth.syncToSystemCalendar)
        assertEquals(true, fourth.useCalendarNotification)
        assertEquals(true, fourth.isWithoutYear)
    }

    @Test
    fun testDeserializeNormalBackupData() {
        val normalJson = """
        [
            {
                "id": 10,
                "title": "New Year",
                "targetDate": "2026-01-01",
                "lunarInfo": {
                    "isLunar": false,
                    "isLunarWithoutYear": false,
                    "lunarMonth": null,
                    "lunarDay": null
                },
                "repeatInterval": "YEARLY",
                "notifyDaysInAdvance": 1,
                "notifyTimeHour": 9,
                "notifyTimeMinute": 30,
                "syncToSystemCalendar": true,
                "useCalendarNotification": false,
                "calendarEventId": 12345,
                "isWithoutYear": false
            }
        ]
        """.trimIndent()

        val listType = object : TypeToken<List<CountdownEventEntity>>() {}.type
        val events: List<CountdownEventEntity> = BackupUtils.gson.fromJson(normalJson, listType)

        assertNotNull(events)
        assertEquals(1, events.size)

        val first = events[0]
        assertEquals(10L, first.id)
        assertEquals("New Year", first.title)
        assertEquals(LocalDate.of(2026, 1, 1), first.targetDate)
        assertEquals(false, first.lunarInfo.isLunar)
        assertEquals(RepeatInterval.YEARLY, first.repeatInterval)
        assertEquals(1, first.notifyDaysInAdvance)
        assertEquals(9, first.notifyTimeHour)
        assertEquals(30, first.notifyTimeMinute)
        assertEquals(true, first.syncToSystemCalendar)
        assertEquals(false, first.useCalendarNotification)
        assertEquals(12345L, first.calendarEventId)
        assertEquals(false, first.isWithoutYear)
    }
}
