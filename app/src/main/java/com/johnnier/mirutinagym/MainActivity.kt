package com.johnnier.mirutinagym

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.res.Configuration
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var fileCb: ValueCallback<Array<Uri>>? = null
    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileCb?.onReceiveValue(if (uri != null) arrayOf(uri) else null); fileCb = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = 0xFF0B0E14.toInt()
        window.navigationBarColor = 0xFF0F131B.toInt()
        TimerService.channels(this)

        // Permiso de notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        setContentView(web)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
        }
        web.setBackgroundColor(0xFF0B0E14.toInt())
        web.addJavascriptInterface(Bridge(), "AndroidBridge")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                loader.shouldInterceptRequest(request.url)
        }
        // Permite el botón "Seleccionar archivo" (importar respaldo .json)
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCb?.onReceiveValue(null); fileCb = cb
                return try { pickFile.launch("*/*"); true } catch (_: Exception) { fileCb = null; false }
            }
        }
        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.handleBack ? window.handleBack() : false") { r ->
                    // Durante un entrenamiento, "atrás" minimiza en vez de cerrar
                    if (r != "true") moveTaskToBack(true)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // al volver a la app, quitar la alerta y refrescar el temporizador
        getSystemService(NotificationManager::class.java).cancel(TimerService.ID_ALERT)
        if (::web.isInitialized) web.evaluateJavascript("window.tickTimer && tickTimer()", null)
    }

    private fun openAppSettings() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (_: Exception) {}
    }

    /** Colores de barras del sistema según el tema elegido en la app */
    private fun applyBars(dark: Boolean) {
        val bg = if (dark) 0xFF0B0E14.toInt() else 0xFFF3F4F7.toInt()
        val nav = if (dark) 0xFF0F131B.toInt() else 0xFFFFFFFF.toInt()
        window.statusBarColor = bg
        window.navigationBarColor = nav
        if (::web.isInitialized) web.setBackgroundColor(bg)
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.isAppearanceLightStatusBars = !dark
        c.isAppearanceLightNavigationBars = !dark
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    inner class Bridge {
        @JavascriptInterface
        fun share(text: String) = runOnUiThread {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Respaldo Mi Rutina Gym")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(i, "Guardar respaldo"))
        }

        /** Comparte el respaldo como ARCHIVO .json (sin límite de tamaño, a diferencia del texto) */
        @JavascriptInterface
        fun shareFile(name: String, text: String) = runOnUiThread {
            try {
                val dir = File(cacheDir, "respaldos").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
                val f = File(dir, name).apply { writeText(text, Charsets.UTF_8) }
                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", f)
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Respaldo Mi Rutina Gym")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(i, "Guardar respaldo"))
            } catch (_: Exception) { share(text) }
        }

        @JavascriptInterface
        fun playSound(kind: String, loud: Boolean) = runOnUiThread { Sounds.play(this@MainActivity, kind, loud) }

        @JavascriptInterface
        fun vibrate(kind: String) = runOnUiThread { Sounds.vibrate(this@MainActivity, kind) }

        @JavascriptInterface
        fun startTimer(endMs: String, title: String, body: String, kind: String, sound: Boolean, loud: Boolean, vib: Boolean) =
            runOnUiThread {
                // La alarma del sistema se agenda directamente (no depende de que el servicio arranque)
                AlarmReceiver.schedule(this@MainActivity, endMs.toLongOrNull() ?: 0L, title, body, kind, sound, loud, vib)
                TimerService.send(this@MainActivity, "TIMER") {
                    putExtra("end", endMs.toLongOrNull() ?: 0L)
                    putExtra("title", title); putExtra("body", body); putExtra("kind", kind)
                    putExtra("sound", sound); putExtra("loud", loud); putExtra("vib", vib)
                }
            }

        /** Prueba: agenda solo la alarma del sistema (sin servicio) para verificar el sonido en segundo plano */
        @JavascriptInterface
        fun testAlert(seconds: Int, loud: Boolean) = runOnUiThread {
            AlarmReceiver.schedule(this@MainActivity, System.currentTimeMillis() + seconds * 1000L,
                "🔔 Prueba de alerta", "Si escuchaste esto, las alertas funcionan en segundo plano", "rest", true, loud, true)
        }

        @JavascriptInterface
        fun cancelTimer() = runOnUiThread {
            AlarmReceiver.cancel(this@MainActivity)
            if (TimerService.running) TimerService.send(this@MainActivity, "CANCEL")
        }

        @JavascriptInterface
        fun workout(title: String, text: String, startMs: String) = runOnUiThread {
            TimerService.send(this@MainActivity, "WORKOUT") {
                putExtra("title", title); putExtra("text", text); putExtra("start", startMs.toLongOrNull() ?: 0L)
            }
        }

        @JavascriptInterface
        fun stopWorkout() = runOnUiThread {
            if (TimerService.running) stopService(Intent(this@MainActivity, TimerService::class.java))
        }

        @JavascriptInterface
        fun setTheme(dark: Boolean) = runOnUiThread { applyBars(dark) }

        @JavascriptInterface
        fun systemDark(): Boolean =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        @JavascriptInterface
        fun permissionsStatus(): String {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            val notif = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            return "{\"exact\":$exact,\"notif\":$notif,\"battery\":${pm.isIgnoringBatteryOptimizations(packageName)}}"
        }

        @JavascriptInterface
        fun openExactAlarmSettings() = runOnUiThread {
            try {
                if (Build.VERSION.SDK_INT >= 31) startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            } catch (_: Exception) { openAppSettings() }
        }

        @JavascriptInterface
        fun openNotificationSettings() = runOnUiThread {
            try {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            } catch (_: Exception) { openAppSettings() }
        }

        @JavascriptInterface
        fun openAppDetails() = runOnUiThread { openAppSettings() }

        @SuppressLint("BatteryLife")
        @JavascriptInterface
        fun openBatterySettings() = runOnUiThread {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            val i = if (!pm.isIgnoringBatteryOptimizations(packageName))
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            try { startActivity(i) } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}
