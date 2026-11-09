package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.AnswerResult
import java.time.Duration
import java.time.Instant

// 틀린 문제를 다시 내는 간격 규칙 (FE #14). 임의로 정한 값이라 시연하며 조정함
data class ReviewConfig(
    // 틀리거나 건너뛴 뒤 다시 낼 때까지의 일수
    val retryAfterMissDays: Long = 1,
    // 틀린 뒤 맞힌 횟수별 다음에 다시 낼 때까지의 일수. 첫 번째로 맞히면 3일, 두 번째로 맞히면 7일 뒤.
    // 이 목록의 횟수를 모두 채워 맞히면 더 내지 않음
    val afterCorrectDays: List<Long> = listOf(3, 7),
)

// 풀이 기록 한 건. 시각은 기기에서 저장한 값을 Instant로 바꾼 것
data class ReviewRecord(val result: AnswerResult, val at: Instant)

enum class ReviewStatus {
    // 한 번도 풀지 않음
    NEW,

    // 다시 낼 때가 됨
    DUE,

    // 다시 낼 때를 기다리는 중
    WAITING,

    // 틀린 적이 없거나, 틀린 뒤 충분히 맞혀서 더 내지 않음
    MASTERED,
}

// 한 문제의 풀이 기록으로 지금 내도 되는지 정함. 기록 순서는 상관없음.
// 마지막 풀이가 틀림·건너뜀이면 retryAfterMissDays 뒤, 틀린 뒤 맞혔다면 맞힌 횟수에 맞는 간격 뒤에 다시 냄.
// 틀린 적이 없이 맞히기만 했다면 다시 내지 않음
fun reviewStatus(records: List<ReviewRecord>, now: Instant, config: ReviewConfig = ReviewConfig()): ReviewStatus {
    if (records.isEmpty()) return ReviewStatus.NEW
    val sorted = records.sortedBy { it.at }
    val last = sorted.last()

    val waitDays = when (last.result) {
        AnswerResult.WRONG, AnswerResult.SKIPPED -> config.retryAfterMissDays
        AnswerResult.CORRECT -> {
            val lastWrong = sorted.indexOfLast { it.result == AnswerResult.WRONG }
            if (lastWrong == -1) return ReviewStatus.MASTERED
            val correctSinceWrong = sorted.drop(lastWrong + 1).count { it.result == AnswerResult.CORRECT }
            config.afterCorrectDays.getOrNull(correctSinceWrong - 1) ?: return ReviewStatus.MASTERED
        }
    }
    return if (now >= last.at + Duration.ofDays(waitDays)) ReviewStatus.DUE else ReviewStatus.WAITING
}
