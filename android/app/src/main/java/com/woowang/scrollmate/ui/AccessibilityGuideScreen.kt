package com.woowang.scrollmate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woowang.scrollmate.R

// 접근성 서비스를 켜는 방법을 알려 주는 화면. 서비스가 켜져 있으면 켜져 있다고 알려 주고 닫을 수 있음
@Composable
fun AccessibilityGuideScreen(
    enabled: Boolean,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(R.dimen.screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sheet_spacing)),
    ) {
        Text(
            text = stringResource(if (enabled) R.string.guide_enabled_title else R.string.guide_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(stringResource(R.string.guide_scope_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.guide_scope), style = MaterialTheme.typography.bodyLarge)

        Text(stringResource(R.string.guide_steps_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.guide_step_1), style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(R.string.guide_step_2, stringResource(R.string.service_label)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(stringResource(R.string.guide_step_3), style = MaterialTheme.typography.bodyLarge)

        Text(
            stringResource(R.string.guide_restricted, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(stringResource(R.string.guide_reinstall), style = MaterialTheme.typography.bodyMedium)

        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.guide_open_settings))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (enabled) R.string.guide_close else R.string.guide_later))
        }
    }
}
