package com.woowang.scrollmate

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import com.woowang.scrollmate.data.Question
import com.woowang.scrollmate.data.QuestionType

// 쇼츠 위에 띄우는 문제 카드. 문제 유형마다 레이아웃이 다름 (card_ox, card_short).
// 답하면 onAnswer가 불리고, 건너뛰면 null이 넘어감
class CardOverlay(
    context: Context,
    private val windowManager: WindowManager,
    question: Question,
    onAnswer: (String?) -> Unit,
) {
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
    }

    fun remove() {
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
