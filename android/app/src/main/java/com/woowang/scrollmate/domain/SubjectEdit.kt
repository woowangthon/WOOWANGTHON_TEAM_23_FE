package com.woowang.scrollmate.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

const val SUBJECT_NAME_MAX_LENGTH = 30

enum class SubjectNameCheck { OK, BLANK, TOO_LONG, DUPLICATE }

// 학습 주제 이름을 저장해도 되는지 봄. otherNames는 수정 중인 주제를 뺀 나머지 이름들.
// 앞뒤 공백과 대소문자 차이는 같은 이름으로 봄
fun checkSubjectName(name: String, otherNames: List<String>): SubjectNameCheck {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> SubjectNameCheck.BLANK
        trimmed.length > SUBJECT_NAME_MAX_LENGTH -> SubjectNameCheck.TOO_LONG
        otherNames.any { it.trim().equals(trimmed, ignoreCase = true) } -> SubjectNameCheck.DUPLICATE
        else -> SubjectNameCheck.OK
    }
}

// 날짜 선택 창은 날짜를 UTC 자정 밀리초로 주고받음. 시간대 때문에 하루가 밀리지 않게 UTC로 바꿈
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun pickerMillisToDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
