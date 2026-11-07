package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.AnswerResult
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class GradingTest {
    private val ox = Question("q1", "m1", QuestionType.OX, "문장", "O", emptyList(), "근거")
    private val short = Question(
        "q2", "m1", QuestionType.SHORT, "질문", "페이징", listOf("페이징", "paging"), "근거",
    )

    @Test
    fun `OX는 정답과 같으면 맞다`() {
        assertEquals(AnswerResult.CORRECT, grade(ox, "O"))
        assertEquals(AnswerResult.WRONG, grade(ox, "X"))
    }

    @Test
    fun `OX는 소문자도 같은 답으로 본다`() {
        assertEquals(AnswerResult.CORRECT, grade(ox, "o"))
    }

    @Test
    fun `단답은 정답과 같으면 맞다`() {
        assertEquals(AnswerResult.CORRECT, grade(short, "페이징"))
    }

    @Test
    fun `단답은 허용 답안도 맞다`() {
        assertEquals(AnswerResult.CORRECT, grade(short, "paging"))
    }

    @Test
    fun `단답은 공백과 대소문자를 무시한다`() {
        assertEquals(AnswerResult.CORRECT, grade(short, "  Pa ging "))
    }

    @Test
    fun `단답이 다르면 틀리다`() {
        assertEquals(AnswerResult.WRONG, grade(short, "세그먼테이션"))
    }

    @Test
    fun `단답을 비워 두면 틀리다`() {
        assertEquals(AnswerResult.WRONG, grade(short, "   "))
    }
}
