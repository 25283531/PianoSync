package io.pianosync.midi.util

/**
 * 音乐理论工具：MIDI 音符号到唱名、音名、音级索引、变音记号等的转换。
 *
 * MIDI 音符号约定：中央 C = 60 (C4)，A4 = 69。
 * 唱名采用 C 大调首调（与固定调一致）：C=1 D=2 E=3 F=4 G=5 A=6 B=7。
 */
object MusicTheory {

    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** 白键在自然音阶中的偏移：C=0 D=1 E=2 F=3 G=4 A=5 B=6；黑键映射到同名自然音位置。 */
    private val WHITE_KEY_OFFSET = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)

    /** C 大调唱名数字（黑键回退到其下方自然音的唱名）。 */
    private val SOLFEGE_NAMES = arrayOf("1", "1", "2", "2", "3", "4", "4", "5", "5", "6", "6", "7")

    /**
     * 返回 MIDI 音的音名，如 "C4"、"C#4"。
     */
    fun noteName(midiNote: Int): String {
        val octave = (midiNote / 12) - 1
        return "${NOTE_NAMES[midiNote % 12]}$octave"
    }

    /**
     * 是否为白键（自然音）。
     */
    fun isWhiteKey(midiNote: Int): Boolean = when (midiNote % 12) {
        0, 2, 4, 5, 7, 9, 11 -> true
        else -> false
    }

    /**
     * 返回 C 大调唱名数字（"1"~"7"）。黑键返回其所在位置的自然音唱名。
     */
    fun solfege(midiNote: Int): String = SOLFEGE_NAMES[midiNote % 12]

    /**
     * 以中央 C (C4=60) 为第 4 八度，返回该音相对的八度偏移：
     * 正数表示高八度（数字上方加点），负数表示低八度（下方加点），0 为中央八度。
     */
    fun octaveOffset(midiNote: Int): Int {
        // C4 = 60 为基准八度；每过一个 C (note % 12 == 0) 进入新八度
        val octave = (midiNote / 12) - 1 // MIDI octave
        return octave - 4 // C4 所在八度记为 0
    }

    /**
     * MIDI 音符号 -> 全局自然音级索引（C4=35）。用于五线谱纵向定位：
     * 相邻返回值相差 1，对应五线谱上相邻的"线/间"。
     */
    fun diatonicIndex(midiNote: Int): Int {
        val octave = midiNote / 12
        return octave * 7 + WHITE_KEY_OFFSET[midiNote % 12]
    }

    /**
     * 变音记号类型。
     */
    enum class Accidental { NONE, SHARP, FLAT }

    /**
     * C 大调下该音的变音记号（黑键即升号，白键无）。
     */
    fun accidental(midiNote: Int): Accidental = when (midiNote % 12) {
        1, 3, 6, 8, 10 -> Accidental.SHARP
        else -> Accidental.NONE
    }
}
