package com.woowang.scrollmate.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val BASE_WEIGHT = 1.0

// 과목 하나. key가 null이면 미분류
data class SubjectCandidate(
    val key: Long?,
    val examDate: LocalDate?,
    val enabled: Boolean,
)

// 시험일이 없거나 윈도우보다 멀면 1, 가까울수록 maxWeight까지 선형으로 증가함.
// 꺼진 과목과 시험일이 지난 과목은 0
fun subjectWeight(
    candidate: SubjectCandidate,
    today: LocalDate,
    config: WeightConfig = WeightConfig(),
): Double {
    if (!candidate.enabled) return 0.0
    val exam = candidate.examDate ?: return BASE_WEIGHT
    val daysLeft = ChronoUnit.DAYS.between(today, exam)
    if (daysLeft < 0) return 0.0
    val closeness = ((config.windowDays - daysLeft).toDouble() / config.windowDays).coerceIn(0.0, 1.0)
    return BASE_WEIGHT + (config.maxWeight - BASE_WEIGHT) * closeness
}

// roll은 0 이상 1 미만의 값. 가중치에 비례해 과목을 고름. 고를 과목이 없으면 null
fun pickSubject(
    candidates: List<SubjectCandidate>,
    today: LocalDate,
    roll: Double,
    config: WeightConfig = WeightConfig(),
): SubjectCandidate? {
    val weighted = candidates
        .map { it to subjectWeight(it, today, config) }
        .filter { it.second > 0.0 }
    if (weighted.isEmpty()) return null
    var target = roll.coerceIn(0.0, 1.0) * weighted.sumOf { it.second }
    for ((candidate, weight) in weighted) {
        if (target < weight) return candidate
        target -= weight
    }
    return weighted.last().first
}
