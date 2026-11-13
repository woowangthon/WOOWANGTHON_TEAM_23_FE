package com.woowang.scrollmate

import android.accessibilityservice.AccessibilityService
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.woowang.scrollmate.data.AnswerRecord
import com.woowang.scrollmate.data.AnswerResult
import com.woowang.scrollmate.data.DEV_MATERIAL_ID
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.domain.cardChance
import com.woowang.scrollmate.domain.grade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.random.Random

private const val TAG = "ScrollInterrupt"
private const val SHORTS_FEED_ID = "com.google.android.youtube:id/reel_recycler"
private const val PAGE_SETTLE_MS = 400L
private val KST = ZoneId.of("Asia/Seoul")

class ScrollInterruptService : AccessibilityService() {

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private val audioFocus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        .setOnAudioFocusChangeListener {}
        .build()

    private val handler = Handler(Looper.getMainLooper())
    private var checkPending = false
    private var card: CardOverlay? = null
    private var currentPage: Int? = null
    private var skipNextSwipe = false

    // 문제를 고르는 동안 같은 넘김으로 카드가 두 번 뜨지 않게 막음
    private var cardLoading = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val container get() = (application as ScrollMateApp).container

    // 쇼츠에 들어온 시각. 쇼츠를 나가면 비워서 다음에 들어올 때 0분부터 다시 셈 (FE #13)
    private var shortsEnteredAtMs: Long? = null

    override fun onServiceConnected() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        Log.i(TAG, "connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (card != null || checkPending) return
        checkPending = true
        handler.postDelayed(::checkFeed, PAGE_SETTLE_MS)
    }

    private fun checkFeed() {
        checkPending = false
        if (card != null) return

        val feed = rootInActiveWindow?.findVisibleShortsFeed()
        if (feed == null) {
            if (currentPage != null) Log.i(TAG, "shorts exit")
            currentPage = null
            shortsEnteredAtMs = null
            return
        }

        // YouTube가 스크롤 이벤트를 보내지 않아 화면에 남은 페이지 노드 변화로 넘김을 판별함
        val page = feed.singleChild()?.hashCode() ?: return
        val previous = currentPage
        currentPage = page
        when (previous) {
            null -> {
                shortsEnteredAtMs = SystemClock.elapsedRealtime()
                Log.i(TAG, "shorts enter")
            }
            page -> return
            else -> onSwipe()
        }
    }

    private fun onSwipe() {
        if (skipNextSwipe) {
            skipNextSwipe = false
            Log.i(TAG, "swipe skipped")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val chance = cardChance(now - (shortsEnteredAtMs ?: now))
        val shouldShow = Random.nextDouble() < chance
        Log.i(TAG, "swipe chance=$chance show=$shouldShow")
        if (shouldShow) prepareCard()
    }

    // 저장된 문제를 골라 카드를 띄움. 고를 문제가 없으면 카드를 띄우지 않고 다음 넘김을 그대로 셈
    private fun prepareCard() {
        if (cardLoading) return
        cardLoading = true
        scope.launch {
            try {
                val question = container.cardQuestionSource.next() ?: container.devQuestions.randomOrNull()
                if (question == null) {
                    Log.i(TAG, "no question to show")
                } else if (card == null) {
                    skipNextSwipe = true
                    showCard(question)
                }
            } finally {
                cardLoading = false
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    private fun AccessibilityNodeInfo.findVisibleShortsFeed() =
        findAccessibilityNodeInfosByViewId(SHORTS_FEED_ID).firstOrNull { it.isVisibleToUser }

    private fun AccessibilityNodeInfo.singleChild() =
        if (childCount == 1) getChild(0) else null

    private fun showCard(question: Question) {
        val focus = audioManager.requestAudioFocus(audioFocus)
        Log.i(TAG, "card shown audioFocus=$focus question=${question.id}")

        card = CardOverlay(this, windowManager, question) { input -> dismissCard(question, input) }
            .also { it.show() }
    }

    // input이 null이면 건너뜀. 답했으면 결과를 카드 안에서 보여 주고 닫으며, 건너뛰면 바로 닫음
    private fun dismissCard(question: Question, input: String?) {
        val result = if (input == null) AnswerResult.SKIPPED else grade(question, input)
        Log.i(TAG, "card answered question=${question.id} result=$result")
        if (input == null) {
            closeCard()
        } else {
            card?.showResult(result, input, question.answer, ::closeCard)
        }
        // 개발용 샘플 문제는 DB에 없으므로 기록하지 않음
        if (question.materialId != DEV_MATERIAL_ID) {
            scope.launch {
                container.database.answerRecordDao().insert(
                    AnswerRecord(
                        questionId = question.id,
                        result = result,
                        answeredAt = OffsetDateTime.now(KST).toString(),
                    ),
                )
            }
        }
    }

    private fun closeCard() {
        if (card == null) return
        Log.i(TAG, "card closed")
        card?.remove()
        card = null
        audioManager.abandonAudioFocusRequest(audioFocus)
    }
}
