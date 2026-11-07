package com.woowang.scrollmate.data

import android.content.Context
import com.woowang.scrollmate.net.QuestionsResponse
import kotlinx.serialization.json.Json

private const val DEV_QUESTIONS_ASSET = "dev_questions.json"
const val DEV_MATERIAL_ID = "dev"

private val json = Json { ignoreUnknownKeys = true }

// 서버 없이 카드를 시험하기 위한 개발용 문제. 디버그 빌드의 assets에만 있으므로 릴리스 빌드에서는 항상 빈 목록임.
// DB에 넣지 않으므로 풀이 기록은 남기지 않음 (DEV_MATERIAL_ID)
fun loadDevQuestions(context: Context): List<Question> {
    val text = try {
        context.assets.open(DEV_QUESTIONS_ASSET).bufferedReader().use { it.readText() }
    } catch (_: java.io.IOException) {
        return emptyList()
    }
    return json.decodeFromString<QuestionsResponse>(text).questions.mapNotNull { it.toEntityOrNull() }
}
