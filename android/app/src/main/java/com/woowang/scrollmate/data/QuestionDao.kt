package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface QuestionDao {
    // REPLACE를 쓰면 문제를 다시 받을 때 풀이 기록이 연쇄 삭제되므로 Upsert를 사용함
    @Upsert
    suspend fun upsertAll(questions: List<Question>)

    // 출제할 문제가 있는 과목(null = 미분류)의 목록
    @Query(
        """
        SELECT DISTINCT m.subjectId FROM question q
        JOIN material m ON m.id = q.materialId
        """,
    )
    suspend fun subjectIdsWithQuestions(): List<Long?>

    // 한 학습 주제(null = 미분류)의 문제 전부. 어떤 문제를 낼지는 풀이 기록과 함께 앱에서 정함
    @Query(
        """
        SELECT q.* FROM question q
        JOIN material m ON m.id = q.materialId
        WHERE m.subjectId IS :subjectId
        """,
    )
    suspend fun questionsOfSubject(subjectId: Long?): List<Question>
}
