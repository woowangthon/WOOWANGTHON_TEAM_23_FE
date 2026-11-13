package com.woowang.scrollmate

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.woowang.scrollmate.data.AppDatabase
import com.woowang.scrollmate.data.CardQuestionSource
import com.woowang.scrollmate.data.DeviceId
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.loadDevQuestions
import com.woowang.scrollmate.data.MaterialRepository
import com.woowang.scrollmate.data.PrefsSyncState
import com.woowang.scrollmate.net.ApiClient
import com.woowang.scrollmate.sync.SyncScheduler
import com.woowang.scrollmate.upload.UploadFailureLog

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

    val cardQuestionSource = CardQuestionSource(
        subjects = database.subjectDao(),
        questions = database.questionDao(),
        answers = database.answerRecordDao(),
    )

    // 개발용 샘플 문제. 디버그 빌드에만 있고 DB에는 넣지 않음
    val devQuestions: List<Question> = loadDevQuestions(context)

    // 백그라운드 업로드가 실패한 기록. 화면에서 이유를 알려 줄 때 씀
    val uploadFailures = UploadFailureLog(context)

    val materialRepository = MaterialRepository(
        api = api,
        materials = database.materialDao(),
        questions = database.questionDao(),
        syncState = PrefsSyncState(context),
        answers = database.answerRecordDao(),
    )
}

class ScrollMateApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        SyncScheduler.start(this)
    }
}
