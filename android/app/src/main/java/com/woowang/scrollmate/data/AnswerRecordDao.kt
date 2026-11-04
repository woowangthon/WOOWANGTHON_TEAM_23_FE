package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface AnswerRecordDao {
    @Insert
    suspend fun insert(record: AnswerRecord): Long
}
