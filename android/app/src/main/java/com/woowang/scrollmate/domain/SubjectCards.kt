package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.Material
import com.woowang.scrollmate.data.MaterialStatus
import com.woowang.scrollmate.data.Subject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 홈 화면의 학습 주제 카드 하나. subject가 null이면 학습 주제가 없는 자료를 모은 "미분류" 카드
data class SubjectCard(
    val subject: Subject?,
    val materialCount: Int,
    // 시험일까지 남은 날짜. 시험일이 없으면 null, 지났으면 음수
    val daysLeft: Long?,
)

// 보여 줄 순서: 시험일이 다가오는 주제(가까운 순), 시험일이 없는 주제(이름순), 시험일이 지난 주제, 미분류.
// 실패한 자료는 쓸 수 없으므로 자료 수에 세지 않음
fun buildSubjectCards(subjects: List<Subject>, materials: List<Material>, today: LocalDate): List<SubjectCard> {
    val usable = materials.filter { it.status != MaterialStatus.FAILED }
    val countBySubject = usable.groupingBy { it.subjectId }.eachCount()

    val cards = subjects.map { subject ->
        SubjectCard(
            subject = subject,
            materialCount = countBySubject[subject.id] ?: 0,
            daysLeft = subject.examDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { ChronoUnit.DAYS.between(today, it) },
        )
    }.sortedWith(
        compareBy<SubjectCard> { rank(it.daysLeft) }
            .thenBy { if (it.daysLeft != null && it.daysLeft >= 0) it.daysLeft else 0L }
            .thenBy { it.subject?.name.orEmpty() },
    )

    val unclassified = countBySubject[null] ?: 0
    return if (unclassified > 0) cards + SubjectCard(null, unclassified, null) else cards
}

private fun rank(daysLeft: Long?) = when {
    daysLeft == null -> 1
    daysLeft >= 0 -> 0
    else -> 2
}
