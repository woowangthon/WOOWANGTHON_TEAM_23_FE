package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.Material
import com.woowang.scrollmate.data.MaterialStatus
import com.woowang.scrollmate.data.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SubjectCardsTest {
    private val today = LocalDate.of(2026, 11, 9)

    private fun subject(id: Long, name: String, examDate: String? = null, enabled: Boolean = true) =
        Subject(id = id, name = name, examDate = examDate, enabled = enabled)

    private fun material(id: String, subjectId: Long?, status: MaterialStatus = MaterialStatus.READY) =
        Material(id = id, name = "$id.pdf", subjectId = subjectId, status = status)

    @Test
    fun noSubjectsAndNoMaterialsGivesNoCards() {
        assertTrue(buildSubjectCards(emptyList(), emptyList(), today).isEmpty())
    }

    @Test
    fun countsMaterialsPerSubject() {
        val cards = buildSubjectCards(
            listOf(subject(1, "토익"), subject(2, "운전면허")),
            listOf(material("a", 1), material("b", 1), material("c", 2)),
            today,
        )

        assertEquals(mapOf("토익" to 2, "운전면허" to 1), cards.associate { it.subject!!.name to it.materialCount })
    }

    @Test
    fun failedMaterialsAreNotCounted() {
        val cards = buildSubjectCards(
            listOf(subject(1, "토익")),
            listOf(material("a", 1), material("b", 1, MaterialStatus.FAILED), material("c", 1, MaterialStatus.PROCESSING)),
            today,
        )

        assertEquals(2, cards.single().materialCount)
    }

    @Test
    fun subjectWithoutMaterialsStillShowsWithZero() {
        val cards = buildSubjectCards(listOf(subject(1, "토익")), emptyList(), today)

        assertEquals(0, cards.single().materialCount)
    }

    @Test
    fun unclassifiedCardAppearsOnlyWhenMaterialsHaveNoSubject() {
        val without = buildSubjectCards(listOf(subject(1, "토익")), listOf(material("a", 1)), today)
        val with = buildSubjectCards(listOf(subject(1, "토익")), listOf(material("a", 1), material("b", null)), today)

        assertTrue(without.none { it.subject == null })
        assertNull(with.last().subject)
        assertEquals(1, with.last().materialCount)
    }

    @Test
    fun daysLeftIsCountedFromToday() {
        val cards = buildSubjectCards(
            listOf(subject(1, "당일", "2026-11-09"), subject(2, "열흘", "2026-11-19"), subject(3, "지남", "2026-11-07")),
            emptyList(),
            today,
        )

        assertEquals(mapOf("당일" to 0L, "열흘" to 10L, "지남" to -2L), cards.associate { it.subject!!.name to it.daysLeft })
    }

    @Test
    fun missingOrBrokenExamDateGivesNoDaysLeft() {
        val cards = buildSubjectCards(listOf(subject(1, "없음"), subject(2, "깨짐", "내일")), emptyList(), today)

        assertTrue(cards.all { it.daysLeft == null })
    }

    @Test
    fun cardsAreOrderedUpcomingExamsThenNoExamThenPastThenUnclassified() {
        val cards = buildSubjectCards(
            listOf(
                subject(1, "가_시험없음"),
                subject(2, "나_지남", "2026-11-01"),
                subject(3, "다_30일", "2026-12-09"),
                subject(4, "라_3일", "2026-11-12"),
            ),
            listOf(material("a", null)),
            today,
        )

        assertEquals(listOf("라_3일", "다_30일", "가_시험없음", "나_지남", null), cards.map { it.subject?.name })
    }
}
