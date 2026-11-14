package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.WatchSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val MS_PER_MINUTE = 60_000.0

// 하루 합계. 서버에는 이 숫자만 보냄 (api.md UsageDay)
data class WatchDay(
    val date: LocalDate,
    val sessionCount: Int,
    val totalMinutes: Int,
    val swipes: Int,
    val cardsShown: Int,
    val cardsAnswered: Int,
    val cardsSkipped: Int,
    val correct: Int,
    val lateNightShare: Double,
    val longestSessionMinutes: Int,
)

// 쇼츠에 들어온 날짜 기준으로 묶음. 자정을 넘긴 시청은 들어온 날에 넣음
fun summarizeByDay(sessions: List<WatchSession>, zone: ZoneId): List<WatchDay> =
    sessions.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
        .map { (date, list) ->
            val active = list.sumOf { it.activeMs }
            WatchDay(
                date = date,
                sessionCount = list.size,
                totalMinutes = Math.round(active / MS_PER_MINUTE).toInt(),
                swipes = list.sumOf { it.swipes },
                cardsShown = list.sumOf { it.cardsShown },
                cardsAnswered = list.sumOf { it.cardsAnswered },
                cardsSkipped = list.sumOf { it.cardsSkipped },
                correct = list.sumOf { it.correct },
                lateNightShare = if (active > 0) list.sumOf { it.lateNightMs }.toDouble() / active else 0.0,
                longestSessionMinutes = Math.round(list.maxOf { it.activeMs } / MS_PER_MINUTE).toInt(),
            )
        }
        .sortedBy { it.date }
