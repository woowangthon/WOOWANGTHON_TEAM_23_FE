package com.woowang.scrollmate.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// 쇼츠에 들어와서 나갈 때까지 한 번. 영상 제목, 채널, 계정은 저장하지 않음
@Entity(tableName = "watch_session")
data class WatchSession(
    @PrimaryKey val id: String,
    // epoch 밀리초
    val startedAt: Long,
    val endedAt: Long,
    // 실제로 본 시간. 화면 변화 사이 간격을 더하되, 너무 긴 간격은 자리를 비운 것으로 보고 잘라 냄
    val activeMs: Long = 0,
    // 그중 밤 12시~6시에 본 시간
    val lateNightMs: Long = 0,
    val swipes: Int = 0,
    val cardsShown: Int = 0,
    val cardsAnswered: Int = 0,
    val cardsSkipped: Int = 0,
    val correct: Int = 0,
)

@Dao
interface WatchSessionDao {
    @Upsert
    suspend fun upsert(session: WatchSession)

    @Query("SELECT * FROM watch_session WHERE startedAt >= :since ORDER BY startedAt")
    suspend fun since(since: Long): List<WatchSession>

    @Query("SELECT * FROM watch_session WHERE startedAt >= :since ORDER BY startedAt")
    fun observeSince(since: Long): Flow<List<WatchSession>>
}
