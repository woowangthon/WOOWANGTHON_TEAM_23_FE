package com.woowang.scrollmate.data

import android.content.Context
import com.woowang.scrollmate.domain.WatchDay
import com.woowang.scrollmate.domain.summarizeByDay
import com.woowang.scrollmate.net.ApiClient
import com.woowang.scrollmate.net.UsageDayDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.ZoneId

private const val PREFS_NAME = "scrollmate_advice"
private const val KEY_MESSAGE = "message"
private const val KEY_BASED_ON = "based_on"
private const val KEY_DAY = "day"
private const val SEND_DAYS = 7L

data class Advice(val message: String, val basedOn: String?)

// 시청 기록의 하루 합계를 서버에 보내고 오늘의 한마디를 받아 둠. 받은 문장은 그날 동안 기기에 남겨 오프라인에서도 보여 줌
class AdviceRepository(
    context: Context,
    private val api: ApiClient,
    private val sessions: WatchSessionDao,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _advice = MutableStateFlow(cached())
    val advice: StateFlow<Advice?> = _advice

    // 네트워크가 안 되면 조용히 실패하고 남겨 둔 문장을 그대로 씀
    suspend fun refresh() {
        val today = LocalDate.now(zone)
        val since = today.minusDays(SEND_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
        val days = summarizeByDay(sessions.since(since), zone)
        runCatching {
            if (days.isNotEmpty()) api.postUsage(days.map { it.toDto() })
            val response = api.getTodayAdvice()
            val message = response.message ?: return@runCatching
            val advice = Advice(message, response.basedOn)
            _advice.value = advice
            // 문장을 만들지 못해 수치만 나열한 경우는 남기지 않아 다음에 다시 받음
            if (!response.fallback) {
                prefs.edit()
                    .putString(KEY_MESSAGE, message)
                    .putString(KEY_BASED_ON, response.basedOn)
                    .putString(KEY_DAY, today.toString())
                    .apply()
            }
        }
    }

    private fun cached(): Advice? {
        if (prefs.getString(KEY_DAY, null) != LocalDate.now(zone).toString()) return null
        val message = prefs.getString(KEY_MESSAGE, null) ?: return null
        return Advice(message, prefs.getString(KEY_BASED_ON, null))
    }
}

private fun WatchDay.toDto() = UsageDayDto(
    date = date.toString(),
    sessionCount = sessionCount,
    totalMinutes = totalMinutes,
    swipes = swipes,
    cardsShown = cardsShown,
    cardsAnswered = cardsAnswered,
    cardsSkipped = cardsSkipped,
    correct = correct,
    lateNightShare = lateNightShare,
    longestSessionMinutes = longestSessionMinutes,
)
