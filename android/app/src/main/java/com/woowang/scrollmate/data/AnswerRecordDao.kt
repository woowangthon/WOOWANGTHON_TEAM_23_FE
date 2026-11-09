package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AnswerRecordDao {
    @Insert
    suspend fun insert(record: AnswerRecord): Long

    // 한 학습 주제(null = 미분류)의 문제들에 대한 풀이 기록 전부. 다시 낼 문제를 정하는 데 씀
    @Query(
        """
        SELECT a.* FROM answer_record a
        JOIN question q ON q.id = a.questionId
        JOIN material m ON m.id = q.materialId
        WHERE m.subjectId IS :subjectId
        """,
    )
    suspend fun recordsOfSubject(subjectId: Long?): List<AnswerRecord>
}
