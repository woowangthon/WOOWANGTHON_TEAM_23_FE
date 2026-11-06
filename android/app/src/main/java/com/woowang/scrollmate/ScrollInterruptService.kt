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
import com.woowang.scrollmate.domain.cardChance
import kotlin.random.Random

private const val TAG = "ScrollInterrupt"
private const val SHORTS_FEED_ID = "com.google.android.youtube:id/reel_recycler"
private const val PAGE_SETTLE_MS = 400L

// PoC 전용 고정 문제. 실제 출제 로직 아님
private const val SAMPLE_QUESTION = "[샘플] 쇼츠 감지 확인용 문제. 대한민국의 수도는 서울이다."

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
        if (shouldShow) {
            skipNextSwipe = true
            showCard()
        }
    }

    override fun onInterrupt() = Unit

    private fun AccessibilityNodeInfo.findVisibleShortsFeed() =
        findAccessibilityNodeInfosByViewId(SHORTS_FEED_ID).firstOrNull { it.isVisibleToUser }

    private fun AccessibilityNodeInfo.singleChild() =
        if (childCount == 1) getChild(0) else null

    private fun showCard() {
        val focus = audioManager.requestAudioFocus(audioFocus)
        Log.i(TAG, "card shown audioFocus=$focus")

        val view = buildCard()
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

    private fun dismissCard(result: String) {
        Log.i(TAG, "card dismissed result=$result")
        windowManager.removeView(card)
        card = null
        audioManager.abandonAudioFocusRequest(audioFocus)
    }

    private fun buildCard(): View {
        val input = EditText(this).apply {
            hint = "단답 입력"
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
                text = SAMPLE_QUESTION
                textSize = 18f
                setTextColor(Color.BLACK)
            })
            addView(answerButton("O") { dismissCard("ox:O") })
            addView(answerButton("X") { dismissCard("ox:X") })
            addView(input)
            addView(answerButton("제출") { dismissCard("short:${input.text}") })
            addView(answerButton("건너뛰기") { dismissCard("skip") })
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
