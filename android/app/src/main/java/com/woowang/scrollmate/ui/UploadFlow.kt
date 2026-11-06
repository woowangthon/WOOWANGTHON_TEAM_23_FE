package com.woowang.scrollmate.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.woowang.scrollmate.R
import com.woowang.scrollmate.ScrollMateApp
import com.woowang.scrollmate.data.Subject
import com.woowang.scrollmate.domain.MAX_UPLOAD_FILES
import com.woowang.scrollmate.domain.MAX_UPLOAD_FILE_BYTES
import com.woowang.scrollmate.domain.UploadCandidate
import com.woowang.scrollmate.domain.UploadSelection
import com.woowang.scrollmate.domain.selectForUpload
import com.woowang.scrollmate.upload.CachedFile
import com.woowang.scrollmate.upload.SharedFiles
import com.woowang.scrollmate.upload.UploadScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val BYTES_PER_MB = 1024 * 1024

// 파일을 받아 올리기 시트를 보여 주고, 확인하면 과목을 정해 백그라운드 업로드를 예약함.
// 직접 올리기(MainActivity)와 공유 수신(ShareReceiverActivity)이 같이 씀
@Composable
fun UploadFlow(uris: List<Uri>, onFinished: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as ScrollMateApp).container
    val scope = rememberCoroutineScope()
    val subjects by container.database.subjectDao().observeAll().collectAsState(initial = emptyList<Subject>())

    var accepted by remember(uris) { mutableStateOf<List<Pair<Uri, UploadCandidate>>?>(null) }
    var selection by remember(uris) { mutableStateOf<UploadSelection?>(null) }

    LaunchedEffect(uris) {
        val described = withContext(Dispatchers.IO) {
            uris.map { it to SharedFiles.describe(context, it) }
        }
        val result = selectForUpload(described.map { it.second })
        selection = result
        accepted = described.filter { (_, candidate) -> result.accepted.any { it === candidate } }
        if (result.accepted.isEmpty()) {
            Toast.makeText(context, R.string.upload_nothing_to_upload, Toast.LENGTH_SHORT).show()
            onFinished()
        }
    }

    val files = accepted ?: return
    val picked = selection ?: return
    if (files.isEmpty()) return

    val warnings = buildList {
        if (picked.unsupported.isNotEmpty()) {
            add(stringResource(R.string.upload_unsupported, picked.unsupported.joinToString { it.name }))
        }
        if (picked.tooLarge.isNotEmpty()) {
            add(
                stringResource(
                    R.string.upload_too_large,
                    (MAX_UPLOAD_FILE_BYTES / BYTES_PER_MB).toInt(),
                    picked.tooLarge.joinToString { it.name },
                ),
            )
        }
        if (picked.overLimit.isNotEmpty()) {
            add(stringResource(R.string.upload_over_limit, MAX_UPLOAD_FILES, picked.overLimit.size))
        }
    }

    UploadSheet(
        fileNames = files.map { it.second.name },
        warnings = warnings,
        subjects = subjects,
        onDismiss = onFinished,
        onConfirm = { choice ->
            scope.launch {
                val subjectId = when (choice) {
                    SubjectChoice.Unclassified -> null
                    is SubjectChoice.Existing -> choice.subjectId
                    is SubjectChoice.New -> container.database.subjectDao().insert(Subject(name = choice.name))
                }
                val cached = withContext(Dispatchers.IO) {
                    files.mapNotNull { (uri, candidate) -> SharedFiles.copyToCache(context, uri, candidate) }
                }
                enqueueAndNotify(context, cached, subjectId)
                onFinished()
            }
        },
    )
}

private fun enqueueAndNotify(context: Context, cached: List<CachedFile>, subjectId: Long?) {
    if (cached.isEmpty()) {
        Toast.makeText(context, R.string.upload_copy_failed, Toast.LENGTH_SHORT).show()
        return
    }
    UploadScheduler.enqueue(context, cached, subjectId)
    Toast.makeText(context, R.string.upload_started, Toast.LENGTH_SHORT).show()
}
