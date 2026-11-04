package com.woowang.scrollmate.data

import com.woowang.scrollmate.net.MaterialDto
import com.woowang.scrollmate.net.QuestionDto

// 서버 응답(DTO)을 Room 엔티티로 바꾸는 곳. BE 응답 모양이 달라지면 여기만 고침

// 알 수 없는 상태 값은 실패로 취급함 (계속 생성 중으로 보이는 것보다 낫다고 판단)
fun MaterialDto.toEntity(subjectId: Long?): Material = Material(
    id = id,
    name = name,
    subjectId = subjectId,
    status = when (status) {
        "processing" -> MaterialStatus.PROCESSING
        "ready" -> MaterialStatus.READY
        else -> MaterialStatus.FAILED
    },
    questionCount = questionCount,
)

// 알 수 없는 유형의 문제는 출제할 수 없으므로 버림
fun QuestionDto.toEntityOrNull(): Question? {
    val questionType = when (type) {
        "ox" -> QuestionType.OX
        "short" -> QuestionType.SHORT
        else -> return null
    }
    return Question(
        id = id,
        materialId = materialId,
        type = questionType,
        question = question,
        answer = answer,
        accepted = accepted,
        evidence = evidence,
        page = page,
    )
}
