package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM material ORDER BY name")
    fun observeAll(): Flow<List<Material>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(materials: List<Material>)

    @Query("UPDATE material SET subjectId = :subjectId WHERE id = :materialId")
    suspend fun assignSubject(materialId: String, subjectId: Long?)

    @Query("DELETE FROM material WHERE id = :materialId")
    suspend fun delete(materialId: String)
}
