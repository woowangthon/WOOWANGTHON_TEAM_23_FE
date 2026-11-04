package com.woowang.scrollmate

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.woowang.scrollmate.data.AppDatabase
import com.woowang.scrollmate.data.DeviceId
import com.woowang.scrollmate.data.MaterialRepository
import com.woowang.scrollmate.data.PrefsSyncState
import com.woowang.scrollmate.net.ApiClient

private const val DATABASE_NAME = "scrollmate.db"

// 앱 전체에서 공유하는 객체 모음
class AppContainer(context: Context) {
    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DATABASE_NAME,
    ).build()

    private val deviceId = DeviceId(context)

    val api = ApiClient(BuildConfig.API_BASE_URL, deviceId::get)

    val materialRepository = MaterialRepository(
        api = api,
        materials = database.materialDao(),
        questions = database.questionDao(),
        syncState = PrefsSyncState(context),
    )
}

class ScrollMateApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
