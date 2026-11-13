package com.woowang.scrollmate

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.woowang.scrollmate.data.AnswerResult
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionType

// 결과를 보여 주는 동안 가리는 답하기 화면의 요소들
private val RESULT_HIDDEN_VIEW_IDS = listOf(
    R.id.card_o,
    R.id.card_x,
    R.id.card_input,
    R.id.card_submit,
    R.id.card_skip,
)

// 결과를 읽을 시간. 확인을 누르지 않아도 이 시간이 지나면 닫힘. 임의로 정한 값
private const val RESULT_AUTO_CLOSE_MS = 4000L

// 카드가 나타나고 결과 아이콘이 튀어 오르는 시간과 시작 크기. 보기 좋게 임의로 정한 값
private const val ENTER_MS = 180L
private const val ENTER_START_SCALE = 0.92f
private const val ICON_POP_MS = 350L

// 쇼츠 위에 띄우는 문제 카드. 문제 유형마다 레이아웃이 다름 (card_ox, card_short).
// 답하면 onAnswer가 불리고, 건너뛰면 null이 넘어감
class CardOverlay(
    context: Context,
    private val windowManager: WindowManager,
    question: Question,
    onAnswer: (String?) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    // 접근성 서비스 컨텍스트에는 테마가 없으므로 감싸서 씀
    private val themed = ContextThemeWrapper(context, R.style.Theme_ScrollMate_Card)

    private val root: View = LayoutInflater.from(themed).inflate(
        when (question.type) {
            QuestionType.OX -> R.layout.card_ox
            QuestionType.SHORT -> R.layout.card_short
        },
        null,
    )

    init {
        root.findViewById<TextView>(R.id.card_question).text = question.question
        root.findViewById<View>(R.id.card_skip).setOnClickListener { onAnswer(null) }
        when (question.type) {
            QuestionType.OX -> {
                root.findViewById<View>(R.id.card_o).setOnClickListener { onAnswer("O") }
                root.findViewById<View>(R.id.card_x).setOnClickListener { onAnswer("X") }
            }
            QuestionType.SHORT -> {
                val input = root.findViewById<EditText>(R.id.card_input)
                root.findViewById<View>(R.id.card_submit).setOnClickListener {
                    onAnswer(input.text.toString())
                }
            }
        }
        liftAboveKeyboard()
    }

    fun show() {
        val params = WindowManager.LayoutParams(
            MATCH_PARENT,
            MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        )
        // 오버레이 창에서는 ADJUST_RESIZE가 키보드를 피하지 못한다는 의심이 있어(FE #3),
        // Android 11 이상에서는 키보드 높이를 직접 받아 카드를 올림
        @Suppress("DEPRECATION")
        params.softInputMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        windowManager.addView(root, params)
        // 카드가 살짝 커지며 나타남
        (root as ViewGroup).getChildAt(0).apply {
            alpha = 0f
            scaleX = ENTER_START_SCALE
            scaleY = ENTER_START_SCALE
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ENTER_MS).start()
        }
    }

    // 답한 뒤 카드 안에서 결과를 보여 줌. 토스트는 앱 알림이 꺼져 있으면 시스템이 막아 보이지 않음 (FE #42).
    // 입력 부분을 결과 문구와 확인 버튼으로 바꾸고, 확인을 누르거나 일정 시간이 지나면 onClose를 부름
    // 맞으면 내 답을, 틀리면 내 답과 정답을 나란히 보여 줌
    fun showResult(result: AnswerResult, myAnswer: String, correctAnswer: String, onClose: () -> Unit) {
        hideKeyboard()
        RESULT_HIDDEN_VIEW_IDS.forEach { root.findViewById<View>(it)?.visibility = View.GONE }
        val correct = result == AnswerResult.CORRECT
        val myAnswerText = if (myAnswer.isBlank()) {
            themed.getString(R.string.card_my_answer_empty)
        } else {
            themed.getString(R.string.card_my_answer, myAnswer.trim())
        }
        root.findViewById<ImageView>(R.id.card_result_icon).apply {
            setImageResource(if (correct) R.drawable.ic_card_correct else R.drawable.ic_card_wrong)
            visibility = View.VISIBLE
            // 결과 아이콘이 살짝 튀어 오르며 나타남
            scaleX = 0f
            scaleY = 0f
            animate().scaleX(1f).scaleY(1f).setDuration(ICON_POP_MS)
                .setInterpolator(OvershootInterpolator()).start()
        }
        root.findViewById<TextView>(R.id.card_result).apply {
            text = themed.getString(if (correct) R.string.card_correct else R.string.card_wrong)
            visibility = View.VISIBLE
        }
        root.findViewById<TextView>(R.id.card_result_detail).apply {
            text = if (correct) {
                myAnswerText
            } else {
                myAnswerText + "\n" + themed.getString(R.string.card_correct_answer, correctAnswer)
            }
            visibility = View.VISIBLE
        }
        root.findViewById<View>(R.id.card_confirm).apply {
            visibility = View.VISIBLE
            setOnClickListener { onClose() }
        }
        handler.postDelayed(onClose, RESULT_AUTO_CLOSE_MS)
    }

    fun remove() {
        handler.removeCallbacksAndMessages(null)
        hideKeyboard()
        windowManager.removeView(root)
    }

    // 창을 지우기 전에 키보드를 내리지 않으면 카드가 닫힌 뒤에도 키보드가 한참 남음 (FE #28)
    private fun hideKeyboard() {
        val input = root.findViewById<View>(R.id.card_input) ?: return
        input.clearFocus()
        themed.getSystemService(InputMethodManager::class.java)
            .hideSoftInputFromWindow(input.windowToken, 0)
    }

    private fun liftAboveKeyboard() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        root.setOnApplyWindowInsetsListener { view, insets ->
            val keyboardHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
            view.setPadding(0, 0, 0, keyboardHeight)
            insets
        }
    }
}
