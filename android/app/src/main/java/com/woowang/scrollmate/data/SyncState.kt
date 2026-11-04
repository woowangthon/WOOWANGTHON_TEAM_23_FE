package com.woowang.scrollmate.data

import android.content.Context

private const val PREFS_NAME = "scrollmate_sync"
private const val KEY_LAST_QUESTION_SYNC = "last_question_sync"

// 서버와 마지막으로 동기화한 시점을 기억함 (GET /questions의 since 값)
interface SyncState {
    var lastQuestionSync: String?
}

class PrefsSyncState(context: Context) : SyncState {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var lastQuestionSync: String?
        get() = prefs.getString(KEY_LAST_QUESTION_SYNC, null)
        set(value) = prefs.edit().putString(KEY_LAST_QUESTION_SYNC, value).apply()
}
