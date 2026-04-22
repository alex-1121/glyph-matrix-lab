package com.sajenko.glyphtoys.toys

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Base class for all Glyph Toy services.
 *
 * Subclasses must be registered in AndroidManifest.xml with:
 *   <intent-filter> <action android:name="com.nothing.glyph.TOY" /> </intent-filter>
 *
 * Override [onServiceConnected] to start rendering, and [onServiceDisconnected] to clean up.
 * Override the touch callbacks if the toy reacts to Glyph Button presses.
 *
 * Target: Nothing Phone (4a) Pro — DEVICE_25111p — 13×13 matrix.
 */
abstract class GlyphToyBase(private val tag: String) : Service() {

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            val gmm = glyphMatrixManager ?: return
            Log.d(TAG, "$tag: onServiceConnected")
            // Register for the target device. Change to DEVICE_23112 for Phone (3).
            gmm.register(Glyph.DEVICE_25111p)
            onServiceConnected(applicationContext, gmm)
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    final override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "$tag: onBind")
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "$tag: onUnbind")
        glyphMatrixManager?.let { onServiceDisconnected(applicationContext) }
        glyphMatrixManager?.turnOff()
        glyphMatrixManager?.unInit()
        glyphMatrixManager = null
        return false
    }

    // ── Overridable callbacks ──────────────────────────────────────────────────

    /** Called when the GlyphMatrix service is connected and ready to receive frames. */
    open fun onServiceConnected(context: Context, gmm: GlyphMatrixManager) {}

    /** Called just before the GlyphMatrix service is disconnected. Clean up timers here. */
    open fun onServiceDisconnected(context: Context) {}

    /** Glyph Button touch-down (requires `com.nothing.glyph.toy.longpress = 1` in manifest). */
    open fun onTouchDown() {}

    /** Glyph Button touch-up. */
    open fun onTouchUp() {}

    /** Fires every minute when the toy is active as an AOD toy. */
    open fun onAod() {}

    /** Glyph Button long-press ("change" event). */
    open fun onLongPress() {}

    // ── Input handling ─────────────────────────────────────────────────────────

    private val inputHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.getString(KEY_DATA)?.let { event ->
                        when (event) {
                            GlyphToy.EVENT_AOD         -> onAod()
                            GlyphToy.EVENT_ACTION_DOWN -> onTouchDown()
                            GlyphToy.EVENT_ACTION_UP   -> onTouchUp()
                            GlyphToy.EVENT_CHANGE      -> onLongPress()
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(inputHandler)

    private companion object {
        private val TAG = GlyphToyBase::class.java.simpleName
        private const val KEY_DATA = "data"
    }
}
