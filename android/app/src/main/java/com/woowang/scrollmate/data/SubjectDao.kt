package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subject ORDER BY name")
    fun observeAll(): Flow<List<Subject>>

    @Insert
    suspend fun insert(subject: Subject): Long
}
