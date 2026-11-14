package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.WatchSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WatchSummaryTest {
    private val zone = ZoneId.of("Asia/Seoul")

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun groupsByStartDayAndSums() {
        val sessions = listOf(
            WatchSession("a", at(13, 23, 50), at(14, 0, 20), activeMs = 30 * 60_000L, lateNightMs = 20 * 60_000L,
                swipes = 50, cardsShown = 5, cardsAnswered = 4, cardsSkipped = 1, correct = 3),
            WatchSession("b", at(13, 12), at(13, 12, 10), activeMs = 10 * 60_000L, swipes = 20, cardsShown = 2,
                cardsAnswered = 2, correct = 1),
            WatchSession("c", at(14, 9), at(14, 9, 5), activeMs = 5 * 60_000L, swipes = 7),
        )
        val days = summarizeByDay(sessions, zone)
        assertEquals(listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 14)), days.map { it.date })
        val first = days[0]
        assertEquals(2, first.sessionCount)
        assertEquals(40, first.totalMinutes)
        assertEquals(70, first.swipes)
        assertEquals(6, first.cardsAnswered)
        assertEquals(4, first.correct)
        assertEquals(0.5, first.lateNightShare, 1e-9)
        assertEquals(30, first.longestSessionMinutes)
        assertEquals(0.0, days[1].lateNightShare, 1e-9)
    }

    @Test
    fun emptyInputGivesNoDays() {
        assertEquals(emptyList<WatchDay>(), summarizeByDay(emptyList(), zone))
    }
}
