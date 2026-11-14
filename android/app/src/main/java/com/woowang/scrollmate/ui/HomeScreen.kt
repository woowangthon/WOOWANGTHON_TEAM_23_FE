package com.woowang.scrollmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woowang.scrollmate.R
import com.woowang.scrollmate.domain.SubjectCard

// 앱을 열면 처음 보는 화면. 학습 주제 카드와 시험일까지 남은 날짜, 자료 올리기를 보여 줌
@Composable
fun HomeScreen(
    cards: List<SubjectCard>,
    onToggleEnabled: (SubjectCard, Boolean) -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (SubjectCard) -> Unit,
    failureCount: Int,
    onMaterialsClick: () -> Unit,
    onUploadClick: () -> Unit,
    onGuideClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
    ) {
        item {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        if (failureCount > 0) {
            item { UploadFailureBanner(failureCount, onMaterialsClick) }
        }
        item {
            Button(onClick = onUploadClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.upload_button))
            }
        }
        item {
            TextButton(onClick = onMaterialsClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_materials_button))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.home_subjects_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onAddSubject) {
                    Text(stringResource(R.string.subject_add))
                }
            }
        }
        if (cards.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.home_subjects_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            items(cards, key = { it.subject?.id ?: UNCLASSIFIED_KEY }) { card ->
                SubjectCardItem(card, onToggleEnabled, onEditSubject)
            }
        }
        item {
            TextButton(onClick = onGuideClick) {
                Text(stringResource(R.string.home_guide_button))
            }
        }
    }
}

private const val UNCLASSIFIED_KEY = -1L

// 눌러서 이름과 시험일을 고침. 학습 주제가 없는 자료를 모은 미분류 카드는 고칠 대상이 없음
@Composable
private fun SubjectCardItem(
    card: SubjectCard,
    onToggleEnabled: (SubjectCard, Boolean) -> Unit,
    onEdit: (SubjectCard) -> Unit,
) {
    if (card.subject != null) {
        Card(onClick = { onEdit(card) }, modifier = Modifier.fillMaxWidth()) {
            SubjectCardRow(card, onToggleEnabled)
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth()) {
            SubjectCardRow(card, onToggleEnabled)
        }
    }
}

@Composable
private fun SubjectCardRow(card: SubjectCard, onToggleEnabled: (SubjectCard, Boolean) -> Unit) {
    val subject = card.subject
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.sheet_padding)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject?.name ?: stringResource(R.string.subject_unclassified),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_materials_count, card.materialCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            card.daysLeft?.let { Text(text = daysLeftText(it), style = MaterialTheme.typography.bodyMedium) }
        }
        // 학습 주제가 없는 자료(미분류)는 켜고 끌 대상이 없음
        if (subject != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = stringResource(R.string.home_enabled_label), style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = subject.enabled,
                    onCheckedChange = { onToggleEnabled(card, it) },
                )
            }
        }
    }
}

@Composable
private fun daysLeftText(daysLeft: Long): String = when {
    daysLeft == 0L -> stringResource(R.string.home_dday_today)
    daysLeft > 0 -> stringResource(R.string.home_dday_left, daysLeft)
    else -> stringResource(R.string.home_dday_past)
}

// 백그라운드 업로드가 실패했을 때 홈에서 바로 알려 줌. 이유와 다음 행동은 자료 목록에서 보여 줌
@Composable
private fun UploadFailureBanner(count: Int, onMaterialsClick: () -> Unit) {
    Card(onClick = onMaterialsClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.sheet_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
        ) {
            Text(
                text = stringResource(R.string.home_upload_failed_banner, count),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.home_upload_failed_action),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
