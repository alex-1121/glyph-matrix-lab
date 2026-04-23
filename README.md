# Matrix Lab

An Android app for creating and managing Glyph Toys on the Nothing Phone (4a) Pro 13x13 Glyph Matrix LED display.

Create custom 13x13 pixel images, save multiple designs, choose one for display.

## Features

<img width="640" height="551" alt="eq" src="https://github.com/user-attachments/assets/08072335-a6b1-4e60-b501-f9df219c660d" />

- Live preview on the app's home screen — mirrors what the Glyph Matrix is displaying in real time
- Audio permission management for the reactive equalizer
- List of saved custom glyph images. Includes few sample images.
- 13x13 pixel image editor
- Display mode selection per image:
  - **Idle Only**: shown by the multi-function toy when the phone is idle
  - **Always On**: shown by the static image toy at all times
- AOD toy setup guidance
- Two AOD Glyph Toys: **Matrix Lab: Multi function** and **Matrix Lab: Static image**

## Glyph Toys

### Matrix Lab: Multi function (CompositeToyService)

A multimode always-on display Glyph Toy with automatic mode switching:

| Priority | Mode | Description |
|----------|------|-------------|
| 1 | **Call** | Phone icon during active calls |
| 2 | **Equalizer** | Audio-reactive bars using real-time waveform data |
| 3 | **Custom Idle** | Selected custom image when configured as Idle Only |
| 4 | **Clock** | 4x5 pixel digits showing HH:MM |

Select **Matrix Lab: Multi function** in AOD Glyph Toy settings when using an image with **Idle Only** mode. Calls and music override the custom image; otherwise it replaces the clock.

### Matrix Lab: Static image (StaticImageToyService)

Renders the selected custom image when the active selection mode is **Always On**. If no always-on image is configured, it renders a blank frame.

Select **Matrix Lab: Static image** in AOD Glyph Toy settings when using an image with **Always On** mode.

## App Flow

1. Open Matrix Lab.
2. Grant audio permission if you want the reactive equalizer.
3. Browse the showcase presets or tap **+ Create New Image**.
4. Toggle pixels in the 13x13 editor and save the image.
5. Open a saved image and tap **Select for Display**.
6. Choose **Idle Only** or **Always On**.
7. If you see a warning, follow the guidance there.

The app cannot programmatically activate or switch Nothing toy services. Only one toy can be active at a time, and final activation happens in:

```text
Settings > Glyph Interface > Always-on Glyph Toy
```

Also in my experience, after switching Glyph Toys in the settings, Glyph Matrix does not display anyting. Workaround is to turn Glyph Interface off and on (Settings - Glyph Interface). This happens with standard Glyph Toys too, hopefully it gets fixed soon.

## Requirements

- Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`)
- Android SDK 34+
- Glyph Matrix SDK 2.0, included at `app/libs/glyph-matrix-sdk-2.0.aar`

## Quick Start

```bash
# Build and install
./gradlew installDebug

# Enable Glyph debug mode (expires after 48 hours)
adb shell settings put global nt_glyph_interface_debug_enable 1

# Open the Always-on Glyph Toy picker
adb shell am start -n com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity
```

Then select the appropriate toy in **Settings > Glyph Interface > Always-on Glyph Toy**.

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Build and install on connected device
./gradlew test                 # Run local unit tests
./gradlew testDebugUnitTest    # Run fast debug JVM tests
./gradlew connectedAndroidTest # Run instrumented tests on a device
./gradlew clean build          # Clean rebuild
```

## Architecture

Toy services extend `GlyphToyBase`, which handles SDK binding and Glyph event routing. Rendering goes through pure domain objects where possible:

```text
CompositeToyService
    └── CompositeToyController
            ├── FrameBuilders
            ├── EqualizerProcessor
            ├── RepositoryCustomGlyphProvider
            └── PixelGrid
                    └── GlyphDisplayAdapter

StaticImageToyService
    └── GlyphImageRepository
            └── GlyphImageSerializer
                    └── PixelGrid
```

Image management:

- `MainActivity` — home screen, permission status, live preview, gallery
- `ImageEditorActivity` — create, view, edit, delete, and select images
- `CompositeInfoActivity` — AOD toy setup guidance for the multi-function toy
- `GlyphMatrixView` — Canvas-based grid renderer for previews and editing
- `MaskedPixelGrid` — editor-only mask model
- `GlyphImageRepository` — SharedPreferences storage
- `GlyphImageSerializer` — 169-bit binary string serialization
- `ShowcaseGlyphImages` — built-in preset images seeded on first launch

This keeps serializer, grid, frame builder, equalizer, and controller logic testable with JVM unit tests.

## Persistence

Custom images are stored in SharedPreferences:

```text
image_list = "id1,id2"
image_<id>_name = "Image Name"
image_<id>_pixels = "010101..."       # exactly 169 bits
image_<id>_created = 1714000000
image_<id>_updated = 1714000000
active_selection_id = "id1"
active_selection_mode = "IDLE_ONLY"   # or "ALWAYS_ON"
active_selection_updated = 1714000000
```

Deleting the active image clears the active selection.

## Permissions

- `RECORD_AUDIO` — required for the equalizer to react to playback through `Visualizer(0)`. Open the app to trigger the permission request, or grant microphone permission manually in Android settings.
- `MODIFY_AUDIO_SETTINGS` — required for output-mix visualization on the target device.

## Testing

```bash
./gradlew testDebugUnitTest    # Fast local loop for pure logic
./gradlew connectedAndroidTest # Instrumented tests (requires device)
```

For toy behavior changes, also install on the phone and reselect the toy in the AOD picker:

```bash
./gradlew installDebug
adb shell am start -n com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity
```

## Resources

- [Glyph Developer Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit)
- [Nothing Developer Programme](https://nothing.tech/pages/developers)

## License

This project uses the Nothing Glyph Matrix SDK under Nothing's developer terms.
