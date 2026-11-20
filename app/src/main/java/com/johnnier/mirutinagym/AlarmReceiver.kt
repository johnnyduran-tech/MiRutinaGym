package com.johnnier.mirutinagym

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Recibe la alarma del sistema al terminar el descanso. AlarmManager la entrega
 * aunque la app esté en segundo plano, congelada por el ahorro de batería o con
 * la pantalla apagada (el Handler del servicio no siempre corre en esos casos).
 */
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val REQ = 4242

        private fun pending(ctx: Context, i: Intent?, flags: Int): PendingIntent? =
            PendingIntent.getBroadcast(ctx, REQ, i ?: Intent(ctx, AlarmReceiver::class.java), flags or PendingIntent.FLAG_IMMUTABLE)

        fun schedule(ctx: Context, endMs: Long, title: String, body: String, kind: String, sound: Boolean, loud: Boolean, vib: Boolean) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(ctx, AlarmReceiver::class.java).apply {
                putExtra("title", title); putExtra("body", body); putExtra("kind", kind)
                putExtra("sound", sound); putExtra("loud", loud); putExtra("vib", vib); putExtra("end", endMs)
            }
            val pi = pending(ctx, i, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
            val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            try {
                if (canExact) {
                    // setAlarmClock: máxima prioridad, no lo retrasa el modo Doze ni el ahorro de batería
                    val show = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(endMs, show), pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMs, pi)
                }
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMs, pi)
            }
        }

        fun cancel(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            pending(ctx, null, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() }
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        // Ignorar alarmas viejas (más de 2 minutos de retraso)
        val end = intent.getLongExtra("end", 0L)
        if (end > 0 && System.currentTimeMillis() - end > 120_000) return

        val pending = goAsync()
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiRutinaGym:alarm").apply { acquire(9_000) }

        val kind = intent.getStringExtra("kind") ?: "rest"
        if (intent.getBooleanExtra("sound", true)) Sounds.play(ctx, kind, intent.getBooleanExtra("loud", true))
        if (intent.getBooleanExtra("vib", true)) Sounds.vibrate(ctx, kind)

        TimerService.channels(ctx)
        val open = PendingIntent.getActivity(ctx, 1,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, TimerService.CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(intent.getStringExtra("title") ?: "¡Descanso terminado!")
            .setContentText(intent.getStringExtra("body") ?: "")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(open)
            .build()
        try { ctx.getSystemService(NotificationManager::class.java).notify(TimerService.ID_ALERT, n) } catch (_: Exception) {}

        // Avisar al servicio para que actualice su notificación fija (si sigue vivo)
        if (TimerService.running) {
            try { ctx.startService(Intent(ctx, TimerService::class.java).setAction("FIRED")) } catch (_: Exception) {}
        }

        // Mantener vivo el receptor mientras suena (máx. ~8 s)
        Handler(Looper.getMainLooper()).postDelayed({
            if (wl.isHeld) wl.release()
            pending.finish()
        }, 8_000)
    }
}
