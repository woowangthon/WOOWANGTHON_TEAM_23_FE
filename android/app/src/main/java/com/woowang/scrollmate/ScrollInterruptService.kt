package com.woowang.scrollmate

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.woowang.scrollmate.data.AnswerRecord
import com.woowang.scrollmate.data.AnswerResult
import com.woowang.scrollmate.data.DEV_MATERIAL_ID
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionType
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
    private var card: View? = null
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

        val view = buildCard(question)
        val params = WindowManager.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        windowManager.addView(view, params)
        card = view
    }

    // input이 null이면 건너뜀
    private fun dismissCard(question: Question, input: String?) {
        val result = if (input == null) AnswerResult.SKIPPED else grade(question, input)
        Log.i(TAG, "card dismissed question=${question.id} result=$result")
        windowManager.removeView(card)
        card = null
        audioManager.abandonAudioFocusRequest(audioFocus)

        when (result) {
            AnswerResult.CORRECT -> toast(getString(R.string.card_correct))
            AnswerResult.WRONG -> toast(getString(R.string.card_wrong, question.answer))
            AnswerResult.SKIPPED -> Unit
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

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun buildCard(question: Question): View {
        val input = EditText(this).apply {
            hint = getString(R.string.card_short_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 56, 56, 40)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 48f
            }
            addView(TextView(context).apply {
                text = question.question
                textSize = 18f
                setTextColor(Color.BLACK)
            })
            when (question.type) {
                QuestionType.OX -> {
                    addView(answerButton("O") { dismissCard(question, "O") })
                    addView(answerButton("X") { dismissCard(question, "X") })
                }
                QuestionType.SHORT -> {
                    addView(input)
                    addView(answerButton(getString(R.string.card_submit)) { dismissCard(question, input.text.toString()) })
                }
            }
            addView(answerButton(getString(R.string.card_skip)) { dismissCard(question, null) })
        }
        return FrameLayout(this).apply {
            setBackgroundColor(0x99000000.toInt())
            addView(
                body,
                FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER).apply {
                    setMargins(48, 48, 48, 48)
                },
            )
        }
    }

    private fun answerButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }
}
