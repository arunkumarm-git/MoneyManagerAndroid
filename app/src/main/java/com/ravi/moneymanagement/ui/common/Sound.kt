package com.ravi.moneymanagement.ui.common

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.ravi.moneymanagement.R
import com.ravi.moneymanagement.settings.AppSettings

/**
 * Plays a short, soft click sound on tap, gated by [AppSettings]. Uses a bundled WAV
 * (res/raw/click.wav) via SoundPool rather than a synthesized ToneGenerator beep, and
 * AudioAttributes.USAGE_MEDIA so it tracks the regular, always-visible media volume —
 * the stream apps normally use for UI sound effects — rather than a separately muted one.
 */
object ClickSound {
    private var soundPool: SoundPool? = null
    private var soundId = 0
    private var loaded = false

    fun init(context: Context) {
        if (soundPool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        soundId = pool.load(context, R.raw.click, 1)
        soundPool = pool
    }

    fun play() {
        if (!AppSettings.soundEnabled.value || !loaded) return
        soundPool?.play(soundId, 0.6f, 0.6f, 0, 0, 1f)
    }
}

/** Wraps [action] so it plays a short click sound before running. */
fun soundClick(action: () -> Unit): () -> Unit = {
    ClickSound.play()
    action()
}
