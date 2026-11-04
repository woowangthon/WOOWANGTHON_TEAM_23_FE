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

    // 아직 풀지 않은 문제 중 하나. 과목 id가 null이면 미분류
    @Query(
        """
        SELECT q.* FROM question q
        JOIN material m ON m.id = q.materialId
        WHERE m.subjectId IS :subjectId
          AND q.id NOT IN (SELECT questionId FROM answer_record)
        ORDER BY RANDOM() LIMIT 1
        """,
    )
    suspend fun randomUnanswered(subjectId: Long?): Question?

    // 안 푼 문제가 없을 때의 대체. 과목 안에서 무작위
    @Query(
        """
        SELECT q.* FROM question q
        JOIN material m ON m.id = q.materialId
        WHERE m.subjectId IS :subjectId
        ORDER BY RANDOM() LIMIT 1
        """,
    )
    suspend fun randomAny(subjectId: Long?): Question?
}
