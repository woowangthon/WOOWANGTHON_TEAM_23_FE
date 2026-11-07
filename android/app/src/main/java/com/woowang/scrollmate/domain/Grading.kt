package com.woowang.scrollmate.domain

import com.woowang.scrollmate.data.AnswerResult
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionType

// 기기에서 하는 채점. OX는 O/X 일치, 단답은 공백과 대소문자를 무시하고 정답·허용 답안과 비교함.
// 기기에서 틀렸을 때 서버 /grade로 다시 묻는 것은 FE #18에서 다룸
fun grade(question: Question, input: String): AnswerResult {
    val matches = when (question.type) {
        QuestionType.OX -> normalize(input) == normalize(question.answer)
        QuestionType.SHORT -> {
            val given = normalize(input)
            given.isNotEmpty() && (listOf(question.answer) + question.accepted).any { normalize(it) == given }
        }
    }
    return if (matches) AnswerResult.CORRECT else AnswerResult.WRONG
}

private fun normalize(text: String) = text.filterNot { it.isWhitespace() }.lowercase()
