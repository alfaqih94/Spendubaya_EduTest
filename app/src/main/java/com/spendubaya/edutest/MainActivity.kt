package com.spendubaya.edutest

import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.InputType // Import for InputType

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var attentionMediaPlayer: MediaPlayer? = null
    private var exitMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private lateinit var batteryStatus: TextView
    private lateinit var timeStatus: TextView
    private lateinit var statusContainer: LinearLayout // Container for battery and time
    private lateinit var exitButton: ImageButton // Exit button instance

    // STATIC WEB URL
    private val staticWebURL = "https://sites.google.com/view/spendubaya-edutest"

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Device Policy Manager for Kiosk Mode functionalities
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        // Set FLAG_SECURE to prevent screenshots and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        // Keep the screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Set the main activity layout (ensure this contains a FrameLayout or similar root)
        setContentView(R.layout.main_activity)

        // Initialize WebView and its settings
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true // Enable JavaScript for web content
        webView.webViewClient = WebViewClient() // Handle all URLs inside this WebView

        // Get the root view of the activity to add dynamic views on top
        val rootView = findViewById<FrameLayout>(android.R.id.content)

        // Create the top bar layout that will hold battery/time and the exit button
        val topBarLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL // Arrange children horizontally
            setBackgroundColor(Color.TRANSPARENT) // No background for the bar
            // Padding for the entire top bar (increased top padding to move it down)
            setPadding(dpToPx(16f), dpToPx(24f), dpToPx(16f), dpToPx(8f))
            gravity = Gravity.CENTER_VERTICAL // Vertically center content within this bar
        }

        // Add the topBarLayout to the root view, positioning it at the very top
        rootView.addView(topBarLayout, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, // Match parent width
            FrameLayout.LayoutParams.WRAP_CONTENT // Wrap content height
        ).apply {
            gravity = Gravity.TOP // Position at the top
        })

        // --- Start of Layout Changes for Centering ---

        // Left spacer to push content to the center
        val leftSpacer = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // 0 width
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1.0f // This will take up available space equally with rightSpacer
            }
        }
        topBarLayout.addView(leftSpacer)

        // Initialize statusContainer (for battery and time), it will be added to topBarLayout
        statusContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            // No weight here, it will wrap its content
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        topBarLayout.addView(statusContainer) // Add statusContainer to topBarLayout

        // Right spacer to push content to the center and the exit button to the right
        val rightSpacer = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // 0 width
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1.0f // This will take up available space equally with leftSpacer
            }
        }
        topBarLayout.addView(rightSpacer)

        // Initialize and add the exit button
        exitButton = ImageButton(this).apply {
            setImageResource(R.drawable.ikon_keluar) // Set the exit icon
            // Tint the icon red
            setColorFilter(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.SRC_IN)
            setBackgroundColor(Color.TRANSPARENT) // Transparent background for the button
            setOnClickListener { showExitDialog() } // Set click listener to show exit dialog
            scaleType = ImageView.ScaleType.FIT_CENTER // Scale icon to fit button boundaries

            val iconSize = dpToPx(48f) // Increased size for the top bar icon

            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                // No specific gravity needed here as the spacers handle the positioning
            }
        }
        topBarLayout.addView(exitButton) // Add exitButton to topBarLayout

        // --- End of Layout Changes for Centering ---

        // Call function to add battery and time display into statusContainer
        addBatteryAndTimeDisplay()

        // Initialize AudioManager for volume control
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Attempt to load media players for sounds
        try {
            exitMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
            attentionMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Failed to load initial sounds: ${e.message}")
        }

        // Set up the OnBackPressedCallback for modern back navigation handling
        // This callback effectively disables the back button by doing nothing.
        val onBackPressedCallback = object : OnBackPressedCallback(true) { // 'true' means enabled
            override fun handleOnBackPressed() {
                // Do nothing to disable the back button
                playAttentionSound() // Play an attention sound when back is pressed
            }
        }
        // Add the callback to the activity's onBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Check and activate Kiosk Mode
        checkKioskModeStatus()
    }

    override fun onResume() {
        super.onResume()
        // Start Lock Task mode (Kiosk Mode)
        startLockTask()
        playAttentionSound() // Play sound on resume
        checkKioskModeStatus() // Re-check kiosk mode status
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Intercept key events to control Kiosk Mode behavior
        if (event.action == KeyEvent.ACTION_DOWN) {
            playAttentionSound() // Play sound on any key press

            when (event.keyCode) {
                // Block these system keys to prevent exiting Kiosk Mode
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_HOME,
                KeyEvent.KEYCODE_APP_SWITCH -> {
                    return true // Consume the event, preventing default behavior
                }
                else -> {
                    // Allow default behavior for other keys
                }
            }
        }
        return super.dispatchKeyEvent(event) // Pass the event to the superclass for normal handling
    }

    // Generates a dynamic exit code based on the current date
    private fun getExitCode(): String {
        val dateFormat = SimpleDateFormat("ddMMyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    // Shows a dialog for exiting Kiosk Mode, requiring a code
    private fun showExitDialog() {
        val input = EditText(this).apply {
            hint = "Masukkan Kode"
            setTextColor(Color.RED)
            textSize = 40f
            setHintTextColor(Color.DKGRAY)
            // Use TYPE_CLASS_NUMBER and TYPE_NUMBER_VARIATION_PASSWORD to obscure input
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTypeface(typeface, android.graphics.Typeface.BOLD) // Bold text
            setPadding(50, 40, 50, 40) // Internal padding
            setBackgroundResource(android.R.drawable.editbox_background) // Standard edit box background
            gravity = Gravity.CENTER_HORIZONTAL // Center the text within the EditText
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // Adjusted padding to be more symmetric and provide enough space for buttons
            setPadding(dpToPx(24f), dpToPx(24f), dpToPx(24f), dpToPx(24f))
            setBackgroundColor(Color.WHITE) // White background for the dialog content
            gravity = Gravity.CENTER_HORIZONTAL // Center the EditText horizontally within this layout
            addView(input) // Add the EditText to the layout
        }

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Keluar")
            .setMessage("Masukkan kode unik untuk keluar")
            .setView(layout) // Set the custom view for the dialog
            .setPositiveButton("KELUAR") { _, _ ->
                if (input.text.toString() == getExitCode()) {
                    playAttentionSound() // Play sound on successful exit attempt
                    stopLockTask() // Exit Lock Task mode
                    finishAffinity() // Finish all activities in the task
                } else {
                    Toast.makeText(this, "Kode salah!", Toast.LENGTH_SHORT).show() // Show error toast
                }
            }
            // Add a negative button to dismiss the dialog
            .setNegativeButton("BATAL") { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(R.drawable.ikon_keluar) // Set dialog icon
            .show() // Display the dialog
    }

    // Plays an attention sound, ensuring max volume
    private fun playAttentionSound() {
        try {
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0) // Set max volume

                if (attentionMediaPlayer != null) {
                    if (attentionMediaPlayer!!.isPlaying) {
                        attentionMediaPlayer?.seekTo(0) // Restart if already playing
                    } else {
                        attentionMediaPlayer?.start() // Start playing
                    }
                } else {
                    // Re-create if null (e.g., after release in onDestroy)
                    attentionMediaPlayer = MediaPlayer.create(this, R.raw.alarm)
                    attentionMediaPlayer?.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("MainActivity", "Error playing attention sound: ${e.message}")
        }
    }

    // Loads the static URL into the WebView
    private fun loadStaticUrl() {
        webView.loadUrl(staticWebURL)
        Log.i("MainActivity", "Loading URL: $staticWebURL")
    }

    // Checks and enables Kiosk Mode if conditions are met
    private fun checkKioskModeStatus() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)) {
            startLockTask() // Start Lock Task mode
            Log.i("MainActivity", "Kiosk Mode is active!")
            loadStaticUrl() // Load URL once Kiosk Mode is confirmed/started
        } else {
            // If not device/profile owner, Kiosk Mode might not activate automatically
            // Still attempt to startLockTask but log a warning.
            startLockTask()
            Log.w("MainActivity", "Kiosk Mode might not be active automatically. Enable manually if needed.")
            loadStaticUrl()
        }
    }

    // Handles the result of enabling device admin
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Log.i("MainActivity", "Device admin enabled. Please try opening the app again.")
                finish() // Finish to restart the app and apply changes
            } else {
                Log.w("MainActivity", "Device admin not enabled. Kiosk Mode cannot be activated.")
            }
        }
    }

    // Releases media players when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        attentionMediaPlayer?.release()
        attentionMediaPlayer = null
        exitMediaPlayer?.release()
        exitMediaPlayer = null
    }

    // Utility function to convert DP to Pixels
    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val REQUEST_CODE_ENABLE_ADMIN = 1
    }

    // Adds battery status and time display to the statusContainer
    private fun addBatteryAndTimeDisplay() {
        // Layout for battery icon and text
        val batteryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL // Vertically align items within this layout
            setPadding(0, 0, dpToPx(8f), 0) // Padding to the right for spacing from time
        }

        val batteryIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_baterai)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply { // Icon size
                setMargins(0, 0, dpToPx(4f), 0) // Margin right for icon
            }
        }
        batteryStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f // Text size for battery status
        }

        batteryLayout.addView(batteryIcon)
        batteryLayout.addView(batteryStatus)

        // Layout for time icon and text, with vertical adjustment
        val timeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL // Vertically align items within this layout
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // Add a small top margin to push the time down slightly as requested
                setMargins(0, dpToPx(2f), 0, 0)
            }
        }

        val timeIcon = ImageView(this).apply {
            setImageResource(R.drawable.ikon_jam)
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply { // Icon size
                setMargins(0, 0, dpToPx(4f), 0) // Margin right for icon
            }
        }
        timeStatus = TextView(this).apply {
            setTextColor(Color.DKGRAY)
            textSize = 16f // Text size for time status
        }

        timeLayout.addView(timeIcon)
        timeLayout.addView(timeStatus)

        // Add battery and time layouts to the main statusContainer
        statusContainer.addView(batteryLayout)
        statusContainer.addView(timeLayout)

        updateBatteryAndTime() // Initial update of battery and time
    }

    // Updates battery percentage and current time periodically
    private fun updateBatteryAndTime() {
        // Get battery level
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        batteryStatus.text = "$level%"

        // Get current time
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeStatus.text = timeFormat.format(Date())

        // Schedule next update after 1 minute (60000 ms)
        Handler(Looper.getMainLooper()).postDelayed({ updateBatteryAndTime() }, 60000)
    }
}