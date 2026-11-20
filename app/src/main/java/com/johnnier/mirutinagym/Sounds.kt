package com.johnnier.mirutinagym

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Reproduce las alertas por el canal de ALARMA (suena fuerte y aun en modo silencio). */
object Sounds {
    private var player: MediaPlayer? = null
    private var savedVolume = -1

    private fun resFor(kind: String) = when (kind) {
        "set" -> R.raw.serie_ok
        "hold" -> R.raw.iso_fin
        else -> R.raw.descanso_fin
    }

    fun play(ctx: Context, kind: String, loud: Boolean) {
        val app = ctx.applicationContext
        val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        stop(app)
        if (loud) {
            try {
                savedVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            } catch (_: Exception) { savedVolume = -1 }
        }
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(app, Uri.parse("android.resource://${app.packageName}/${resFor(kind)}"))
            mp.setOnCompletionListener { stop(app) }
            mp.prepare()
            mp.start()
            player = mp
        } catch (e: Exception) {
            restoreVolume(app)
        }
    }

    fun stop(ctx: Context) {
        player?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        player = null
        restoreVolume(ctx.applicationContext)
    }

    private fun restoreVolume(ctx: Context) {
        if (savedVolume >= 0) {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            try { am.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0) } catch (_: Exception) {}
            savedVolume = -1
        }
    }

    /**
     * Vibración con uso "ALARMA": Android ignora las vibraciones de apps en segundo plano
     * si no declaran este uso (por eso antes no vibraba al terminar el descanso).
     */
    @Suppress("DEPRECATION")
    fun vibrate(ctx: Context, kind: String) {
        val timings = if (kind == "set") longArrayOf(0, 180, 100, 180)
                      else longArrayOf(0, 600, 250, 600, 250, 900)
        val amps = IntArray(timings.size) { i -> if (i % 2 == 1) 255 else 0 }
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amps, -1)
                     else VibrationEffect.createWaveform(timings, -1)
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                v.vibrate(effect, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
        } catch (_: Exception) {
            try { v.vibrate(effect) } catch (_: Exception) {}
        }
    }

}
