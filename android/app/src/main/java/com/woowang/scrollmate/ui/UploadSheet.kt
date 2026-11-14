package com.woowang.scrollmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.woowang.scrollmate.R
import com.woowang.scrollmate.data.Subject

// 어느 과목에 넣을지의 선택. 아무것도 고르지 않아도 올릴 수 있음
sealed interface SubjectChoice {
    data object Unclassified : SubjectChoice
    data class Existing(val subjectId: Long) : SubjectChoice
    data class New(val name: String) : SubjectChoice
}

// 올리기 시트. 직접 올리기와 공유로 받은 파일이 모두 이 시트 하나를 씀
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadSheet(
    fileNames: List<String>,
    warnings: List<String>,
    subjects: List<Subject>,
    onConfirm: (SubjectChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var newSubjectName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.upload_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            fileNames.forEach { name ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileBadge(name)
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            warnings.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(text = stringResource(R.string.upload_subject_label), style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubjectChip(
                    label = stringResource(R.string.subject_unclassified),
                    selected = selectedSubjectId == null && newSubjectName.isBlank(),
                    onClick = {
                        selectedSubjectId = null
                        newSubjectName = ""
                    },
                )
                subjects.forEach { subject ->
                    SubjectChip(
                        label = subject.name,
                        selected = selectedSubjectId == subject.id && newSubjectName.isBlank(),
                        onClick = {
                            selectedSubjectId = subject.id
                            newSubjectName = ""
                        },
                    )
                }
            }
            OutlinedTextField(
                value = newSubjectName,
                onValueChange = { newSubjectName = it },
                label = { Text(stringResource(R.string.subject_new_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brand.Background)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = stringResource(R.string.upload_pdf_hint), style = CaptionStyle)
                Text(text = stringResource(R.string.upload_notice), style = CaptionStyle)
            }

            PrimaryButton(
                text = stringResource(R.string.upload_confirm),
                icon = R.drawable.ic_upload,
                onClick = {
                    val trimmed = newSubjectName.trim()
                    onConfirm(
                        when {
                            trimmed.isNotEmpty() -> SubjectChoice.New(trimmed)
                            selectedSubjectId != null -> SubjectChoice.Existing(selectedSubjectId!!)
                            else -> SubjectChoice.Unclassified
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SubjectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Brand.Primary,
            selectedLabelColor = Color.White,
        ),
    )
}
