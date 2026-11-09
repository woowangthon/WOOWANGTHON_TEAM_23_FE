package com.woowang.scrollmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woowang.scrollmate.R
import com.woowang.scrollmate.data.Subject
import com.woowang.scrollmate.domain.SUBJECT_NAME_MAX_LENGTH
import com.woowang.scrollmate.domain.SubjectNameCheck
import com.woowang.scrollmate.domain.checkSubjectName
import com.woowang.scrollmate.domain.pickerMillisToDate
import com.woowang.scrollmate.domain.toPickerMillis
import java.time.LocalDate

// 학습 주제를 만들거나 고치는 창. subject가 null이면 새로 만듦. 시험일은 선택 사항임
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectEditDialog(
    subject: Subject?,
    otherNames: List<String>,
    onSave: (name: String, examDate: LocalDate?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(subject?.name.orEmpty()) }
    var examDateText by rememberSaveable { mutableStateOf(subject?.examDate) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val examDate = examDateText?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val check = checkSubjectName(name, otherNames)
    // 아직 아무것도 쓰지 않았을 때는 오류 문구를 보여 주지 않음
    val showNameError = check != SubjectNameCheck.OK && !(check == SubjectNameCheck.BLANK && name.isEmpty())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (subject == null) R.string.subject_new_title else R.string.subject_edit_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing))) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.subject_name_label)) },
                    singleLine = true,
                    isError = showNameError,
                    supportingText = {
                        if (showNameError) {
                            Text(
                                when (check) {
                                    SubjectNameCheck.TOO_LONG ->
                                        stringResource(R.string.subject_name_too_long, SUBJECT_NAME_MAX_LENGTH)
                                    SubjectNameCheck.DUPLICATE -> stringResource(R.string.subject_name_duplicate)
                                    else -> stringResource(R.string.subject_name_blank)
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.subject_exam_date_label), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = examDate?.toString() ?: stringResource(R.string.subject_exam_date_none),
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (examDate != null && examDate.isBefore(LocalDate.now())) {
                    Text(
                        text = stringResource(R.string.subject_exam_date_past),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing))) {
                    TextButton(onClick = { showPicker = true }) {
                        Text(stringResource(R.string.subject_exam_date_pick))
                    }
                    if (examDate != null) {
                        TextButton(onClick = { examDateText = null }) {
                            Text(stringResource(R.string.subject_exam_date_clear))
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text(stringResource(R.string.subject_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = check == SubjectNameCheck.OK,
                onClick = { onSave(name.trim(), examDate) },
            ) { Text(stringResource(R.string.subject_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.subject_cancel)) }
        },
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (examDate ?: LocalDate.now()).toPickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { examDateText = pickerMillisToDate(it).toString() }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.subject_picker_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.subject_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.subject_delete_confirm_title)) },
            text = { Text(stringResource(R.string.subject_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.subject_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.subject_cancel)) }
            },
        )
    }
}
