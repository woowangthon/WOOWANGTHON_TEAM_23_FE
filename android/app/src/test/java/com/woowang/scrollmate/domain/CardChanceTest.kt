package com.woowang.scrollmate.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CardChanceTest {
    private val config = CardChanceConfig(
        baseChance = 0.10,
        maxChance = 0.20,
        boostStartMinutes = 10,
        rampMinutes = 10,
    )

    private fun minutes(m: Double) = (m * 60_000).toLong()

    private fun chanceAt(m: Double) = cardChance(minutes(m), config)

    @Test
    fun startsAtBaseChance() {
        assertEquals(0.10, chanceAt(0.0), 1e-9)
    }

    @Test
    fun staysAtBaseChanceBeforeTenMinutes() {
        assertEquals(0.10, chanceAt(5.0), 1e-9)
        assertEquals(0.10, chanceAt(9.99), 1e-9)
    }

    @Test
    fun startsRisingAfterTenMinutes() {
        assertEquals(0.10, chanceAt(10.0), 1e-9)
        assertEquals(0.11, chanceAt(11.0), 1e-9)
    }

    @Test
    fun risesGraduallyAndLinearly() {
        assertEquals(0.15, chanceAt(15.0), 1e-9)
        assertEquals(0.19, chanceAt(19.0), 1e-9)
    }

    @Test
    fun reachesMaxChanceAndStaysThere() {
        assertEquals(0.20, chanceAt(20.0), 1e-9)
        assertEquals(0.20, chanceAt(60.0), 1e-9)
    }

    @Test
    fun negativeElapsedTimeFallsBackToBaseChance() {
        assertEquals(0.10, cardChance(-1_000, config), 1e-9)
    }

    @Test
    fun defaultConfigMatchesTheDecidedRule() {
        // FE #13: 10분 전 10%, 10분 이후 20%까지 서서히
        assertEquals(0.10, cardChance(minutes(9.0)), 1e-9)
        assertEquals(0.20, cardChance(minutes(25.0)), 1e-9)
    }

    @Test
    fun respectsCustomConfig() {
        val custom = CardChanceConfig(baseChance = 0.05, maxChance = 0.50, boostStartMinutes = 2, rampMinutes = 4)

        assertEquals(0.05, cardChance(minutes(2.0), custom), 1e-9)
        assertEquals(0.275, cardChance(minutes(4.0), custom), 1e-9)
        assertEquals(0.50, cardChance(minutes(6.0), custom), 1e-9)
    }
}
