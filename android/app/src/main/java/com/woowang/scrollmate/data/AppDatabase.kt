package com.woowang.scrollmate.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, Material::class, Question::class, AnswerRecord::class, WatchSession::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun materialDao(): MaterialDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerRecordDao(): AnswerRecordDao
    abstract fun watchSessionDao(): WatchSessionDao
}

// 시청 기록 표를 더함. 이미 받은 문제와 풀이 기록은 그대로 둠
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `watch_session` (`id` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, " +
                "`endedAt` INTEGER NOT NULL, `activeMs` INTEGER NOT NULL, `lateNightMs` INTEGER NOT NULL, " +
                "`swipes` INTEGER NOT NULL, `cardsShown` INTEGER NOT NULL, `cardsAnswered` INTEGER NOT NULL, " +
                "`cardsSkipped` INTEGER NOT NULL, `correct` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
    }
}
