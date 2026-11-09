package com.woowang.scrollmate.data

import com.woowang.scrollmate.domain.ReviewConfig
import com.woowang.scrollmate.domain.ReviewRecord
import com.woowang.scrollmate.domain.ReviewStatus
import com.woowang.scrollmate.domain.SubjectCandidate
import com.woowang.scrollmate.domain.pickSubject
import com.woowang.scrollmate.domain.reviewStatus
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.random.Random

// 카드에 띄울 문제를 고름. 시험일 가중치로 학습 주제를 먼저 고르고, 그 안에서는
// 다시 낼 때가 된 문제(틀린 문제), 아직 안 푼 문제 순으로 고름. 둘 다 없으면 그 주제의 문제 아무거나 냄.
// 출제할 문제가 하나도 없으면 null
class CardQuestionSource(
    private val subjects: SubjectDao,
    private val questions: QuestionDao,
    private val answers: AnswerRecordDao,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Instant = { Instant.now() },
    private val roll: () -> Double = { Random.nextDouble() },
    // 후보가 n개일 때 몇 번째를 고를지
    private val pickIndex: (Int) -> Int = { Random.nextInt(it) },
    private val reviewConfig: ReviewConfig = ReviewConfig(),
) {
    suspend fun next(): Question? {
        val subjectsById = subjects.getAll().associateBy { it.id }
        val candidates = questions.subjectIdsWithQuestions().distinct().map { id ->
            val subject = id?.let { subjectsById[it] }
            SubjectCandidate(
                key = id,
                examDate = subject?.examDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                enabled = subject?.enabled ?: true,
            )
        }
        val picked = pickSubject(candidates, today(), roll()) ?: return null
        return pickQuestion(picked.key)
    }

    private suspend fun pickQuestion(subjectId: Long?): Question? {
        val all = questions.questionsOfSubject(subjectId)
        if (all.isEmpty()) return null
        val recordsById = answers.recordsOfSubject(subjectId).groupBy { it.questionId }
        val at = now()
        val byStatus = all.groupBy { question ->
            val records = recordsById[question.id].orEmpty().mapNotNull { it.toReviewRecord() }
            reviewStatus(records, at, reviewConfig)
        }
        val candidates = byStatus[ReviewStatus.DUE] ?: byStatus[ReviewStatus.NEW] ?: all
        return candidates[pickIndex(candidates.size)]
    }
}

// 저장된 시각이 깨졌으면 그 기록은 건너뜀. 다시 내는 시점이 조금 어긋날 뿐 앱이 멈추지 않게 함
private fun AnswerRecord.toReviewRecord(): ReviewRecord? =
    runCatching { ReviewRecord(result, OffsetDateTime.parse(answeredAt).toInstant()) }.getOrNull()
