package com.woowang.scrollmate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.woowang.scrollmate.ScrollMateApp
import com.woowang.scrollmate.domain.buildSubjectCards
import kotlinx.coroutines.launch
import java.time.LocalDate

// 기기에 저장된 학습 주제와 자료를 읽어 홈 화면에 넘김. 출제 켜기/끄기는 기기 DB에만 저장함
@Composable
fun HomeRoute(onUploadClick: () -> Unit, onGuideClick: () -> Unit) {
    val container = (LocalContext.current.applicationContext as ScrollMateApp).container
    val subjectDao = container.database.subjectDao()
    val subjects by subjectDao.observeAll().collectAsState(initial = emptyList())
    val materials by container.materialRepository.observeMaterials().collectAsState(initial = emptyList())
    val cards = remember(subjects, materials) { buildSubjectCards(subjects, materials, LocalDate.now()) }
    val scope = rememberCoroutineScope()

    HomeScreen(
        cards = cards,
        onToggleEnabled = { card, enabled ->
            card.subject?.let { scope.launch { subjectDao.update(it.copy(enabled = enabled)) } }
        },
        onUploadClick = onUploadClick,
        onGuideClick = onGuideClick,
    )
}
