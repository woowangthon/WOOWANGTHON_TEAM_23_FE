package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material ORDER BY name")
    fun observeAll(): Flow<List<Material>>

    @Query("SELECT * FROM material")
    suspend fun getAll(): List<Material>

    // REPLACE는 기존 행을 지우고 다시 넣어서 연결된 문제와 풀이 기록이 연쇄 삭제되므로 Upsert를 사용함
    @Upsert
    suspend fun upsertAll(materials: List<Material>)

    @Query("UPDATE material SET subjectId = :subjectId WHERE id = :materialId")
    suspend fun assignSubject(materialId: String, subjectId: Long?)

    @Query("DELETE FROM material WHERE id = :materialId")
    suspend fun delete(materialId: String)
}
