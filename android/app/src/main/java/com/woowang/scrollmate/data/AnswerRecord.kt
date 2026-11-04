package com.woowang.scrollmate.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AnswerResult { CORRECT, WRONG, SKIPPED }

@Entity(
    tableName = "answer_record",
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("questionId")],
)
data class AnswerRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val result: AnswerResult,
    // ISO 8601 (KST 오프셋 포함)
    val answeredAt: String,
)
