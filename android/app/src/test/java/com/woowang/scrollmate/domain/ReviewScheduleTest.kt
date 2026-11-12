package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.AnswerResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ReviewScheduleTest {
    private val now = Instant.parse("2026-11-09T12:00:00Z")

    private fun at(result: AnswerResult, daysAgo: Long, hoursAgo: Long = 0) =
        ReviewRecord(result, now.minus(Duration.ofDays(daysAgo)).minus(Duration.ofHours(hoursAgo)))

    @Test
    fun noRecordsMeansNew() {
        assertEquals(ReviewStatus.NEW, reviewStatus(emptyList(), now))
    }

    @Test
    fun correctWithoutEverBeingWrongIsMastered() {
        assertEquals(ReviewStatus.MASTERED, reviewStatus(listOf(at(AnswerResult.CORRECT, 0)), now))
    }

    @Test
    fun wrongAnswerWaitsOneDay() {
        assertEquals(ReviewStatus.WAITING, reviewStatus(listOf(at(AnswerResult.WRONG, 0, hoursAgo = 23)), now))
        assertEquals(ReviewStatus.DUE, reviewStatus(listOf(at(AnswerResult.WRONG, 1)), now))
    }

    @Test
    fun skippedWaitsOneDay() {
        assertEquals(ReviewStatus.WAITING, reviewStatus(listOf(at(AnswerResult.SKIPPED, 0)), now))
        assertEquals(ReviewStatus.DUE, reviewStatus(listOf(at(AnswerResult.SKIPPED, 2)), now))
    }

    @Test
    fun firstCorrectAfterWrongWaitsThreeDays() {
        val records = listOf(at(AnswerResult.WRONG, 10), at(AnswerResult.CORRECT, 2))
        assertEquals(ReviewStatus.WAITING, reviewStatus(records, now))
        assertEquals(ReviewStatus.DUE, reviewStatus(listOf(at(AnswerResult.WRONG, 10), at(AnswerResult.CORRECT, 3)), now))
    }

    @Test
    fun secondCorrectAfterWrongWaitsSevenDays() {
        val waiting = listOf(at(AnswerResult.WRONG, 30), at(AnswerResult.CORRECT, 10), at(AnswerResult.CORRECT, 6))
        val due = listOf(at(AnswerResult.WRONG, 30), at(AnswerResult.CORRECT, 10), at(AnswerResult.CORRECT, 7))

        assertEquals(ReviewStatus.WAITING, reviewStatus(waiting, now))
        assertEquals(ReviewStatus.DUE, reviewStatus(due, now))
    }

    @Test
    fun thirdCorrectAfterWrongIsMastered() {
        val records = listOf(
            at(AnswerResult.WRONG, 30),
            at(AnswerResult.CORRECT, 20),
            at(AnswerResult.CORRECT, 10),
            at(AnswerResult.CORRECT, 0),
        )

        assertEquals(ReviewStatus.MASTERED, reviewStatus(records, now))
    }

    @Test
    fun wrongAgainStartsOver() {
        val records = listOf(at(AnswerResult.WRONG, 30), at(AnswerResult.CORRECT, 20), at(AnswerResult.WRONG, 5))

        assertEquals(ReviewStatus.DUE, reviewStatus(records, now))
    }

    @Test
    fun skippedThenCorrectIsReviewedLikeAWrongAnswer() {
        // 건너뛴 뒤 맞히면 바로 끝나지 않고 3일 뒤에 다시 냄
        val waiting = listOf(at(AnswerResult.SKIPPED, 5), at(AnswerResult.CORRECT, 2))
        val due = listOf(at(AnswerResult.SKIPPED, 5), at(AnswerResult.CORRECT, 3))

        assertEquals(ReviewStatus.WAITING, reviewStatus(waiting, now))
        assertEquals(ReviewStatus.DUE, reviewStatus(due, now))
    }

    @Test
    fun skippingAfterCorrectStartsOver() {
        // 틀린 뒤 맞히고, 건너뛴 다음 다시 맞힘: 건너뛴 뒤로 맞힌 횟수는 1번이라 3일 간격
        val records = listOf(
            at(AnswerResult.WRONG, 30),
            at(AnswerResult.CORRECT, 20),
            at(AnswerResult.SKIPPED, 15),
            at(AnswerResult.CORRECT, 2),
        )

        assertEquals(ReviewStatus.WAITING, reviewStatus(records, now))
    }

    @Test
    fun recordOrderDoesNotMatter() {
        val records = listOf(at(AnswerResult.CORRECT, 4), at(AnswerResult.WRONG, 10))

        assertEquals(ReviewStatus.DUE, reviewStatus(records, now))
    }

    @Test
    fun customIntervalsAreUsed() {
        val config = ReviewConfig(retryAfterMissDays = 2, afterCorrectDays = listOf(5))
        val wrong = listOf(at(AnswerResult.WRONG, 1))
        val corrected = listOf(at(AnswerResult.WRONG, 10), at(AnswerResult.CORRECT, 4))

        assertEquals(ReviewStatus.WAITING, reviewStatus(wrong, now, config))
        assertEquals(ReviewStatus.WAITING, reviewStatus(corrected, now, config))
        assertEquals(ReviewStatus.MASTERED, reviewStatus(corrected + at(AnswerResult.CORRECT, 0), now, config))
    }
}
