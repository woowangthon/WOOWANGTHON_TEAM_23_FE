package com.woowang.scrollmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woowang.scrollmate.R
import com.woowang.scrollmate.data.MaterialStatus
import com.woowang.scrollmate.domain.MAX_UPLOAD_FILE_BYTES
import com.woowang.scrollmate.domain.MaterialItem
import com.woowang.scrollmate.domain.UploadFailure
import com.woowang.scrollmate.domain.UploadFailureReason

private const val BYTES_PER_MB = 1024 * 1024

// 올린 자료의 상태와 올리지 못한 자료의 이유를 보여 주는 화면
@Composable
fun MaterialListScreen(
    items: List<MaterialItem>,
    failures: List<UploadFailure>,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onDelete: (MaterialItem) -> Unit,
    onDismissFailure: (UploadFailure) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.materials_back)) }
                TextButton(onClick = onRefresh, enabled = !refreshing) {
                    Text(stringResource(if (refreshing) R.string.materials_refreshing else R.string.materials_refresh))
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.materials_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        items(failures, key = { "failure-${it.id}" }) { failure ->
            FailureCard(failure, onDismiss = { onDismissFailure(failure) })
        }

        if (items.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.materials_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            items(items, key = { "material-${it.material.id}" }) { item ->
                MaterialCard(item, onDelete = { onDelete(item) })
            }
        }
    }
}

@Composable
private fun FailureCard(failure: UploadFailure, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.sheet_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
        ) {
            Text(
                text = failureMessage(failure),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.materials_failure_dismiss)) }
        }
    }
}

@Composable
private fun failureMessage(failure: UploadFailure): String {
    val names = failure.fileNames.joinToString()
    return when (failure.reason) {
        UploadFailureReason.UNSUPPORTED_FILE -> stringResource(R.string.upload_failed_unsupported, names)
        UploadFailureReason.FILE_TOO_LARGE ->
            stringResource(R.string.upload_failed_too_large, names, (MAX_UPLOAD_FILE_BYTES / BYTES_PER_MB).toInt())
        UploadFailureReason.RATE_LIMITED -> stringResource(R.string.upload_failed_rate_limited, names)
        UploadFailureReason.NETWORK -> stringResource(R.string.upload_failed_network, names)
        UploadFailureReason.OTHER -> stringResource(R.string.upload_failed_other, names)
    }
}

@Composable
private fun MaterialCard(item: MaterialItem, onDelete: () -> Unit) {
    val material = item.material
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.sheet_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
        ) {
            Text(text = material.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.subjectName ?: stringResource(R.string.subject_unclassified),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text = statusText(item), style = MaterialTheme.typography.bodyMedium)

            val progress = item.progress
            if (material.status == MaterialStatus.PROCESSING && progress != null && progress.total > 0) {
                LinearProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = stringResource(R.string.materials_question_count, material.questionCount),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDelete) { Text(stringResource(R.string.materials_delete)) }
        }
    }
}

@Composable
private fun statusText(item: MaterialItem): String = when (item.material.status) {
    MaterialStatus.PROCESSING -> {
        val progress = item.progress
        if (progress != null && progress.total > 0) {
            stringResource(R.string.materials_status_processing_progress, progress.done, progress.total)
        } else {
            stringResource(R.string.materials_status_processing)
        }
    }
    MaterialStatus.READY -> stringResource(R.string.materials_status_ready)
    MaterialStatus.FAILED -> stringResource(R.string.materials_status_failed)
}
