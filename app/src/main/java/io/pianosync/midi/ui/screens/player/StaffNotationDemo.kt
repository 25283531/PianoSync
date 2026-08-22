package io.pianosync.midi.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

/**
 * 五线谱滚动 + 红色高亮效果的 Mock 演示。
 * 内置《小星星》C 大调旋律，自动循环播放，无需 MIDI 文件。
 * 用于在正式集成前确认视觉效果。
 */
@Composable
fun StaffNotationDemo() {
    // ---- Mock 数据：《小星星》120 BPM，1 拍 = 500ms ----
    val notes = remember {
        listOf(
            // 右手（高音谱表）：1 1 5 5 6 6 5 - | 4 4 3 3 2 2 1 -
            MidiNote(note = 60, isLeftHand = false, startTime = 0, duration = 500),
            MidiNote(note = 60, isLeftHand = false, startTime = 500, duration = 500),
            MidiNote(note = 67, isLeftHand = false, startTime = 1000, duration = 500),
            MidiNote(note = 67, isLeftHand = false, startTime = 1500, duration = 500),
            MidiNote(note = 69, isLeftHand = false, startTime = 2000, duration = 500),
            MidiNote(note = 69, isLeftHand = false, startTime = 2500, duration = 500),
            MidiNote(note = 67, isLeftHand = false, startTime = 3000, duration = 1000),
            MidiNote(note = 65, isLeftHand = false, startTime = 4000, duration = 500),
            MidiNote(note = 65, isLeftHand = false, startTime = 4500, duration = 500),
            MidiNote(note = 64, isLeftHand = false, startTime = 5000, duration = 500),
            MidiNote(note = 64, isLeftHand = false, startTime = 5500, duration = 500),
            MidiNote(note = 62, isLeftHand = false, startTime = 6000, duration = 500),
            MidiNote(note = 62, isLeftHand = false, startTime = 6500, duration = 500),
            MidiNote(note = 60, isLeftHand = false, startTime = 7000, duration = 1500),
            // 左手（低音谱表）：每小节一个根音长音
            MidiNote(note = 48, isLeftHand = true, startTime = 0, duration = 2000),
            MidiNote(note = 43, isLeftHand = true, startTime = 2000, duration = 2000),
            MidiNote(note = 48, isLeftHand = true, startTime = 4000, duration = 2000),
            MidiNote(note = 43, isLeftHand = true, startTime = 6000, duration = 2500)
        )
    }

    val totalDuration = 8500L

    // ---- 模拟播放时钟，约 60fps ----
    var currentTimeMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            currentTimeMs = (currentTimeMs + 16) % (totalDuration + 1500)
        }
    }

    val activeNotes = remember(notes, currentTimeMs) {
        notes.filter { it.startTime <= currentTimeMs && currentTimeMs < it.startTime + it.duration }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1115))
            .padding(12.dp)
    ) {
        Text(
            text = "五线谱演示 · Mock《小星星》",
            style = TextStyle(color = Color.White, fontSize = 16.sp),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "红色 = 当前弹奏音符　玫红 = 右手　蓝色 = 左手　灰色 = 已弹过",
            style = TextStyle(color = Color(0xFFB0B0B0), fontSize = 11.sp),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        StaffNotationCanvas(
            notes = notes,
            currentTimeMs = currentTimeMs,
            activeNotes = activeNotes,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Text(
            text = "播放位置: ${(currentTimeMs / 1000.0).format(1)}s / ${totalDuration / 1000.0}s（自动循环）",
            style = TextStyle(color = Color(0xFF888888), fontSize = 11.sp),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
        )
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

@Composable
private fun StaffNotationCanvas(
    notes: List<MidiNote>,
    currentTimeMs: Long,
    activeNotes: List<MidiNote>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // 尺寸常量（dp -> px 在 draw 时取）
    val staffSpacingDp = 9.dp
    val noteSpacingDp = 46.dp       // 一个四分音符的水平距离
    val playheadRatio = 0.32f       // 播放指针位于屏幕宽度的比例
    val leftPaddingDp = 70.dp       // 留给谱号/拍号

    val rightHandColor = Color(0xFFE91E63)
    val leftHandColor = Color(0xFF2196F3)
    val activeColor = Color(0xFFFF3030)
    val pastColor = Color(0xFF555555)
    val lineColor = Color(0xFFD0D0D0)
    val playheadColor = Color(0xFFFFC107)

    Canvas(modifier = modifier) {
        val staffSpacing = with(density) { staffSpacingDp.toPx() }
        val quarterPx = with(density) { noteSpacingDp.toPx() }
        val pxPerMs = quarterPx / 500f
        val leftPad = with(density) { leftPaddingDp.toPx() }
        val playheadX = size.width * playheadRatio

        // ---- 谱表几何布局 ----
        val trebleTopY = size.height * 0.18f
        val trebleBottomY = trebleTopY + staffSpacing * 4
        val gap = staffSpacing * 6
        val bassTopY = trebleBottomY + gap
        val bassBottomY = bassTopY + staffSpacing * 4

        // 绘制 5 条谱线（高/低各 5 条，横贯宽度）
        for (i in 0..4) {
            val ty = trebleTopY + staffSpacing * i
            val by = bassTopY + staffSpacing * i
            drawLine(lineColor, Offset(0f, ty), Offset(size.width, ty), strokeWidth = 1.2f)
            drawLine(lineColor, Offset(0f, by), Offset(size.width, by), strokeWidth = 1.2f)
        }

        // 左起粗纵线（连接大谱表）
        drawLine(
            lineColor,
            Offset(4f, trebleTopY),
            Offset(4f, bassBottomY),
            strokeWidth = 3f
        )

        // ---- 拍点 / 小节线（随时间滚动）----
        val measureMs = 2000L
        val firstBar = ((currentTimeMs - 6000) / measureMs) * measureMs
        val lastBar = ((currentTimeMs + 8000) / measureMs) * measureMs
        var t = firstBar
        while (t <= lastBar) {
            val x = playheadX + (t - currentTimeMs) * pxPerMs
            if (x in leftPad..size.width) {
                drawLine(
                    lineColor.copy(alpha = 0.5f),
                    Offset(x, trebleTopY),
                    Offset(x, bassBottomY),
                    strokeWidth = 1f
                )
            }
            t += measureMs
        }

        // ---- 绘制音符 ----
        val noteHeadW = staffSpacing * 1.5f
        val noteHeadH = staffSpacing * 1.05f
        val stemLength = staffSpacing * 3.2f

        for (note in notes) {
            val x = playheadX + (note.startTime - currentTimeMs) * pxPerMs
            if (x < leftPad - 40 || x > size.width + 40) continue

            val isActive = activeNotes.any { it.note == note.note && it.startTime == note.startTime }
            val isPast = note.startTime + note.duration < currentTimeMs

            val color = when {
                isActive -> activeColor
                isPast -> pastColor
                note.isLeftHand -> leftHandColor
                else -> rightHandColor
            }

            // 选择高/低音谱表
            val useTreble = note.note >= 55
            val refIdx = if (useTreble) DIATONIC_E4 else DIATONIC_G2
            val refY = if (useTreble) trebleBottomY else bassBottomY
            val staffTop = if (useTreble) trebleTopY else bassTopY
            val staffBottom = if (useTreble) trebleBottomY else bassBottomY
            val middleIdx = refIdx - 4 // 第三线（中间线）

            val idx = diatonicIndex(note.note)
            val y = refY + (refIdx - idx) * (staffSpacing / 2f)

            // 加线（ledger lines）
            drawLedgerLines(
                idx = idx,
                refIdx = refIdx,
                y = y,
                x = x,
                staffTop = staffTop,
                staffBottom = staffBottom,
                spacing = staffSpacing,
                noteHeadW = noteHeadW,
                color = color.copy(alpha = if (isActive) 1f else 0.7f)
            )

            // 符干方向：在中间线及以上符干朝下，否则朝上
            val stemDown = idx >= middleIdx
            val stemX = if (stemDown) x - noteHeadW / 2 else x + noteHeadW / 2
            val stemStartY = if (stemDown) y - noteHeadH / 2 else y + noteHeadH / 2
            val stemEndY = stemStartY + if (stemDown) -stemLength else stemLength

            // 红色发光效果
            if (isActive) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = noteHeadW * 0.95f,
                    center = Offset(x, y)
                )
            }

            drawLine(
                color = color,
                start = Offset(stemX, stemStartY),
                end = Offset(stemX, stemEndY),
                strokeWidth = staffSpacing * 0.18f
            )

            // 符头（旋转椭圆）
            rotate(-18f, pivot = Offset(x, y)) {
                drawOval(
                    color = color,
                    topLeft = Offset(x - noteHeadW / 2, y - noteHeadH / 2),
                    size = Size(noteHeadW, noteHeadH)
                )
                // 符头内描边
                drawOval(
                    color = Color.Black.copy(alpha = 0.25f),
                    topLeft = Offset(x - noteHeadW / 2, y - noteHeadH / 2),
                    size = Size(noteHeadW, noteHeadH),
                    style = Stroke(width = 1f)
                )
            }
        }

        // ---- 谱号 / 拍号（固定在左侧，不随滚动）----
        drawClefAndTimeSignature(
            textMeasurer = textMeasurer,
            trebleTopY = trebleTopY,
            bassTopY = bassTopY,
            bassBottomY = bassBottomY,
            leftPad = leftPad,
            lineColor = lineColor
        )

        // ---- 播放指针（固定竖线）----
        drawLine(
            playheadColor,
            Offset(playheadX, trebleTopY - 10),
            Offset(playheadX, bassBottomY + 10),
            strokeWidth = 2.5f
        )
        // 指针顶端圆点
        drawCircle(
            color = playheadColor,
            radius = 5f,
            center = Offset(playheadX, trebleTopY - 14)
        )
    }
}

/** 绘制加线（音符超出五线谱范围时） */
private fun DrawScope.drawLedgerLines(
    idx: Int,
    refIdx: Int,
    y: Float,
    x: Float,
    staffTop: Float,
    staffBottom: Float,
    spacing: Float,
    noteHeadW: Float,
    color: Color
) {
    val step = spacing / 2f
    // 下方加线（idx 比 refIdx 小，y 比 staffBottom 大）
    if (y > staffBottom) {
        var lineIdx = refIdx - 2
        while (lineIdx >= idx) {
            val lineY = staffBottom + (refIdx - lineIdx) * step
            drawLine(
                color,
                Offset(x - noteHeadW * 0.75f, lineY),
                Offset(x + noteHeadW * 0.75f, lineY),
                strokeWidth = 1.3f
            )
            lineIdx -= 2
        }
    }
    // 上方加线
    if (y < staffTop) {
        var lineIdx = refIdx - 8 + 2 // top line is refIdx - 8
        while (lineIdx <= idx) {
            val lineY = staffTop - (lineIdx - (refIdx - 8)) * step
            drawLine(
                color,
                Offset(x - noteHeadW * 0.75f, lineY),
                Offset(x + noteHeadW * 0.75f, lineY),
                strokeWidth = 1.3f
            )
            lineIdx += 2
        }
    }
}

/** 绘制谱号与拍号（使用 Unicode 字符，失败时退化为字母标签） */
private fun DrawScope.drawClefAndTimeSignature(
    textMeasurer: TextMeasurer,
    trebleTopY: Float,
    bassTopY: Float,
    bassBottomY: Float,
    leftPad: Float,
    lineColor: Color
) {
    val trebleClef = textMeasurer.measure(
        AnnotatedString("𝄞"),
        style = TextStyle(color = lineColor, fontSize = 52.sp, textAlign = TextAlign.Center)
    )
    drawText(
        trebleClef,
        topLeft = Offset(8f, trebleTopY - 22)
    )
    val bassClef = textMeasurer.measure(
        AnnotatedString("𝄢"),
        style = TextStyle(color = lineColor, fontSize = 42.sp, textAlign = TextAlign.Center)
    )
    drawText(
        bassClef,
        topLeft = Offset(12f, bassTopY - 6)
    )

    // 拍号 4/4
    val fourTop = textMeasurer.measure(
        AnnotatedString("4"),
        style = TextStyle(color = lineColor, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    )
    val fourBottom = textMeasurer.measure(
        AnnotatedString("4"),
        style = TextStyle(color = lineColor, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    )
    drawText(fourTop, topLeft = Offset(leftPad - 24, trebleTopY - 2))
    drawText(fourBottom, topLeft = Offset(leftPad - 24, trebleTopY + 16))
}

// ---- 音程计算工具（与后续 MusicTheory.kt 保持一致）----

/** 白键在自然音阶中的偏移：C=0 D=1 E=2 F=3 G=4 A=5 B=6；黑键映射到同位置自然音 */
private val WHITE_KEY_OFFSET = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)

/** MIDI 音符号 -> 全局自然音级索引（C4=35） */
private fun diatonicIndex(midiNote: Int): Int {
    val octave = midiNote / 12
    return octave * 7 + WHITE_KEY_OFFSET[midiNote % 12]
}

// 参考音级
private const val DIATONIC_E4 = 37 // 高音谱表第一线（底线）
private const val DIATONIC_G2 = 25 // 低音谱表第一线（底线）
