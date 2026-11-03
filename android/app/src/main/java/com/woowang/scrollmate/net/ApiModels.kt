package com.woowang.scrollmate.net

import kotlinx.serialization.Serializable

// docs의 api.md(FE ↔ BE) 응답·요청 모양. 서버 JSON을 그대로 받는 용도이며 Room 엔티티와는 분리함

@Serializable
data class MaterialDto(
    val id: String,
    val name: String,
    // processing / ready / failed
    val status: String,
    val chunksTotal: Int = 0,
    val chunksDone: Int = 0,
    val questionCount: Int = 0,
)

@Serializable
data class MaterialsResponse(val materials: List<MaterialDto>)

@Serializable
data class QuestionDto(
    val id: String,
    val materialId: String,
    // ox / short
    val type: String,
    val question: String,
    val answer: String,
    val accepted: List<String> = emptyList(),
    val evidence: String,
    val page: Int? = null,
)

@Serializable
data class QuestionsResponse(
    val questions: List<QuestionDto>,
    val serverTime: String,
)

@Serializable
data class AnswerDto(
    val questionId: String,
    // correct / wrong / skipped
    val result: String,
    val answeredAt: String,
)

@Serializable
data class AnswersRequest(val answers: List<AnswerDto>)

@Serializable
data class RecordedResponse(val recorded: Int)

@Serializable
data class ErrorResponse(val error: ErrorBody)

@Serializable
data class ErrorBody(val code: String, val message: String)
