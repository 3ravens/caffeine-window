package com.caffeinewindow.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Single-arc progress ring. Purely decorative: callers describe the state in text and
 * clear the semantics of the surrounding block.
 */
@Composable
fun WindowProgressRing(
    progress: Float,
    diameter: Dp,
    modifier: Modifier = Modifier,
    stroke: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "windowProgress",
    )
    Canvas(modifier = modifier.size(diameter).clearAndSetSemantics { }) {
        val strokePx = stroke.toPx()
        val inset = strokePx / 2f
        val arcSize = Size(size.width - strokePx, size.height - strokePx)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        if (animatedProgress > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Single-line text that shrinks to fit its width. The countdown ("1 h 12 min" at 64sp)
 * does not fit a phone-width ring at 200% font scale otherwise.
 */
@Composable
fun FittedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxSize: TextUnit = style.fontSize,
    minSize: TextUnit = 28.sp,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val measurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth
        val fontSize = remember(measurer, text, style, maxWidthPx, maxSize, minSize) {
            if (maxWidthPx == Constraints.Infinity || maxWidthPx <= 0) {
                maxSize
            } else {
                var candidate = maxSize
                while (candidate > minSize && measurer.measure(
                        text = text,
                        style = style.copy(fontSize = candidate),
                    ).size.width > maxWidthPx
                ) {
                    candidate = (candidate.value - 4f).coerceAtLeast(minSize.value).sp
                    if (candidate == minSize) break
                }
                candidate
            }
        }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = textAlign,
        )
    }
}
