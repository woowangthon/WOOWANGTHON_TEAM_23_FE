package com.woowang.scrollmate.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.woowang.scrollmate.ScrollMateApp
import com.woowang.scrollmate.data.Subject
import com.woowang.scrollmate.domain.buildSubjectCards
import kotlinx.coroutines.launch
import java.time.LocalDate

// 기기에 저장된 학습 주제와 자료를 읽어 홈 화면에 넘김. 학습 주제와 시험일은 기기 DB에만 저장하고 서버로 보내지 않음
@Composable
fun HomeRoute(onUploadClick: () -> Unit, onGuideClick: () -> Unit) {
    val container = (LocalContext.current.applicationContext as ScrollMateApp).container
    val subjectDao = container.database.subjectDao()
    val subjects by subjectDao.observeAll().collectAsState(initial = emptyList())
    val materials by container.materialRepository.observeMaterials().collectAsState(initial = emptyList())
    val cards = remember(subjects, materials) { buildSubjectCards(subjects, materials, LocalDate.now()) }
    val scope = rememberCoroutineScope()

    // 편집 창: 열려 있는지와 고치는 주제의 id(null이면 새로 만듦)
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    HomeScreen(
        cards = cards,
        onToggleEnabled = { card, enabled ->
            card.subject?.let { scope.launch { subjectDao.update(it.copy(enabled = enabled)) } }
        },
        onAddSubject = {
            editingId = null
            editorOpen = true
        },
        onEditSubject = { card ->
            editingId = card.subject?.id
            editorOpen = true
        },
        onUploadClick = onUploadClick,
        onGuideClick = onGuideClick,
    )

    if (editorOpen) {
        val editing: Subject? = subjects.firstOrNull { it.id == editingId }
        SubjectEditDialog(
            subject = editing,
            otherNames = subjects.filter { it.id != editing?.id }.map { it.name },
            onSave = { name, examDate ->
                scope.launch {
                    if (editing == null) {
                        subjectDao.insert(Subject(name = name, examDate = examDate?.toString()))
                    } else {
                        subjectDao.update(editing.copy(name = name, examDate = examDate?.toString()))
                    }
                }
                editorOpen = false
            },
            // 주제를 지워도 자료는 남고 미분류로 옮겨짐 (자료의 subjectId는 삭제 시 null이 됨)
            onDelete = editing?.let { subject ->
                {
                    scope.launch { subjectDao.delete(subject) }
                    editorOpen = false
                }
            },
            onDismiss = { editorOpen = false },
        )
    }
}
