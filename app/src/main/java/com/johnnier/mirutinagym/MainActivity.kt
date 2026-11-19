package com.johnnier.mirutinagym

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView

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

        @JavascriptInterface
        fun playSound(kind: String, loud: Boolean) = runOnUiThread { Sounds.play(this@MainActivity, kind, loud) }

        @JavascriptInterface
        fun startTimer(endMs: String, title: String, body: String, kind: String, sound: Boolean, loud: Boolean, vib: Boolean) =
            runOnUiThread {
                TimerService.send(this@MainActivity, "TIMER") {
                    putExtra("end", endMs.toLongOrNull() ?: 0L)
                    putExtra("title", title); putExtra("body", body); putExtra("kind", kind)
                    putExtra("sound", sound); putExtra("loud", loud); putExtra("vib", vib)
                }
            }

        @JavascriptInterface
        fun cancelTimer() = runOnUiThread { if (TimerService.running) TimerService.send(this@MainActivity, "CANCEL") }

        @JavascriptInterface
        fun workout(title: String, text: String, startMs: String) = runOnUiThread {
            TimerService.send(this@MainActivity, "WORKOUT") {
                putExtra("title", title); putExtra("text", text); putExtra("start", startMs.toLongOrNull() ?: 0L)
            }
        }

        @JavascriptInterface
        fun stopWorkout() = runOnUiThread {
            // Solo detener si el servicio está corriendo (evita arrancarlo para nada)
            stopService(Intent(this@MainActivity, TimerService::class.java))
        }

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
