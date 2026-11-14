package com.woowang.scrollmate.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 색은 res/values/colors.xml과 같은 값. 쇼츠 위 카드(XML 뷰)와 앱 화면(Compose)의 색을 맞춤
object Brand {
    val Primary = Color(0xFF4F46E5)
    val PrimaryDark = Color(0xFF3B2FC9)
    val PrimaryLight = Color(0xFF7C6CF6)
    val Soft = Color(0xFFEEEDFE)
    val Accent = Color(0xFFFFB020)
    val Ink = Color(0xFF17171F)
    val InkSub = Color(0xFF6B6B7B)
    val Line = Color(0xFFE7E7EF)
    val Background = Color(0xFFF6F6FA)
    val Success = Color(0xFF16A34A)
    val SuccessSoft = Color(0xFFE8F7EE)
    val Danger = Color(0xFFDC2626)
    val DangerSoft = Color(0xFFFFEFEF)
    val Warning = Color(0xFFB45309)
    val WarningSoft = Color(0xFFFFF4E0)
}

private val colors = lightColorScheme(
    primary = Brand.Primary,
    onPrimary = Color.White,
    primaryContainer = Brand.Soft,
    onPrimaryContainer = Brand.PrimaryDark,
    secondary = Brand.Accent,
    onSecondary = Brand.Ink,
    background = Brand.Background,
    onBackground = Brand.Ink,
    surface = Color.White,
    onSurface = Brand.Ink,
    surfaceVariant = Color(0xFFF1F1F6),
    onSurfaceVariant = Brand.InkSub,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF1F1F6),
    outline = Color(0xFFD5D5E0),
    outlineVariant = Brand.Line,
    error = Brand.Danger,
)

private val base = Typography()

private val typography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val CaptionStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = Brand.InkSub)

@Composable
fun ScrollMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
}
