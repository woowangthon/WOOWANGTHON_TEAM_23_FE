package com.woowang.scrollmate.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

// 화면 변화 사이 간격이 이보다 길면 자리를 비운 것으로 보고 이만큼만 셈. 보기 좋게 임의로 정한 값
private const val MAX_GAP_MS = 2 * 60_000L

// 시청 중에는 이 간격마다 기록을 저장함. 넘김, 카드, 나가기는 바로 저장함
private const val SAVE_EVERY_MS = 30_000L
private const val LATE_NIGHT_END_HOUR = 6

// 접근성 서비스가 알려 주는 쇼츠 진입, 넘김, 카드 결과를 세션으로 모아 기기 DB에 저장함
@OptIn(ExperimentalCoroutinesApi::class)
class WatchRecorder(
    private val dao: WatchSessionDao,
    private val scope: CoroutineScope,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    // 저장 순서가 바뀌지 않게 한 번에 하나씩 씀
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private var current: WatchSession? = null
    private var lastTick = 0L
    private var lastSave = 0L

    fun enter(now: Long) {
        current = WatchSession(id = UUID.randomUUID().toString(), startedAt = now, endedAt = now)
        lastTick = now
        save(now)
    }

    // 쇼츠 화면이 바뀔 때마다 불러 본 시간을 더함
    fun tick(now: Long) {
        val session = current ?: return
        val gap = (now - lastTick).coerceIn(0, MAX_GAP_MS)
        val hour = Instant.ofEpochMilli(now).atZone(zone).hour
        current = session.copy(
            endedAt = now,
            activeMs = session.activeMs + gap,
            lateNightMs = session.lateNightMs + if (hour < LATE_NIGHT_END_HOUR) gap else 0,
        )
        lastTick = now
        if (now - lastSave >= SAVE_EVERY_MS) save(now)
    }

    fun swipe(now: Long) = update(now) { it.copy(swipes = it.swipes + 1) }

    fun cardShown(now: Long) = update(now) { it.copy(cardsShown = it.cardsShown + 1) }

    fun cardResult(now: Long, result: AnswerResult) = update(now) {
        when (result) {
            AnswerResult.SKIPPED -> it.copy(cardsSkipped = it.cardsSkipped + 1)
            AnswerResult.CORRECT -> it.copy(cardsAnswered = it.cardsAnswered + 1, correct = it.correct + 1)
            AnswerResult.WRONG -> it.copy(cardsAnswered = it.cardsAnswered + 1)
        }
    }

    fun exit(now: Long) {
        if (current == null) return
        tick(now)
        save(now)
        current = null
    }

    private fun update(now: Long, change: (WatchSession) -> WatchSession) {
        tick(now)
        current = current?.let(change) ?: return
        save(now)
    }

    private fun save(now: Long) {
        val session = current ?: return
        lastSave = now
        scope.launch(writer) { dao.upsert(session) }
    }
}
