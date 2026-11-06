package com.woowang.scrollmate.data

import android.content.Context
import java.util.UUID

private const val PREFS_NAME = "scrollmate_device"
private const val KEY_DEVICE_ID = "device_id"

// 로그인이 없으므로 처음 실행할 때 만든 무작위 ID를 기기 식별에 씀 (api.md의 X-Device-Id)
class DeviceId(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // lazy는 여러 스레드에서 동시에 처음 불러도 한 번만 만들어서 서로 다른 ID가 나가지 않음
    private val id: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    fun get(): String = id
}
