package com.woowang.scrollmate.data

import com.woowang.scrollmate.domain.SubjectCandidate
import com.woowang.scrollmate.domain.pickSubject
import java.time.LocalDate
import kotlin.random.Random

// 카드에 띄울 문제를 고름. 시험일 가중치로 학습 주제를 먼저 고르고, 그 안에서 안 푼 문제를 우선함.
// 출제할 문제가 하나도 없으면 null
class CardQuestionSource(
    private val subjects: SubjectDao,
    private val questions: QuestionDao,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val roll: () -> Double = { Random.nextDouble() },
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
        return questions.randomUnanswered(picked.key) ?: questions.randomAny(picked.key)
    }
}
