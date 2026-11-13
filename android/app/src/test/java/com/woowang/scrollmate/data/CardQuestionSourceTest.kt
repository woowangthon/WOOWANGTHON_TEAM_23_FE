package com.woowang.scrollmate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

private class FakeSubjectDao(private val rows: List<Subject>) : SubjectDao {
    override fun observeAll(): Flow<List<Subject>> = flowOf(rows)
    override suspend fun getAll(): List<Subject> = rows
    override suspend fun insert(subject: Subject): Long = error("unused")
    override suspend fun update(subject: Subject) = error("unused")
    override suspend fun delete(subject: Subject) = error("unused")
}

// 과목 id(null = 미분류)별 문제 목록
private class FakeCardQuestionDao(private val bySubject: Map<Long?, List<Question>>) : QuestionDao {
    override suspend fun upsertAll(questions: List<Question>) = error("unused")
    override suspend fun subjectIdsWithQuestions(): List<Long?> = bySubject.filterValues { it.isNotEmpty() }.keys.toList()
    override suspend fun questionsOfSubject(subjectId: Long?): List<Question> = bySubject[subjectId].orEmpty()
}

private class FakeAnswerRecordDao(private val records: List<AnswerRecord>) : AnswerRecordDao {
    override suspend fun insert(record: AnswerRecord): Long = error("unused")
    override suspend fun recordsOfSubject(subjectId: Long?): List<AnswerRecord> = records
    override suspend fun getAll(): List<AnswerRecord> = records
}

private fun question(id: String) = Question(id, "m", QuestionType.OX, "문장", "O", emptyList(), "근거")

class CardQuestionSourceTest {
    private val today = LocalDate.of(2026, 11, 7)
    private val now = Instant.parse("2026-11-09T12:00:00Z")

    private fun record(questionId: String, result: AnswerResult, daysAgo: Long) = AnswerRecord(
        questionId = questionId,
        result = result,
        answeredAt = now.minus(Duration.ofDays(daysAgo)).atOffset(java.time.ZoneOffset.UTC).toString(),
    )

    private fun source(
        subjects: List<Subject>,
        bySubject: Map<Long?, List<Question>>,
        records: List<AnswerRecord> = emptyList(),
        roll: Double = 0.0,
    ) = CardQuestionSource(
        subjects = FakeSubjectDao(subjects),
        questions = FakeCardQuestionDao(bySubject),
        answers = FakeAnswerRecordDao(records),
        today = { today },
        now = { now },
        roll = { roll },
        pickIndex = { 0 },
    )

    @Test
    fun `문제가 하나도 없으면 null이다`() = runBlocking {
        assertNull(source(emptyList(), emptyMap()).next())
    }

    @Test
    fun `미분류 문제도 출제한다`() = runBlocking {
        val s = source(emptyList(), mapOf(null to listOf(question("u"))))
        assertEquals("u", s.next()?.id)
    }

    @Test
    fun `꺼진 학습 주제의 문제는 내지 않는다`() = runBlocking {
        val off = Subject(id = 1, name = "토익", enabled = false)
        assertNull(source(listOf(off), mapOf(1L to listOf(question("off")))).next())
    }

    @Test
    fun `시험일이 지난 학습 주제의 문제는 내지 않는다`() = runBlocking {
        val past = Subject(id = 1, name = "운전면허", examDate = "2026-11-01")
        assertNull(source(listOf(past), mapOf(1L to listOf(question("past")))).next())
    }

    @Test
    fun `시험이 임박한 학습 주제가 더 자주 뽑힌다`() = runBlocking {
        val soon = Subject(id = 1, name = "토익", examDate = "2026-11-07") // 가중치 5
        val none = Subject(id = 2, name = "수업") // 가중치 1
        val bySubject: Map<Long?, List<Question>> = mapOf(1L to listOf(question("soon")), 2L to listOf(question("none")))
        // 합 6 중 앞쪽 5/6 구간이면 soon, 그 뒤면 none
        assertEquals("soon", source(listOf(soon, none), bySubject, roll = 0.5).next()?.id)
        assertEquals("none", source(listOf(soon, none), bySubject, roll = 0.9).next()?.id)
    }

    @Test
    fun `안 푼 문제를 이미 푼 문제보다 먼저 낸다`() = runBlocking {
        // q1은 틀린 적 없이 맞힘(더 내지 않음), q2는 안 풀었음
        val records = listOf(record("q1", AnswerResult.CORRECT, daysAgo = 5))
        val s = source(emptyList(), mapOf(null to listOf(question("q1"), question("q2"))), records)

        assertEquals("q2", s.next()?.id)
    }

    @Test
    fun `틀린 지 하루가 지난 문제를 안 푼 문제보다 먼저 낸다`() = runBlocking {
        val records = listOf(record("wrong", AnswerResult.WRONG, daysAgo = 2))
        val s = source(emptyList(), mapOf(null to listOf(question("new"), question("wrong"))), records)

        assertEquals("wrong", s.next()?.id)
    }

    @Test
    fun `방금 틀린 문제는 기다렸다가 안 푼 문제를 먼저 낸다`() = runBlocking {
        val records = listOf(record("wrong", AnswerResult.WRONG, daysAgo = 0))
        val s = source(emptyList(), mapOf(null to listOf(question("wrong"), question("new"))), records)

        assertEquals("new", s.next()?.id)
    }

    @Test
    fun `낼 문제가 모두 끝났거나 기다리는 중이면 그 주제의 문제 아무거나 낸다`() = runBlocking {
        val records = listOf(
            record("done", AnswerResult.CORRECT, daysAgo = 3),
            record("wait", AnswerResult.WRONG, daysAgo = 0),
        )
        val s = source(emptyList(), mapOf(null to listOf(question("done"), question("wait"))), records)

        assertEquals("done", s.next()?.id)
    }

    @Test
    fun `틀린 뒤 한 번 맞힌 문제는 3일 뒤에 다시 낸다`() = runBlocking {
        val bySubject: Map<Long?, List<Question>> = mapOf(null to listOf(question("again"), question("new")))
        val recent = listOf(
            record("again", AnswerResult.WRONG, daysAgo = 10),
            record("again", AnswerResult.CORRECT, daysAgo = 2),
        )
        val due = listOf(
            record("again", AnswerResult.WRONG, daysAgo = 10),
            record("again", AnswerResult.CORRECT, daysAgo = 4),
        )

        assertEquals("new", source(emptyList(), bySubject, recent).next()?.id)
        assertEquals("again", source(emptyList(), bySubject, due).next()?.id)
    }
}
