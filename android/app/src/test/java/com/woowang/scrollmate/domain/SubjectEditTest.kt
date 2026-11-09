package com.woowang.scrollmate.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SubjectEditTest {
    @Test
    fun normalNameIsOk() {
        assertEquals(SubjectNameCheck.OK, checkSubjectName("토익", listOf("운전면허")))
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(SubjectNameCheck.BLANK, checkSubjectName("", emptyList()))
        assertEquals(SubjectNameCheck.BLANK, checkSubjectName("   ", emptyList()))
    }

    @Test
    fun nameAtLimitIsOkAndOverLimitIsRejected() {
        assertEquals(SubjectNameCheck.OK, checkSubjectName("가".repeat(SUBJECT_NAME_MAX_LENGTH), emptyList()))
        assertEquals(SubjectNameCheck.TOO_LONG, checkSubjectName("가".repeat(SUBJECT_NAME_MAX_LENGTH + 1), emptyList()))
    }

    @Test
    fun lengthIsCountedWithoutSurroundingSpaces() {
        assertEquals(SubjectNameCheck.OK, checkSubjectName("  " + "가".repeat(SUBJECT_NAME_MAX_LENGTH) + "  ", emptyList()))
    }

    @Test
    fun sameNameAsAnotherSubjectIsRejected() {
        assertEquals(SubjectNameCheck.DUPLICATE, checkSubjectName("토익", listOf("운전면허", "토익")))
    }

    @Test
    fun duplicateCheckIgnoresCaseAndSurroundingSpaces() {
        assertEquals(SubjectNameCheck.DUPLICATE, checkSubjectName(" toeic ", listOf("TOEIC")))
    }

    @Test
    fun keepingOwnNameIsOkWhenOthersAreChecked() {
        // 수정 중인 주제의 이름은 otherNames에 넣지 않으므로 이름을 그대로 두고 저장할 수 있음
        assertEquals(SubjectNameCheck.OK, checkSubjectName("토익", listOf("운전면허")))
    }

    @Test
    fun pickerMillisRoundTripKeepsTheSameDay() {
        val date = LocalDate.of(2026, 11, 20)

        assertEquals(date, pickerMillisToDate(date.toPickerMillis()))
    }

    @Test
    fun pickerMillisIsUtcMidnight() {
        assertEquals(0L, LocalDate.of(1970, 1, 1).toPickerMillis())
        assertEquals(LocalDate.of(1970, 1, 2), pickerMillisToDate(86_400_000L))
    }
}
