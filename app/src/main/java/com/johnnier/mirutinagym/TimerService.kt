package com.johnnier.mirutinagym

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Servicio en primer plano: mantiene viva la app durante el entrenamiento
 * y dispara la alerta de fin de descanso aunque estés en otra aplicación
 * o con la pantalla apagada.
 */
class TimerService : Service() {
    companion object {
        const val CH_ONGOING = "entreno"
        const val CH_ALERT = "alertas"
        const val ID_ONGOING = 1
        const val ID_ALERT = 2
        @Volatile var running = false

        fun send(ctx: Context, action: String, fill: Intent.() -> Unit = {}) {
            val i = Intent(ctx, TimerService::class.java).setAction(action).apply(fill)
            try { ContextCompat.startForegroundService(ctx, i) } catch (_: Exception) {}
        }

        fun channels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_ONGOING, "Entrenamiento en curso", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false); setSound(null, null)
            })
            nm.createNotificationChannel(NotificationChannel(CH_ALERT, "Fin de descanso", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null) // el sonido fuerte lo reproduce Sounds por el canal de alarma
                enableVibration(true); vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            })
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private var baseTitle = "Mi Rutina Gym"
    private var baseText = "Entrenamiento en curso"
    private var workoutStart = 0L
    private var timerEnd = 0L
    private var timerTitle = ""
    private var timerBody = ""
    private var timerKind = "rest"
    private var sound = true
    private var loud = true
    private var vib = true
    private var started = false

    private val fire = Runnable { onTimerEnd() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        channels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Siempre pasar a primer plano primero (requisito de Android)
        goForeground()
        when (intent?.action) {
            "WORKOUT" -> {
                baseTitle = intent.getStringExtra("title") ?: baseTitle
                baseText = intent.getStringExtra("text") ?: baseText
                workoutStart = intent.getLongExtra("start", workoutStart)
                updateOngoing()
            }
            "TIMER" -> {
                timerEnd = intent.getLongExtra("end", 0L)
                timerTitle = intent.getStringExtra("title") ?: "¡Descanso terminado!"
                timerBody = intent.getStringExtra("body") ?: ""
                timerKind = intent.getStringExtra("kind") ?: "rest"
                sound = intent.getBooleanExtra("sound", true)
                loud = intent.getBooleanExtra("loud", true)
                vib = intent.getBooleanExtra("vib", true)
                handler.removeCallbacks(fire)
                AlarmReceiver.schedule(this, timerEnd, timerTitle, timerBody, timerKind, sound, loud, vib)
                // refresca la notificación fija cuando termina la cuenta regresiva
                handler.postDelayed(fire, maxOf(0L, timerEnd - System.currentTimeMillis()) + 500)
                updateOngoing()
            }
            "CANCEL" -> { cancelTimer(); updateOngoing() }
            "FIRED" -> { handler.removeCallbacks(fire); timerEnd = 0L; updateOngoing() }
            "STOP" -> {
                handler.removeCallbacksAndMessages(null)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                started = false
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelTimer() {
        handler.removeCallbacks(fire)
        AlarmReceiver.cancel(this)
        timerEnd = 0L
        releaseWake()
    }

    private fun onTimerEnd() {
        timerEnd = 0L
        updateOngoing()
    }

    private fun openApp(): PendingIntent {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildOngoing(): Notification {
        val b = NotificationCompat.Builder(this, CH_ONGOING)
            .setSmallIcon(R.drawable.ic_stat)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        if (timerEnd > System.currentTimeMillis()) {
            b.setContentTitle("⏱ Descansando…")
                .setContentText(timerBody)
                .setWhen(timerEnd).setShowWhen(true)
                .setUsesChronometer(true).setChronometerCountDown(true)
        } else {
            b.setContentTitle(baseTitle).setContentText(baseText)
            if (workoutStart > 0) b.setWhen(workoutStart).setShowWhen(true).setUsesChronometer(true)
        }
        return b.build()
    }

    private fun goForeground() {
        val n = buildOngoing()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(ID_ONGOING, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID_ONGOING, n)
        started = true
    }

    private fun updateOngoing() {
        if (!started) return
        try { getSystemService(NotificationManager::class.java).notify(ID_ONGOING, buildOngoing()) } catch (_: Exception) {}
    }

    private fun acquireWake(ms: Long) {
        releaseWake()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiRutinaGym:timer").apply { acquire(ms) }
    }

    private fun releaseWake() {
        wake?.let { if (it.isHeld) it.release() }
        wake = null
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        releaseWake()
        super.onDestroy()
    }
}
