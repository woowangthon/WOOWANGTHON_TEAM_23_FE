package com.woowang.scrollmate.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QuestionType { OX, SHORT }

// docs의 api.md Question 모양을 따름
@Entity(
    tableName = "question",
    foreignKeys = [
        ForeignKey(
            entity = Material::class,
            parentColumns = ["id"],
            childColumns = ["materialId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("materialId")],
)
data class Question(
    @PrimaryKey val id: String,
    val materialId: String,
    val type: QuestionType,
    val question: String,
    val answer: String,
    // 단답 허용 답안. OX는 빈 목록
    val accepted: List<String> = emptyList(),
    val evidence: String,
    val page: Int? = null,
)
