package io.pianosync.midi.ui.screens.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.pianosync.midi.ui.screens.player.HandMode
import io.pianosync.midi.ui.screens.player.MidiNote
import io.pianosync.midi.util.MusicTheory
import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface

/**
 * 大谱表（高音 + 低音）五线谱视图。
 *
 * - 音符随 [currentTimeMs] 从右向左流动，当前弹奏位置的播放指针固定在屏幕中央。
 * - 当前激活的音符：右手显示红色、左手显示蓝色，并带同色发光。
 * - 尚未弹奏的音符显示白色，已弹奏过的音符显示浅灰色。
 *
 * @param notes 全部音符
 * @param currentTimeMs 当前播放时间（毫秒）
 * @param activeNotes 当前正在播放/应弹奏的音符（红色/蓝色高亮）
 * @param bpm 当前播放 BPM，用于决定滚动像素/毫秒
 * @param originalBpm MIDI 原始 BPM，用于将音符时间换算为视觉距离
 * @param handMode 只显示右手/左手/双手
 */
@Composable
fun StaffNotationView(
    notes: List<MidiNote>,
    currentTimeMs: Long,
    activeNotes: List<MidiNote>,
    bpm: Int,
    originalBpm: Int,
    handMode: HandMode,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    val staffSpacingDp = 10.dp
    val noteSpacingDp = 64.dp
    val leftPaddingDp = 70.dp

    val futureColor = Color(0xFFFFFFFF)
    val activeRightColor = Color(0xFFFF3030)
    val activeLeftColor = Color(0xFF2196F3)
    val pastColor = Color(0xFF9A9A9A)
    val lineColor = Color(0xFFD0D0D0)
    val playheadColor = Color(0xFFFFC107)

    // 用原生 Paint 绘制谱号/拍号文字（兼容 Compose UI 1.6，不依赖 1.7 的 drawText）
    val trebleClefPaint = remember(lineColor) {
        AndroidPaint().apply {
            color = lineColor.toArgb()
            textSize = with(density) { 52.sp.toPx() }
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
    }
    val bassClefPaint = remember(lineColor) {
        AndroidPaint().apply {
            color = lineColor.toArgb()
            textSize = with(density) { 42.sp.toPx() }
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
    }
    val timeSigPaint = remember(lineColor) {
        AndroidPaint().apply {
            color = lineColor.toArgb()
            textSize = with(density) { 18.sp.toPx() }
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = AndroidPaint.Align.CENTER
        }
    }

    // 根据手模式过滤音符
    val visibleNotes = when (handMode) {
        HandMode.BOTH_HANDS -> notes
        HandMode.RIGHT_HAND_ONLY -> notes.filter { !it.isLeftHand }
        HandMode.LEFT_HAND_ONLY -> notes.filter { it.isLeftHand }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val staffSpacing = with(density) { staffSpacingDp.toPx() }
        val quarterPx = with(density) { noteSpacingDp.toPx() }
        val leftPad = with(density) { leftPaddingDp.toPx() }
        val playheadX = size.width * 0.5f

        // 滚动速度：以原始 BPM 下 500ms 为一个四分音符格；变速时按比例调整
        val baseQuarterMs = 60_000f / originalBpm.coerceAtLeast(1)
        val speedRatio = bpm.toFloat() / originalBpm.coerceAtLeast(1)
        val pxPerMs = (quarterPx / baseQuarterMs) * speedRatio

        // ---- 谱表几何布局 ----
        val trebleTopY = size.height * 0.18f
        val trebleBottomY = trebleTopY + staffSpacing * 4
        val gap = staffSpacing * 6
        val bassTopY = trebleBottomY + gap
        val bassBottomY = bassTopY + staffSpacing * 4

        // 5 条谱线
        for (i in 0..4) {
            val ty = trebleTopY + staffSpacing * i
            val by = bassTopY + staffSpacing * i
            drawLine(lineColor, Offset(0f, ty), Offset(size.width, ty), strokeWidth = 1.2f)
            drawLine(lineColor, Offset(0f, by), Offset(size.width, by), strokeWidth = 1.2f)
        }
        // 左侧粗纵线
        drawLine(lineColor, Offset(4f, trebleTopY), Offset(4f, bassBottomY), strokeWidth = 3f)

        // 小节线（按时间滚动，假设 4/4 拍、每拍 baseQuarterMs）
        val measureMs = (baseQuarterMs * 4).toLong()
        val firstBar = ((currentTimeMs - 6000) / measureMs) * measureMs
        val lastBar = ((currentTimeMs + 8000) / measureMs) * measureMs
        var t = firstBar
        while (t <= lastBar) {
            val x = playheadX + (t - currentTimeMs) * pxPerMs
            if (x in leftPad..size.width) {
                drawLine(
                    lineColor.copy(alpha = 0.5f),
                    Offset(x, trebleTopY), Offset(x, bassBottomY),
                    strokeWidth = 1f
                )
            }
            t += measureMs
        }

        val noteHeadW = staffSpacing * 1.5f
        val noteHeadH = staffSpacing * 1.05f
        val stemLength = staffSpacing * 3.2f

        for (note in visibleNotes) {
            val x = playheadX + (note.startTime - currentTimeMs) * pxPerMs
            if (x < leftPad - 40 || x > size.width + 40) continue

            val isActive = activeNotes.any { it.note == note.note && it.startTime == note.startTime }
            val isPast = note.startTime + note.duration < currentTimeMs

            val color = when {
                isActive && note.isLeftHand -> activeLeftColor
                isActive -> activeRightColor
                isPast -> pastColor
                else -> futureColor
            }

            // 高音/低音谱表选择：中央 C (60) 为分界
            val useTreble = note.note >= 55
            val refIdx = if (useTreble) DIATONIC_E4 else DIATONIC_G2
            val refY = if (useTreble) trebleBottomY else bassBottomY
            val staffTop = if (useTreble) trebleTopY else bassTopY
            val staffBottom = if (useTreble) trebleBottomY else bassBottomY
            val middleIdx = refIdx - 4

            val idx = MusicTheory.diatonicIndex(note.note)
            val y = refY + (refIdx - idx) * (staffSpacing / 2f)

            drawLedgerLines(idx, refIdx, y, x, staffTop, staffBottom, staffSpacing, noteHeadW,
                color.copy(alpha = if (isActive) 1f else 0.85f))

            val stemDown = idx >= middleIdx
            val stemX = if (stemDown) x - noteHeadW / 2 else x + noteHeadW / 2
            val stemStartY = if (stemDown) y - noteHeadH / 2 else y + noteHeadH / 2
            val stemEndY = stemStartY + if (stemDown) -stemLength else stemLength

            if (isActive) {
                drawCircle(color = color.copy(alpha = 0.45f), radius = noteHeadW * 1.1f, center = Offset(x, y))
            }

            drawLine(
                color = color,
                start = Offset(stemX, stemStartY),
                end = Offset(stemX, stemEndY),
                strokeWidth = staffSpacing * 0.18f
            )

            rotate(-18f, pivot = Offset(x, y)) {
                drawOval(color = color, topLeft = Offset(x - noteHeadW / 2, y - noteHeadH / 2),
                    size = Size(noteHeadW, noteHeadH))
                val outlineColor = if (color == futureColor) Color(0xFF1A1A1A)
                else Color.Black.copy(alpha = 0.3f)
                drawOval(
                    color = outlineColor,
                    topLeft = Offset(x - noteHeadW / 2, y - noteHeadH / 2),
                    size = Size(noteHeadW, noteHeadH),
                    style = Stroke(width = if (color == futureColor) 1.6f else 1f)
                )
            }
        }

        // 谱号 / 拍号（固定左侧）
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            // 高音谱号 𝄞：基线对齐到高音谱表底部附近
            nativeCanvas.drawText("𝄞", 8f, trebleBottomY - staffSpacing * 0.3f, trebleClefPaint)
            // 低音谱号 𝄢：基线对齐到低音谱表底部附近
            nativeCanvas.drawText("𝄢", 12f, bassBottomY - staffSpacing * 0.3f, bassClefPaint)
            // 4/4 拍号：上下两个 4
            val sigX = leftPad - 12f
            nativeCanvas.drawText("4", sigX, trebleTopY + staffSpacing * 1.3f, timeSigPaint)
            nativeCanvas.drawText("4", sigX, trebleTopY + staffSpacing * 3.6f, timeSigPaint)
        }

        // 播放指针
        drawLine(playheadColor, Offset(playheadX, trebleTopY - 10), Offset(playheadX, bassBottomY + 10),
            strokeWidth = 2.5f)
        drawCircle(color = playheadColor, radius = 5f, center = Offset(playheadX, trebleTopY - 14))
    }
}

private fun DrawScope.drawLedgerLines(
    idx: Int, refIdx: Int, y: Float, x: Float,
    staffTop: Float, staffBottom: Float, spacing: Float, noteHeadW: Float, color: Color
) {
    val step = spacing / 2f
    if (y > staffBottom) {
        var lineIdx = refIdx - 2
        while (lineIdx >= idx) {
            val lineY = staffBottom + (refIdx - lineIdx) * step
            drawLine(color, Offset(x - noteHeadW * 0.75f, lineY), Offset(x + noteHeadW * 0.75f, lineY),
                strokeWidth = 1.3f)
            lineIdx -= 2
        }
    }
    if (y < staffTop) {
        var lineIdx = refIdx - 8 + 2
        while (lineIdx <= idx) {
            val lineY = staffTop - (lineIdx - (refIdx - 8)) * step
            drawLine(color, Offset(x - noteHeadW * 0.75f, lineY), Offset(x + noteHeadW * 0.75f, lineY),
                strokeWidth = 1.3f)
            lineIdx += 2
        }
    }
}

// 参考音级（与 MusicTheory.diatonicIndex 对应）
private const val DIATONIC_E4 = 37 // 高音谱表第一线
private const val DIATONIC_G2 = 25 // 低音谱表第一线
