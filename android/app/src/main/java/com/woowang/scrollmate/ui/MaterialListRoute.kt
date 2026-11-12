package com.woowang.scrollmate.ui

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.woowang.scrollmate.domain.MaterialItem
import com.woowang.scrollmate.domain.buildMaterialItems
import com.woowang.scrollmate.net.ApiException
import com.woowang.scrollmate.net.NetworkException
import kotlinx.coroutines.launch

// 기기에 저장된 자료와 올리지 못한 기록을 읽어 자료 목록 화면에 넘김.
// 열 때와 새로고침을 누를 때 서버에서 자료 상태를 다시 받음
@Composable
fun MaterialListRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as ScrollMateApp).container
    val repository = container.materialRepository
    val scope = rememberCoroutineScope()

    val materials by repository.observeMaterials().collectAsState(initial = emptyList())
    val subjects by container.database.subjectDao().observeAll().collectAsState(initial = emptyList())
    val progress by repository.progress.collectAsState()
    val failures by container.uploadFailures.failures.collectAsState()
    val items = remember(materials, subjects, progress) { buildMaterialItems(materials, subjects, progress) }

    var refreshing by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MaterialItem?>(null) }

    fun toast(message: Int) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun refresh() {
        scope.launch {
            refreshing = true
            try {
                repository.refreshMaterials()
            } catch (e: NetworkException) {
                toast(R.string.materials_refresh_failed)
            } catch (e: ApiException) {
                toast(R.string.materials_refresh_failed)
            } finally {
                refreshing = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    MaterialListScreen(
        items = items,
        failures = failures,
        refreshing = refreshing,
        onRefresh = ::refresh,
        onDelete = { pendingDelete = it },
        onDismissFailure = { container.uploadFailures.dismiss(it.id) },
        onBack = onBack,
    )

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.materials_delete_confirm_title, item.material.name)) },
            text = { Text(stringResource(R.string.materials_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        scope.launch {
                            try {
                                repository.delete(item.material.id)
                            } catch (e: NetworkException) {
                                toast(R.string.materials_delete_failed)
                            } catch (e: ApiException) {
                                toast(R.string.materials_delete_failed)
                            }
                        }
                    },
                ) { Text(stringResource(R.string.materials_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.subject_cancel)) }
            },
        )
    }
}
