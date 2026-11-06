package com.woowang.scrollmate.domain

// 카드 출제 정책 값. 임의로 정한 값이므로 시연하며 조정함
data class WeightConfig(
    // 시험 며칠 전부터 가중치를 올리기 시작하는지
    val windowDays: Int = 14,
    // 시험 당일의 가중치 (기본 가중치는 1)
    val maxWeight: Double = 5.0,
)

// 쇼츠를 넘길 때 카드가 뜰 확률 규칙 (FE #13). 쇼츠를 계속 본 시간이 길어지면 확률이 올라감
data class CardChanceConfig(
    // 기본 확률. 쇼츠를 본 지 boostStartMinutes분이 될 때까지 유지함
    val baseChance: Double = 0.10,
    // 시간이 지나 올라갈 수 있는 최대 확률
    val maxChance: Double = 0.20,
    // 쇼츠를 계속 본 지 몇 분이 지나면 확률을 올리기 시작하는지
    val boostStartMinutes: Int = 10,
    // 올리기 시작해서 최대 확률에 닿을 때까지 걸리는 시간. 임의로 정한 값이라 시연하며 조정함
    val rampMinutes: Int = 10,
)
