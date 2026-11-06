package com.woowang.scrollmate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

private class FakeSubjectDao(private val rows: List<Subject>) : SubjectDao {
    override fun observeAll(): Flow<List<Subject>> = flowOf(rows)
    override suspend fun getAll(): List<Subject> = rows
    override suspend fun insert(subject: Subject): Long = error("unused")
    override suspend fun update(subject: Subject) = error("unused")
    override suspend fun delete(subject: Subject) = error("unused")
}

// 과목 id(null = 미분류)별로 안 푼 문제와 푼 문제를 따로 들고 있음
private class FakeCardQuestionDao(
    private val unanswered: Map<Long?, Question> = emptyMap(),
    private val any: Map<Long?, Question> = emptyMap(),
) : QuestionDao {
    override suspend fun upsertAll(questions: List<Question>) = error("unused")
    override suspend fun subjectIdsWithQuestions(): List<Long?> = (unanswered.keys + any.keys).toList()
    override suspend fun randomUnanswered(subjectId: Long?): Question? = unanswered[subjectId]
    override suspend fun randomAny(subjectId: Long?): Question? = any[subjectId]
}

private fun question(id: String) = Question(id, "m", QuestionType.OX, "문장", "O", emptyList(), "근거")

class CardQuestionSourceTest {
    private val today = LocalDate.of(2026, 11, 7)

    private fun source(subjects: List<Subject>, dao: QuestionDao, roll: Double = 0.0) =
        CardQuestionSource(FakeSubjectDao(subjects), dao, today = { today }, roll = { roll })

    @Test
    fun `문제가 하나도 없으면 null이다`() = runBlocking {
        assertNull(source(emptyList(), FakeCardQuestionDao()).next())
    }

    @Test
    fun `미분류 문제도 출제한다`() = runBlocking {
        val dao = FakeCardQuestionDao(unanswered = mapOf(null to question("u")))
        assertEquals("u", source(emptyList(), dao).next()?.id)
    }

    @Test
    fun `안 푼 문제가 없으면 푼 문제에서 고른다`() = runBlocking {
        val dao = FakeCardQuestionDao(any = mapOf(null to question("old")))
        assertEquals("old", source(emptyList(), dao).next()?.id)
    }

    @Test
    fun `꺼진 학습 주제의 문제는 내지 않는다`() = runBlocking {
        val off = Subject(id = 1, name = "토익", enabled = false)
        val dao = FakeCardQuestionDao(unanswered = mapOf(1L to question("off")))
        assertNull(source(listOf(off), dao).next())
    }

    @Test
    fun `시험일이 지난 학습 주제의 문제는 내지 않는다`() = runBlocking {
        val past = Subject(id = 1, name = "운전면허", examDate = "2026-11-01")
        val dao = FakeCardQuestionDao(unanswered = mapOf(1L to question("past")))
        assertNull(source(listOf(past), dao).next())
    }

    @Test
    fun `시험이 임박한 학습 주제가 더 자주 뽑힌다`() = runBlocking {
        val soon = Subject(id = 1, name = "토익", examDate = "2026-11-07") // 가중치 5
        val none = Subject(id = 2, name = "수업") // 가중치 1
        val dao = FakeCardQuestionDao(unanswered = mapOf(1L to question("soon"), 2L to question("none")))
        // 합 6 중 앞쪽 5/6 구간이면 soon, 그 뒤면 none
        assertEquals("soon", source(listOf(soon, none), dao, roll = 0.5).next()?.id)
        assertEquals("none", source(listOf(soon, none), dao, roll = 0.9).next()?.id)
    }
}
