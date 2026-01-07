package com.spendubaya.edutest

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // --- Services / Managers Logic ---
    private lateinit var soundManager: SoundManager
    private lateinit var examTimer: ExamTimer
    private lateinit var systemMonitor: SystemMonitor

    // --- UI Managers ---
    private lateinit var topBarManager: TopBarManager
    private lateinit var statusViewManager: StatusViewManager
    private lateinit var exitDialogManager: ExitDialogManager
    private lateinit var navigationManager: WebViewNavigationManager

    // --- UI Components ---
    private lateinit var webView: WebView
    private lateinit var errorLayout: LinearLayout
    private lateinit var exitErrorButton: Button

    // --- Data Variables ---
    private var dynamicExamURL: String? = null
    private var dynamicExitToken: String? = null
    private var examDurationMinutes: Int = 0

    // --- Kiosk Mode Components ---
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Setup Awal (Layar, Security)
        setupWindowSettings()
        setupDevicePolicy()

        // 2. Setup UI Dasar (XML Content)
        setContentView(R.layout.main_activity)
        initializeBaseViews()

        // 3. Setup UI Dinamis (PERBAIKAN LAYOUT DI SINI)
        setupDynamicLayoutStructure()

        // 4. Inisialisasi Logic Services
        soundManager = SoundManager(this)
        exitDialogManager = ExitDialogManager(this)

        // 5. Proses Data Intent
        processIntentData()
        setupNavigationSecurity()

        // 6. Mulai Logika Timer & Ujian
        startExamLogic()
        checkKioskModeStatus()
    }

    override fun onResume() {
        super.onResume()
        startLockTask()
        soundManager.playAttentionSound()
        checkKioskModeStatus()
        systemMonitor.startMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
        examTimer.cancel()
        systemMonitor.stopMonitoring()
    }

    // --- 1. Setup Functions ---

    private fun setupWindowSettings() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun setupDevicePolicy() {
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)
    }

    private fun initializeBaseViews() {
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val rootView = findViewById<FrameLayout>(android.R.id.content)
        errorLayout = layoutInflater.inflate(R.layout.error_urlload, rootView, false) as LinearLayout
        rootView.addView(errorLayout)
        errorLayout.visibility = View.GONE

        exitErrorButton = errorLayout.findViewById(R.id.exit_error_button)
        exitErrorButton.setOnClickListener {
            stopLockTask()
            finishAffinity()
        }
    }

    // --- 2. Dynamic UI Setup (RESTRUCTURED) ---

    private fun setupDynamicLayoutStructure() {
        // --- LOGIKA BARU: HEAD (Status) - BODY (Web) - FOOT (Nav) ---

        val originalParent = webView.parent as ViewGroup
        val index = originalParent.indexOfChild(webView)
        val originalParams = webView.layoutParams
        originalParent.removeView(webView)

        // 1. Main Container (Vertikal)
        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = originalParams
        }

        // 2. Header Container (Atas: Status)
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 3. Footer Container (Bawah: Navigasi + Exit)
        val footerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Tambahkan shadow atau garis pemisah di atas footer agar terlihat rapi (opsional)
            elevation = 10f
        }

        // 4. WebView Params (Tengah: Mengisi sisa ruang)
        val webViewParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0
        ).apply {
            weight = 1f
        }
        webView.layoutParams = webViewParams

        // 5. Susun Layout
        mainContainer.addView(headerContainer) // Atas
        mainContainer.addView(webView)         // Tengah
        mainContainer.addView(footerContainer) // Bawah

        originalParent.addView(mainContainer, index)


        // --- ISI KONTEN ---

        // Init Managers
        topBarManager = TopBarManager(this)
        statusViewManager = StatusViewManager(this)
        navigationManager = WebViewNavigationManager(this, webView)

        // A. Setup Top Bar (Masuk Header)
        val statusContainer = topBarManager.setupTopBar(headerContainer)

        // B. Setup Navigation Bar (Masuk Footer)
        val navView = navigationManager.createNavigationBar(
            onExitClick = { showExitConfirmation() }
        )
        footerContainer.addView(navView)


        // --- Logic Status ---
        val statusComponents = statusViewManager.createStatusViews(statusContainer)
        systemMonitor = SystemMonitor(this, statusComponents.batteryText, statusComponents.timeText)
        examTimer = ExamTimer(
            timerTextView = statusComponents.timerText,
            onWarning = { soundManager.playAttentionSound() },
            onFinishExam = { handleExamFinished() }
        )
    }

    // --- 3. Logic & Navigation Functions ---

    private fun showExitConfirmation() {
        exitDialogManager.show(
            expectedToken = dynamicExitToken,
            onConfirm = {
                soundManager.playAttentionSound()
                stopLockTask()
                finishAffinity()
            },
            onDeny = {
                soundManager.playAttentionSound()
            }
        )
    }

    private fun processIntentData() {
        dynamicExamURL = intent.getStringExtra("EXAM_URL")
        dynamicExitToken = intent.getStringExtra("EXIT_TOKEN")
        examDurationMinutes = intent.getIntExtra("EXAM_DURATION_MINUTES", 0)
        Log.i("MainActivity", "URL: $dynamicExamURL, Token: $dynamicExitToken")
    }

    private fun startExamLogic() {
        if (examDurationMinutes > 0) {
            examTimer.start(examDurationMinutes)
        }
    }

    private fun handleExamFinished() {
        Toast.makeText(this, "Waktu ujian habis! Aplikasi keluar otomatis.", Toast.LENGTH_LONG).show()
        soundManager.playAttentionSound()
        Handler(Looper.getMainLooper()).postDelayed({
            stopLockTask()
            finishAffinity()
        }, 1500)
    }

    private fun loadWebViewUrl() {
        if (!dynamicExamURL.isNullOrEmpty()) {
            webView.visibility = View.VISIBLE
            errorLayout.visibility = View.GONE
            webView.loadUrl(dynamicExamURL!!)
        } else {
            webView.visibility = View.GONE
            errorLayout.visibility = View.VISIBLE
            webView.loadUrl("about:blank")
        }
    }

    private fun checkKioskModeStatus() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
            startLockTask()
            loadWebViewUrl()
        } else {
            startLockTask()
            loadWebViewUrl()
        }
    }

    private fun setupNavigationSecurity() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                soundManager.playAttentionSound()
            }
        })
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode != KeyEvent.KEYCODE_BACK) {
                soundManager.playAttentionSound()
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH -> return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}