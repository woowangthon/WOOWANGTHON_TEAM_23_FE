package com.woowang.scrollmate.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Subject::class, Material::class, Question::class, AnswerRecord::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun materialDao(): MaterialDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerRecordDao(): AnswerRecordDao
}
