package com.woowang.scrollmate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SubjectWeightsTest {
    private val today = LocalDate.of(2026, 11, 3)
    private val config = WeightConfig(windowDays = 14, maxWeight = 5.0)

    private fun subject(key: Long?, daysLeft: Long?, enabled: Boolean = true) =
        SubjectCandidate(key, daysLeft?.let { today.plusDays(it) }, enabled)

    private fun weight(c: SubjectCandidate) = subjectWeight(c, today, config)

    @Test
    fun noExamDateGivesBaseWeight() {
        assertEquals(1.0, weight(subject(1, null)), 1e-9)
    }

    @Test
    fun farExamGivesBaseWeight() {
        assertEquals(1.0, weight(subject(1, 14)), 1e-9)
        assertEquals(1.0, weight(subject(1, 60)), 1e-9)
    }

    @Test
    fun examDayGivesMaxWeight() {
        assertEquals(5.0, weight(subject(1, 0)), 1e-9)
    }

    @Test
    fun weightGrowsLinearlyInWindow() {
        assertEquals(3.0, weight(subject(1, 7)), 1e-9)
    }

    @Test
    fun pastExamGivesZero() {
        assertEquals(0.0, weight(subject(1, -1)), 1e-9)
    }

    @Test
    fun disabledSubjectGivesZero() {
        assertEquals(0.0, weight(subject(1, 0, enabled = false)), 1e-9)
    }

    @Test
    fun nothingPickableReturnsNull() {
        val list = listOf(subject(1, -3), subject(2, 0, enabled = false))
        assertNull(pickSubject(list, today, 0.5, config))
        assertNull(pickSubject(emptyList(), today, 0.5, config))
    }

    @Test
    fun pickIsProportionalToWeight() {
        // A=1 (far exam), B=5 (exam day). total 6, boundary at roll 1/6
        val a = subject(1, 60)
        val b = subject(2, 0)
        val list = listOf(a, b)
        assertEquals(a, pickSubject(list, today, 0.0, config))
        assertEquals(a, pickSubject(list, today, 0.16, config))
        assertEquals(b, pickSubject(list, today, 0.17, config))
        assertEquals(b, pickSubject(list, today, 0.999, config))
    }

    @Test
    fun disabledSubjectIsNeverPicked() {
        val off = subject(1, 0, enabled = false)
        val on = subject(2, null)
        for (roll in listOf(0.0, 0.5, 0.99)) {
            assertEquals(on, pickSubject(listOf(off, on), today, roll, config))
        }
    }

    @Test
    fun unclassifiedCanBePicked() {
        val unclassified = subject(null, null)
        assertEquals(unclassified, pickSubject(listOf(unclassified), today, 0.5, config))
    }
}
