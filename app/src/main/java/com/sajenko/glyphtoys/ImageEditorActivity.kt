package com.sajenko.glyphtoys

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sajenko.glyphtoys.models.ActiveGlyphSelection
import com.sajenko.glyphtoys.models.CustomGlyphImage
import com.sajenko.glyphtoys.models.DisplayPriority
import com.sajenko.glyphtoys.models.MaskedPixelGrid
import com.sajenko.glyphtoys.repository.GlyphImageRepository
import com.sajenko.glyphtoys.serialization.GlyphImageSerializer
import com.sajenko.glyphtoys.views.GlyphMatrixView
import java.util.UUID

class ImageEditorActivity : Activity() {
    private lateinit var repository: GlyphImageRepository
    private lateinit var titleView: TextView
    private lateinit var matrixView: GlyphMatrixView
    private lateinit var instructionsView: TextView
    private lateinit var createButtonBar: LinearLayout
    private lateinit var viewButtonBar: LinearLayout
    private lateinit var editButtonBar: LinearLayout
    private lateinit var createSaveButton: Button
    private lateinit var editButton: Button
    private lateinit var selectButton: Button
    private lateinit var deleteButton: Button
    private lateinit var cancelButton: Button
    private lateinit var clearButton: Button
    private lateinit var editSaveButton: Button

    private var imageId: String? = null
    private var currentImage: CustomGlyphImage? = null
    private var maskedGrid: MaskedPixelGrid = MaskedPixelGrid.createWithPhoneMask()
    private var mode: EditorMode = EditorMode.CREATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_editor)

        repository = GlyphImageRepository(
            getSharedPreferences(GlyphImageRepository.PreferencesName, Context.MODE_PRIVATE),
        )
        imageId = intent.getStringExtra(ExtraImageId)
        bindViews()
        bindActions()

        if (imageId == null) {
            switchMode(EditorMode.CREATE)
        } else {
            loadImageOrFinish()
            switchMode(EditorMode.VIEW)
        }
    }

    private fun bindViews() {
        findViewById<View>(R.id.editorRoot).applySystemBarsPadding()
        titleView = findViewById(R.id.editorTitle)
        matrixView = findViewById(R.id.editorMatrixView)
        instructionsView = findViewById(R.id.editorInstructions)
        createButtonBar = findViewById(R.id.createButtonBar)
        viewButtonBar = findViewById(R.id.viewButtonBar)
        editButtonBar = findViewById(R.id.editButtonBar)
        createSaveButton = findViewById(R.id.createSaveButton)
        editButton = findViewById(R.id.editButton)
        selectButton = findViewById(R.id.selectButton)
        deleteButton = findViewById(R.id.deleteButton)
        cancelButton = findViewById(R.id.cancelButton)
        clearButton = findViewById(R.id.clearButton)
        editSaveButton = findViewById(R.id.editSaveButton)
    }

    private fun bindActions() {
        createSaveButton.setOnClickListener { promptAndCreateImage() }
        editButton.setOnClickListener { switchMode(EditorMode.EDIT) }
        selectButton.setOnClickListener { promptDisplayMode() }
        deleteButton.setOnClickListener { confirmDelete() }
        cancelButton.setOnClickListener {
            loadImageOrFinish()
            switchMode(EditorMode.VIEW)
        }
        clearButton.setOnClickListener {
            maskedGrid.clear()
            matrixView.invalidate()
        }
        editSaveButton.setOnClickListener { saveExistingImage() }
    }

    private fun loadImageOrFinish() {
        val id = imageId ?: return
        val image = repository.getImage(id)
        if (image == null) {
            finish()
            return
        }
        currentImage = image
        maskedGrid = MaskedPixelGrid.fromPixelGrid(
            GlyphImageSerializer.binaryToPixelGrid(image.pixels) ?: return finish(),
        )
    }

    private fun switchMode(nextMode: EditorMode) {
        mode = nextMode
        when (nextMode) {
            EditorMode.CREATE -> {
                titleView.setText(R.string.editor_title_create)
                instructionsView.setText(R.string.editor_instructions_interactive)
                matrixView.maskedGrid = maskedGrid
                matrixView.pixelGrid = null
                matrixView.interactiveMode = true
                createButtonBar.visibility = View.VISIBLE
                viewButtonBar.visibility = View.GONE
                editButtonBar.visibility = View.GONE
            }
            EditorMode.VIEW -> {
                titleView.text = currentImage?.name ?: getString(R.string.editor_title_view)
                instructionsView.setText(R.string.editor_instructions_view)
                matrixView.maskedGrid = maskedGrid
                matrixView.pixelGrid = null
                matrixView.interactiveMode = false
                createButtonBar.visibility = View.GONE
                viewButtonBar.visibility = View.VISIBLE
                editButtonBar.visibility = View.GONE
            }
            EditorMode.EDIT -> {
                titleView.setText(R.string.editor_title_edit)
                instructionsView.setText(R.string.editor_instructions_interactive)
                matrixView.maskedGrid = maskedGrid
                matrixView.pixelGrid = null
                matrixView.interactiveMode = true
                createButtonBar.visibility = View.GONE
                viewButtonBar.visibility = View.GONE
                editButtonBar.visibility = View.VISIBLE
            }
        }
        matrixView.invalidate()
    }

    private fun promptAndCreateImage() {
        val input = EditText(this).apply {
            hint = getString(R.string.name_image_hint)
            setSingleLine(true)
            setPadding(48, 16, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.name_image_dialog_title)
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val now = System.currentTimeMillis()
                val name = input.text.toString().trim().ifEmpty { getString(R.string.untitled_image) }
                val image = CustomGlyphImage(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    pixels = GlyphImageSerializer.pixelGridToBinary(maskedGrid.toPixelGrid()),
                    createdAt = now,
                    updatedAt = now,
                )
                repository.saveImage(image)
                finish()
            }
            .show()
    }

    private fun saveExistingImage() {
        val image = currentImage ?: return
        val updated = image.copy(
            pixels = GlyphImageSerializer.pixelGridToBinary(maskedGrid.toPixelGrid()),
            updatedAt = System.currentTimeMillis(),
        )
        repository.saveImage(updated)
        currentImage = updated
        switchMode(EditorMode.VIEW)
    }

    private fun promptDisplayMode() {
        val image = currentImage ?: return
        val dialogView = layoutInflater.inflate(R.layout.dialog_display_mode, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.idleModeButton).setOnClickListener {
            selectDisplayMode(image, DisplayPriority.IDLE_ONLY)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.alwaysModeButton).setOnClickListener {
            selectDisplayMode(image, DisplayPriority.ALWAYS_ON)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun selectDisplayMode(image: CustomGlyphImage, priority: DisplayPriority) {
        repository.setActiveSelection(
            ActiveGlyphSelection(
                imageId = image.id,
                mode = priority,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        val toastRes = if (priority == DisplayPriority.IDLE_ONLY) {
            R.string.toast_idle_configured
        } else {
            R.string.toast_static_configured
        }
        Toast.makeText(this, toastRes, Toast.LENGTH_LONG).show()
        openAodToyPicker()
    }

    private fun confirmDelete() {
        val image = currentImage ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete_confirm) { _, _ ->
                repository.deleteImage(image.id)
                finish()
            }
            .show()
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
            // The toast above is the fallback path on non-Nothing devices.
        }
    }

    private enum class EditorMode {
        CREATE,
        VIEW,
        EDIT,
    }

    companion object {
        private const val ExtraImageId = "image_id"

        fun viewIntent(context: Context, imageId: String): Intent {
            return Intent(context, ImageEditorActivity::class.java)
                .putExtra(ExtraImageId, imageId)
        }

        fun createIntent(context: Context): Intent {
            return Intent(context, ImageEditorActivity::class.java)
        }
    }
}
