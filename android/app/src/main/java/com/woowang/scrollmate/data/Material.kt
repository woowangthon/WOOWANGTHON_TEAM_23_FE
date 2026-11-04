package com.woowang.scrollmate.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MaterialStatus { PROCESSING, READY, FAILED }

// 서버(BE)가 부여한 자료 id를 그대로 사용함. 과목 연결은 기기에만 있음
@Entity(
    tableName = "material",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            // 과목을 지우면 자료는 미분류(null)로 남김
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("subjectId")],
)
data class Material(
    @PrimaryKey val id: String,
    val name: String,
    val subjectId: Long? = null,
    val status: MaterialStatus = MaterialStatus.PROCESSING,
    val questionCount: Int = 0,
)
