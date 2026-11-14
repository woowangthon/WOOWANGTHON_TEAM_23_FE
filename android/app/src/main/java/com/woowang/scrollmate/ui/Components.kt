package com.woowang.scrollmate.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woowang.scrollmate.R

private val CardShape = RoundedCornerShape(20.dp)

// 앱 화면에서 공통으로 쓰는 흰 카드. 그림자 대신 옅은 테두리로 구분함
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = Color.White,
    border: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = containerColor)
    val stroke = if (border) BorderStroke(1.dp, Brand.Line) else null
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = CardShape, colors = colors, border = stroke) {
            Column(Modifier.padding(18.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = CardShape, colors = colors, border = stroke) {
            Column(Modifier.padding(18.dp), content = content)
        }
    }
}

@Composable
fun ScreenTopBar(title: String, onBack: (() -> Unit)? = null, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = null, tint = Brand.Ink)
            }
        } else {
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        action?.invoke()
    }
}

@Composable
fun Pill(text: String, color: Color, background: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand.Primary),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun IconBubble(@DrawableRes icon: Int, tint: Color, background: Color, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size / 2))
    }
}

// 파일 이름의 확장자로 PDF, PPT 표시를 만듦
@Composable
fun FileBadge(fileName: String) {
    val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
    val label = if (isPdf) "PDF" else "PPT"
    val color = if (isPdf) Color(0xFFE5484D) else Color(0xFFF76B15)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SectionHeader(title: String, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        action?.invoke()
    }
}
