package com.sajenko.glyphtoys

import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var label: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        label = TextView(this).apply {
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
        setContentView(label)
        updateLabel()
        ensureAudioPermission()
    }

    private fun ensureAudioPermission() {
        if (hasAudioPermission()) {
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO) {
            updateLabel()
        }
    }

    private fun updateLabel() {
        val permissionStatus = if (hasAudioPermission()) {
            "Audio permission granted. Equalizer mode can react to playback."
        } else {
            "Audio permission required for reactive equalizer mode."
        }
        label.text = "Glyph Toys\n\nActivate from Settings -> Glyph -> Glyph Toys\n\n$permissionStatus"
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val REQUEST_AUDIO = 1001
    }
}
