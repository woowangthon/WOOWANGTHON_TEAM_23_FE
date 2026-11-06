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
    )
        // 시연 기간에는 스키마가 바뀌어도 앱이 멈추지 않게 기존 데이터를 지우고 다시 만듦.
        // 이 경우 기기에 저장된 문제와 풀이 기록이 사라지므로, 실제 배포 전에 마이그레이션으로 바꿔야 함
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

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
