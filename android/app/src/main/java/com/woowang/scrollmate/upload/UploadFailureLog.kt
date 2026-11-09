package com.woowang.scrollmate.upload

import android.content.Context
import com.woowang.scrollmate.domain.UploadFailure
import com.woowang.scrollmate.domain.UploadFailureReason
import com.woowang.scrollmate.domain.decodeFailures
import com.woowang.scrollmate.domain.encodeFailures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "upload_failures"
private const val KEY_FAILURES = "failures"
private const val MAX_FAILURES = 20

// 백그라운드 업로드가 실패한 기록. 화면이 닫혀 있는 동안 실패해도 앱을 열었을 때 알려 주려고
// Room 스키마를 바꾸지 않고 앱 안 저장소에 둠. 사용자가 확인하면 지움
class UploadFailureLog(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _failures = MutableStateFlow(decodeFailures(prefs.getString(KEY_FAILURES, null)))

    // 최근 실패가 앞에 옴
    val failures: StateFlow<List<UploadFailure>> = _failures

    @Synchronized
    fun record(fileNames: List<String>, reason: UploadFailureReason) {
        val entry = UploadFailure(id = System.currentTimeMillis(), fileNames = fileNames, reason = reason)
        save((listOf(entry) + _failures.value).take(MAX_FAILURES))
    }

    @Synchronized
    fun dismiss(id: Long) = save(_failures.value.filterNot { it.id == id })

    @Synchronized
    fun dismissAll() = save(emptyList())

    private fun save(list: List<UploadFailure>) {
        _failures.value = list
        prefs.edit().putString(KEY_FAILURES, encodeFailures(list)).apply()
    }
}
