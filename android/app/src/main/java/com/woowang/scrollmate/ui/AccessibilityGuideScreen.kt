package com.woowang.scrollmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Brand.PrimaryDark, Brand.PrimaryLight))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(50.dp),
            )
        }
        Text(
            text = stringResource(if (enabled) R.string.guide_enabled_title else R.string.guide_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        AppCard(containerColor = Brand.Soft, border = false) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painterResource(R.drawable.ic_shield),
                    contentDescription = null,
                    tint = Brand.Primary,
                    modifier = Modifier.size(22.dp),
                )
                Column(Modifier.padding(start = 10.dp)) {
                    Text(stringResource(R.string.guide_scope_title), style = MaterialTheme.typography.titleSmall, color = Brand.PrimaryDark)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.guide_scope), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        AppCard {
            Text(stringResource(R.string.guide_steps_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Step(1, stringResource(R.string.guide_step_1))
            Step(2, stringResource(R.string.guide_step_2, stringResource(R.string.service_label)))
            Step(3, stringResource(R.string.guide_step_3))
        }

        Text(stringResource(R.string.guide_restricted, stringResource(R.string.app_name)), style = CaptionStyle)
        Text(stringResource(R.string.guide_reinstall), style = CaptionStyle)

        Spacer(Modifier.height(4.dp))
        PrimaryButton(text = stringResource(R.string.guide_open_settings), onClick = onOpenSettings)
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (enabled) R.string.guide_close else R.string.guide_later), color = Brand.InkSub)
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Brand.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        // 문구 앞의 "1. " 같은 번호는 원 안에 따로 그리므로 뺌
        Text(
            text = text.replaceFirst(Regex("^\\d+\\.\\s*"), ""),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}
