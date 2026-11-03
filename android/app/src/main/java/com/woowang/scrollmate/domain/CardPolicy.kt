package com.woowang.scrollmate.domain

// 카드 출제 정책 값. 임의로 정한 값이므로 시연하며 조정함
data class WeightConfig(
    // 시험 며칠 전부터 가중치를 올리기 시작하는지
    val windowDays: Int = 14,
    // 시험 당일의 가중치 (기본 가중치는 1)
    val maxWeight: Double = 5.0,
)
