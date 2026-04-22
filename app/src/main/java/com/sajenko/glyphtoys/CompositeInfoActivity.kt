package com.sajenko.glyphtoys

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.sajenko.glyphtoys.models.ActiveGlyphSelection
import com.sajenko.glyphtoys.models.DisplayPriority
import com.sajenko.glyphtoys.repository.GlyphImageRepository

class CompositeInfoActivity : Activity() {
    private lateinit var repository: GlyphImageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_composite_info)
        repository = GlyphImageRepository(
            getSharedPreferences(GlyphImageRepository.PreferencesName, Context.MODE_PRIVATE),
        )
        findViewById<View>(R.id.compositeInfoRoot).applySystemBarsPadding()
        findViewById<Button>(R.id.compositeUseButton).setOnClickListener {
            selectCompositeGlyph()
        }
    }

    private fun selectCompositeGlyph() {
        repository.setActiveSelection(
            ActiveGlyphSelection(
                imageId = null,
                mode = DisplayPriority.COMPOSITE,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        Toast.makeText(this, R.string.toast_composite_selected, Toast.LENGTH_SHORT).show()
        returnToMainActivity()
    }

    private fun returnToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    companion object {
        fun intent(context: Context): Intent {
            return Intent(context, CompositeInfoActivity::class.java)
        }
    }
}
