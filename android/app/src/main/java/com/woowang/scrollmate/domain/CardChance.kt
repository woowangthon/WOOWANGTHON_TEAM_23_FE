package com.woowang.scrollmate.domain

private const val MILLIS_PER_MINUTE = 60_000.0

// 쇼츠를 계속 본 시간(밀리초)에 따른 카드 등장 확률.
// 시작 후 boostStartMinutes분까지는 baseChance, 그 뒤 rampMinutes분 동안 직선으로 maxChance까지 올리고 이후 유지함.
// 쇼츠를 나갔다 들어오면 호출하는 쪽에서 시간을 0부터 다시 셈
fun cardChance(elapsedMillis: Long, config: CardChanceConfig = CardChanceConfig()): Double {
    val minutesPastStart = elapsedMillis / MILLIS_PER_MINUTE - config.boostStartMinutes
    if (minutesPastStart <= 0.0) return config.baseChance
    val progress = (minutesPastStart / config.rampMinutes).coerceIn(0.0, 1.0)
    return config.baseChance + (config.maxChance - config.baseChance) * progress
}
