package com.woowang.scrollmate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityStatusTest {
    private val pkg = "com.woowang.scrollmate"
    private val cls = "com.woowang.scrollmate.ScrollInterruptService"

    @Test
    fun emptyOrNullListMeansOff() {
        assertFalse(isServiceInList(null, pkg, cls))
        assertFalse(isServiceInList("", pkg, cls))
        assertFalse(isServiceInList("   ", pkg, cls))
    }

    @Test
    fun fullClassNameMatches() {
        assertTrue(isServiceInList("$pkg/$cls", pkg, cls))
    }

    @Test
    fun shortClassNameMatches() {
        assertTrue(isServiceInList("$pkg/.ScrollInterruptService", pkg, cls))
    }

    @Test
    fun serviceAmongOthersMatches() {
        val list = "com.other.app/.OtherService:$pkg/.ScrollInterruptService:com.third/com.third.Svc"
        assertTrue(isServiceInList(list, pkg, cls))
    }

    @Test
    fun otherServicesOnlyMeansOff() {
        assertFalse(isServiceInList("com.other.app/.OtherService:com.third/com.third.Svc", pkg, cls))
    }

    @Test
    fun samePackageDifferentServiceDoesNotMatch() {
        assertFalse(isServiceInList("$pkg/.AnotherService", pkg, cls))
    }

    @Test
    fun malformedEntriesAreIgnored() {
        assertFalse(isServiceInList("garbage::$pkg", pkg, cls))
    }

    @Test
    fun matchingIgnoresCase() {
        assertTrue(isServiceInList("COM.WOOWANG.SCROLLMATE/.scrollinterruptservice", pkg, cls))
    }
}
