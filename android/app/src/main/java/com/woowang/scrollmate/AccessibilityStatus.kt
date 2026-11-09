package com.woowang.scrollmate

import android.content.Context
import android.content.Intent
import android.provider.Settings

// 설정에 저장된 켜진 접근성 서비스 목록(콜론으로 구분)에 우리 서비스가 있는지 봄.
// 항목은 "패키지/클래스" 형태이고 클래스는 ".이름"처럼 줄여 쓸 수도 있어서 둘 다 같은 것으로 비교함
fun isServiceInList(enabledServices: String?, packageName: String, className: String): Boolean {
    if (enabledServices.isNullOrBlank()) return false
    val expected = "$packageName/$className".lowercase()
    return enabledServices.split(':').any { entry ->
        val parts = entry.trim().split('/')
        if (parts.size != 2) return@any false
        val component = if (parts[1].startsWith('.')) "${parts[0]}/${parts[0]}${parts[1]}" else entry.trim()
        component.lowercase() == expected
    }
}

// 접근성 서비스가 설정에서 켜져 있는지. 설정에는 켜져 있어도 서비스가 연결되지 않은 경우(FE #5)는 알 수 없음
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    val accessibilityOn = Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
    val enabledServices = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return accessibilityOn &&
        isServiceInList(enabledServices, context.packageName, ScrollInterruptService::class.java.name)
}

fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
