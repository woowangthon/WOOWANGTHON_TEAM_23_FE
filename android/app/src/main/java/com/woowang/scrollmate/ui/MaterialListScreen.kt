package com.woowang.scrollmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = stringResource(R.string.materials_title), onBack = onBack) {
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(painterResource(R.drawable.ic_refresh), contentDescription = stringResource(R.string.materials_refresh))
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(failures, key = { "failure-${it.id}" }) { failure ->
                FailureCard(failure, onDismiss = { onDismissFailure(failure) })
            }

            if (items.isEmpty()) {
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.materials_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Brand.InkSub,
                        )
                    }
                }
            } else {
                items(items, key = { "material-${it.material.id}" }) { item ->
                    MaterialCard(item, onDelete = { onDelete(item) })
                }
            }
        }
    }
}

@Composable
private fun FailureCard(failure: UploadFailure, onDismiss: () -> Unit) {
    AppCard(containerColor = Brand.DangerSoft, border = false) {
        Text(
            text = failureMessage(failure),
            style = MaterialTheme.typography.bodyMedium,
            color = Brand.Ink,
        )
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.materials_failure_dismiss), color = Brand.Danger)
        }
    }
}

@Composable
private fun failureMessage(failure: UploadFailure): String {
    val names = failure.fileNames.joinToString()
    val message = reasonMessage(failure, names)
    val others = failure.alsoNotUploaded
    if (others.isEmpty()) return message
    return message + "\n" + stringResource(R.string.upload_failed_also_not_uploaded, others.joinToString())
}

@Composable
private fun reasonMessage(failure: UploadFailure, names: String): String {
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
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FileBadge(material.name)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            ) {
                Text(
                    text = material.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.subjectName ?: stringResource(R.string.subject_unclassified),
                    style = CaptionStyle,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(material.status)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.materials_question_count, material.questionCount),
                style = CaptionStyle,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.materials_delete), color = Brand.InkSub)
            }
        }
        val progress = item.progress
        if (material.status == MaterialStatus.PROCESSING) {
            Spacer(Modifier.height(6.dp))
            Text(text = statusText(item), style = CaptionStyle)
            Spacer(Modifier.height(8.dp))
            if (progress != null && progress.total > 0) {
                LinearProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Brand.Primary,
                    trackColor = Brand.Soft,
                    drawStopIndicator = {},
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Brand.Primary,
                    trackColor = Brand.Soft,
                )
            }
        } else if (material.status == MaterialStatus.FAILED) {
            Spacer(Modifier.height(6.dp))
            Text(text = statusText(item), style = MaterialTheme.typography.bodyMedium, color = Brand.Danger)
        }
    }
}

@Composable
private fun StatusPill(status: MaterialStatus) = when (status) {
    MaterialStatus.PROCESSING -> Pill(stringResource(R.string.materials_pill_processing), Brand.Primary, Brand.Soft)
    MaterialStatus.READY -> Pill(stringResource(R.string.materials_pill_ready), Brand.Success, Brand.SuccessSoft)
    MaterialStatus.FAILED -> Pill(stringResource(R.string.materials_pill_failed), Brand.Danger, Brand.DangerSoft)
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
