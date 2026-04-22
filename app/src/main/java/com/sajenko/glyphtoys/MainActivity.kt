package com.sajenko.glyphtoys

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sajenko.glyphtoys.models.ActiveGlyphSelection
import com.sajenko.glyphtoys.models.CustomGlyphImage
import com.sajenko.glyphtoys.models.DisplayPriority
import com.sajenko.glyphtoys.repository.GlyphImageRepository
import com.sajenko.glyphtoys.serialization.GlyphImageSerializer
import com.sajenko.glyphtoys.toys.AodToySelectionAdvisor
import com.sajenko.glyphtoys.toys.FrameBuilders
import com.sajenko.glyphtoys.toys.LiveGlyphFrame
import com.sajenko.glyphtoys.toys.LiveGlyphMode
import com.sajenko.glyphtoys.toys.LiveGlyphPreview
import com.sajenko.glyphtoys.toys.LiveGlyphSource
import com.sajenko.glyphtoys.toys.PixelGrid
import com.sajenko.glyphtoys.views.GlyphMatrixView
import java.util.Calendar

class MainActivity : Activity(), LiveGlyphPreview.Listener {
    private lateinit var repository: GlyphImageRepository
    private lateinit var permissionStatus: TextView
    private lateinit var permissionSettingsButton: Button
    private lateinit var configuredLabel: TextView
    private lateinit var configuredMatrixView: GlyphMatrixView
    private lateinit var aodWarningContainer: LinearLayout
    private lateinit var aodWarningMessage: TextView
    private lateinit var aodWarningButton: Button
    private lateinit var glyphListContainer: LinearLayout
    private var liveFrame: LiveGlyphFrame? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repository = GlyphImageRepository(
            getSharedPreferences(GlyphImageRepository.PreferencesName, Context.MODE_PRIVATE),
        )
        ShowcaseGlyphImages.seedIfNeeded(this, repository)
        bindViews()
        permissionSettingsButton.setOnClickListener { openAppSettings() }
        aodWarningButton.setOnClickListener { openAodToyPicker() }
        ensureAudioPermission()
    }

    override fun onStart() {
        super.onStart()
        LiveGlyphPreview.addListener(this)
    }

    override fun onResume() {
        super.onResume()
        refreshHome()
    }

    override fun onStop() {
        LiveGlyphPreview.removeListener(this)
        super.onStop()
    }

    override fun onLiveGlyphFrame(frame: LiveGlyphFrame?) {
        liveFrame = frame
        updateConfiguredPreview()
    }

    private fun bindViews() {
        findViewById<View>(R.id.mainRoot).applySystemBarsPadding()
        permissionStatus = findViewById(R.id.permissionStatus)
        permissionSettingsButton = findViewById(R.id.permissionSettingsButton)
        configuredLabel = findViewById(R.id.configuredLabel)
        configuredMatrixView = findViewById(R.id.configuredMatrixView)
        aodWarningContainer = findViewById(R.id.aodWarningContainer)
        aodWarningMessage = findViewById(R.id.aodWarningMessage)
        aodWarningButton = findViewById(R.id.aodWarningButton)
        glyphListContainer = findViewById(R.id.glyphListContainer)
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
            refreshHome()
        }
    }

    private fun refreshHome() {
        updatePermissionStatus()
        updateConfiguredPreview()
        rebuildGlyphList()
    }

    private fun updatePermissionStatus() {
        val hasPermission = hasAudioPermission()
        permissionStatus.visibility = if (hasPermission) View.GONE else View.VISIBLE
        if (!hasPermission) {
            permissionStatus.text = getString(R.string.permission_status_denied)
        }
        permissionSettingsButton.visibility = if (hasPermission) View.GONE else View.VISIBLE
    }

    private fun updateConfiguredPreview() {
        val frame = liveFrame ?: LiveGlyphPreview.latestFrame()
        val selection = repository.getActiveSelection()
        if (frame != null) {
            configuredMatrixView.maskedGrid = null
            configuredMatrixView.pixelGrid = frame.grid
            configuredMatrixView.interactiveMode = false
            configuredLabel.text = "${getString(R.string.live_display_label)} ${liveModeLabel(frame.mode)}"
            updateAodWarning(selection, frame)
            return
        }

        configuredMatrixView.maskedGrid = null
        configuredMatrixView.interactiveMode = false

        when (selection?.mode) {
            DisplayPriority.COMPOSITE -> {
                configuredMatrixView.pixelGrid = currentClockGrid()
                configuredLabel.text =
                    "${getString(R.string.configured_display_label)} ${getString(R.string.configured_composite_clock)}"
            }
            DisplayPriority.IDLE_ONLY,
            DisplayPriority.ALWAYS_ON -> {
                val image = selection.imageId?.let(repository::getImage)
                val grid = image?.pixels?.let(GlyphImageSerializer::binaryToPixelGrid)

                configuredMatrixView.pixelGrid = grid ?: PixelGrid()
                configuredLabel.text = if (image != null) {
                    "${getString(R.string.configured_display_label)} ${image.name} - ${modeLabel(selection.mode)}"
                } else {
                    "${getString(R.string.configured_display_label)} ${getString(R.string.no_selection)}"
                }
            }
            null -> {
                configuredMatrixView.pixelGrid = PixelGrid()
                configuredLabel.text =
                    "${getString(R.string.configured_display_label)} ${getString(R.string.no_selection)}"
            }
        }
        updateAodWarning(selection, null)
    }

    private fun updateAodWarning(selection: ActiveGlyphSelection?, frame: LiveGlyphFrame?) {
        val guidance = selection?.let {
            AodToySelectionAdvisor.guidanceFor(it.mode, frame?.source)
        }

        if (selection == null || guidance == null) {
            aodWarningContainer.visibility = View.GONE
            return
        }

        aodWarningMessage.text = getString(
            R.string.aod_warning_message,
            toySourceLabel(guidance.expectedSource),
        )
        aodWarningContainer.visibility = View.VISIBLE
    }

    private fun rebuildGlyphList() {
        glyphListContainer.removeAllViews()
        val selection = repository.getActiveSelection()
        val (showcaseImages, userImages) = repository.getAllImages()
            .partition(ShowcaseGlyphImages::isShowcaseImage)

        addCompositeItem()
        addCreateNewItem()
        userImages
            .sortedByDescending { image -> image.createdAt }
            .forEach { image -> addGlyphImageItem(image, selection) }
        showcaseImages.forEach { image -> addGlyphImageItem(image, selection) }
    }

    private fun addGlyphImageItem(image: CustomGlyphImage, selection: ActiveGlyphSelection?) {
        val item = layoutInflater.inflate(R.layout.item_glyph_image, glyphListContainer, false)
        val thumbnail = item.findViewById<GlyphMatrixView>(R.id.imageThumbnail)
        val name = item.findViewById<TextView>(R.id.imageName)
        val mode = item.findViewById<TextView>(R.id.imageMode)
        val badge = item.findViewById<TextView>(R.id.activeBadge)

        thumbnail.pixelGrid = GlyphImageSerializer.binaryToPixelGrid(image.pixels) ?: PixelGrid()
        thumbnail.maskedGrid = null
        thumbnail.interactiveMode = false
        name.text = image.name
        mode.text = if (selection?.imageId == image.id) {
            modeLabel(selection.mode)
        } else {
            getString(R.string.no_selection)
        }
        badge.visibility = if (selection?.imageId == image.id) View.VISIBLE else View.GONE
        item.setOnClickListener {
            startActivity(ImageEditorActivity.viewIntent(this, image.id))
        }
        glyphListContainer.addView(item)
    }

    private fun addCompositeItem() {
        val item = layoutInflater.inflate(R.layout.item_glyph_composite, glyphListContainer, false)
        item.setOnClickListener {
            startActivity(CompositeInfoActivity.intent(this))
        }
        glyphListContainer.addView(item)
    }

    private fun addCreateNewItem() {
        val item = layoutInflater.inflate(R.layout.item_glyph_create_new, glyphListContainer, false)
        item.setOnClickListener {
            startActivity(ImageEditorActivity.createIntent(this))
        }
        glyphListContainer.addView(item)
    }

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun openAodToyPicker() {
        val intent = Intent().setComponent(
            ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity",
            ),
        )
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // The AOD toy picker is only available on supported Nothing devices.
        }
    }

    private fun modeLabel(mode: DisplayPriority): String {
        return when (mode) {
            DisplayPriority.COMPOSITE -> getString(R.string.priority_composite_clock)
            DisplayPriority.IDLE_ONLY -> getString(R.string.priority_idle_only)
            DisplayPriority.ALWAYS_ON -> getString(R.string.priority_always_on)
        }
    }

    private fun liveModeLabel(mode: LiveGlyphMode): String {
        return when (mode) {
            LiveGlyphMode.CALL -> getString(R.string.live_mode_call)
            LiveGlyphMode.CLOCK -> getString(R.string.live_mode_clock)
            LiveGlyphMode.CUSTOM_IDLE -> getString(R.string.live_mode_custom_idle)
            LiveGlyphMode.EQUALIZER -> getString(R.string.live_mode_equalizer)
            LiveGlyphMode.STATIC_IMAGE -> getString(R.string.live_mode_static_image)
        }
    }

    private fun toySourceLabel(source: LiveGlyphSource): String {
        return when (source) {
            LiveGlyphSource.COMPOSITE_TOY -> getString(R.string.toy_name_composite)
            LiveGlyphSource.STATIC_IMAGE_TOY -> getString(R.string.toy_name_static)
        }
    }

    private fun currentClockGrid(): PixelGrid {
        val calendar = Calendar.getInstance()
        return FrameBuilders.buildClockGrid(
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
        )
    }

    private companion object {
        const val REQUEST_AUDIO = 1001
    }
}
