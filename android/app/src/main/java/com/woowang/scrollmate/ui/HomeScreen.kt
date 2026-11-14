package com.woowang.scrollmate.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.woowang.scrollmate.domain.SubjectCard

// 시험이 이만큼 남으면 D-day 표시를 강조함. 보기 좋게 임의로 정한 값
private const val URGENT_DAYS = 3L

// 앱을 열면 처음 보는 화면. 카드 켜짐 상태, 자료 올리기, 학습 주제와 시험일까지 남은 날짜를 보여 줌
@Composable
fun HomeScreen(
    cards: List<SubjectCard>,
    serviceEnabled: Boolean,
    onToggleEnabled: (SubjectCard, Boolean) -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (SubjectCard) -> Unit,
    failureCount: Int,
    onMaterialsClick: () -> Unit,
    onUploadClick: () -> Unit,
    onGuideClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { HomeHeader(serviceEnabled, onGuideClick) }
        item { HeroCard(onUploadClick) }
        if (!serviceEnabled) {
            item { ServiceOffCard(onGuideClick) }
        }
        if (failureCount > 0) {
            item { UploadFailureBanner(failureCount, onMaterialsClick) }
        }
        item {
            AppCard(onClick = onMaterialsClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(R.drawable.ic_folder, Brand.Primary, Brand.Soft)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                    ) {
                        Text(stringResource(R.string.home_materials_button), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.home_materials_desc), style = CaptionStyle)
                    }
                    Icon(painterResource(R.drawable.ic_chevron), contentDescription = null, tint = Brand.InkSub)
                }
            }
        }
        item {
            SectionHeader(stringResource(R.string.home_subjects_title)) {
                TextButton(onClick = onAddSubject) {
                    Icon(painterResource(R.drawable.ic_plus), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.subject_add))
                }
            }
        }
        if (cards.isEmpty()) {
            item {
                AppCard(containerColor = Color.White) {
                    Text(
                        text = stringResource(R.string.home_subjects_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Brand.InkSub,
                    )
                }
            }
        } else {
            items(cards, key = { it.subject?.id ?: UNCLASSIFIED_KEY }) { card ->
                SubjectCardItem(card, onToggleEnabled, onEditSubject)
            }
        }
        item {
            TextButton(onClick = onGuideClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_guide_button), color = Brand.InkSub)
            }
        }
    }
}

private const val UNCLASSIFIED_KEY = -1L

@Composable
private fun HomeHeader(serviceEnabled: Boolean, onGuideClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Brand.PrimaryDark, Brand.PrimaryLight))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_logo_mark),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        val (label, color, bg) = if (serviceEnabled) {
            Triple(stringResource(R.string.home_service_on), Brand.Success, Brand.SuccessSoft)
        } else {
            Triple(stringResource(R.string.home_service_off), Brand.Warning, Brand.WarningSoft)
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(bg)
                .clickable(onClick = onGuideClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeroCard(onUploadClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Brand.PrimaryDark, Brand.PrimaryLight)))
            .padding(22.dp),
    ) {
        Text(
            text = stringResource(R.string.home_tagline),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 32.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_hero_body),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onUploadClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.Primary),
        ) {
            Icon(painterResource(R.drawable.ic_upload), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.upload_button), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ServiceOffCard(onGuideClick: () -> Unit) {
    AppCard(onClick = onGuideClick, containerColor = Brand.WarningSoft, border = false) {
        Text(stringResource(R.string.home_service_off_title), style = MaterialTheme.typography.titleMedium, color = Brand.Warning)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.home_service_off_body), style = MaterialTheme.typography.bodyMedium, color = Brand.Ink)
    }
}

// 눌러서 이름과 시험일을 고침. 학습 주제가 없는 자료를 모은 미분류 카드는 고칠 대상이 없음
@Composable
private fun SubjectCardItem(
    card: SubjectCard,
    onToggleEnabled: (SubjectCard, Boolean) -> Unit,
    onEdit: (SubjectCard) -> Unit,
) {
    val subject = card.subject
    AppCard(onClick = if (subject != null) ({ onEdit(card) }) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subject?.name ?: stringResource(R.string.subject_unclassified),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    card.daysLeft?.let {
                        Spacer(Modifier.width(8.dp))
                        DdayPill(it)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.home_materials_count, card.materialCount), style = CaptionStyle)
            }
            // 학습 주제가 없는 자료(미분류)는 켜고 끌 대상이 없음
            if (subject != null) {
                Switch(
                    checked = subject.enabled,
                    onCheckedChange = { onToggleEnabled(card, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Brand.Primary),
                )
            }
        }
    }
}

@Composable
private fun DdayPill(daysLeft: Long) {
    when {
        daysLeft < 0 -> Pill(stringResource(R.string.home_dday_past), Brand.InkSub, Color(0xFFF1F1F6))
        daysLeft == 0L -> Pill(stringResource(R.string.home_dday_today), Color.White, Brand.Danger)
        daysLeft <= URGENT_DAYS -> Pill(stringResource(R.string.home_dday_left, daysLeft), Brand.Danger, Brand.DangerSoft)
        else -> Pill(stringResource(R.string.home_dday_left, daysLeft), Brand.Primary, Brand.Soft)
    }
}

// 백그라운드 업로드가 실패했을 때 홈에서 바로 알려 줌. 이유와 다음 행동은 자료 목록에서 보여 줌
@Composable
private fun UploadFailureBanner(count: Int, onMaterialsClick: () -> Unit) {
    AppCard(onClick = onMaterialsClick, containerColor = Brand.DangerSoft, border = false) {
        Text(
            text = stringResource(R.string.home_upload_failed_banner, count),
            style = MaterialTheme.typography.titleMedium,
            color = Brand.Danger,
        )
        Spacer(Modifier.height(2.dp))
        Text(text = stringResource(R.string.home_upload_failed_action), style = CaptionStyle)
    }
}
