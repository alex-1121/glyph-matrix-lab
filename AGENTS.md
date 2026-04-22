# AGENTS.md

This file provides guidance to coding agents working in this repository.

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew installDebug         # Build and install on connected device
./gradlew test                 # Run unit tests
./gradlew testDebugUnitTest    # Run debug JVM tests only (fast local loop)
./gradlew connectedAndroidTest # Run instrumented tests (requires device)
./gradlew clean build          # Clean rebuild
```

## Testing

Use these commands depending on what you need to verify:

```bash
./gradlew testDebugUnitTest
```

Runs the fast local JVM test suite for the debug variant. Use this for extracted pure logic such as `PixelGrid`, `FrameBuilders`, and `EqualizerProcessor`.

```bash
./gradlew test
```

Runs the full local unit test suite across configured variants.

```bash
./gradlew connectedAndroidTest
```

Runs instrumented tests on a connected device or emulator. This is slower and requires hardware/device setup.

For toy behavior changes, do not stop at Gradle tests. Also verify on-device:

```bash
./gradlew installDebug
adb shell am start -n com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity
```

Then reselect the toy in `Settings -> Glyph Interface -> Always-on Glyph Toy` and confirm the rendered behavior on the phone.

## Device Setup

Debug mode is required and auto-disables after 48 hours:

```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```

Open Toys Manager on device:

```bash
adb shell am start -n com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity
```

Or via intent in code:

```kotlin
val intent = Intent()
intent.setComponent(
    ComponentName(
        "com.nothing.thirdparty",
        "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
    )
)
startActivity(intent)
```

Open the Always-on Glyph Toy picker directly:

```bash
adb shell am start -n com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity
```

Device navigation path:

```text
Settings -> Glyph Interface -> Always-on Glyph Toy
```

Only one toy can be active at a time. The master toggle is at the top of that screen.

## Project Overview

This is an Android app for creating Glyph Toys, which are services that render visuals on the Nothing Phone (4a) Pro 13x13 Glyph Matrix LED display. The launcher UI handles audio permission, shows the latest live toy frame when available, lists saved custom images, and provides a masked 13x13 image editor. Toy output still comes from background services activated from device Settings.

SDK location:

```text
app/libs/glyph-matrix-sdk-2.0.aar
```

The proprietary Nothing SDK provides `GlyphMatrixManager`, `GlyphMatrixFrame`, `GlyphMatrixObject`, `GlyphToy`, and related classes under `com.nothing.ketchum`.

Preview assets for toy registration should be prepared as SVG/vector drawables where possible; the Nothing README explicitly recommends exporting preview images as SVG before importing them into Android Studio.

If a toy needs real-time output audio analysis via `android.media.audiofx.Visualizer` on session `0`, declare `android.permission.RECORD_AUDIO` in the manifest and request it at runtime from the launcher activity; without it, `Visualizer(0)` may fail and the toy should degrade gracefully.

## Target Device

- Device identifier: `Glyph.DEVICE_25111p`
- Matrix size: 13x13 LEDs
- No Glyph Touch hardware
- AOD toys only
- Every toy must declare `aod_support=1` in the manifest
- `GlyphToy.EVENT_AOD` fires every minute while the toy is active

## Architecture

### Creating a Toy

Each toy is an Android `Service` that extends `GlyphToyBase`. The base class:

- Binds to `GlyphMatrixManager` in `onCreate`
- Unbinds in `onDestroy`
- Routes IPC messages from the Glyph system using `Messenger` and `Handler`
- Exposes callbacks: `onServiceConnected(context, gmm)`, `onServiceDisconnected(context)`, `onAod()`, `onTouchDown()`, `onTouchUp()`, and `onLongPress()`

### Pure Domain Layer

Toys can delegate to pure abstractions that are testable without Robolectric:

- `PixelGrid` — 13×13 boolean domain model (no Android dependencies)
- `FrameSink` — interface for rendering frames
- `GlyphDisplayAdapter` — converts `PixelGrid` → `Bitmap` → SDK
- `FrameBuilders` — static builders for clock, equalizer, call grids
- `EqualizerProcessor` — waveform → bar heights with decay
- `CompositeToyController` — orchestrates mode switching (CALL/EQUALIZER/CUSTOM_IDLE/CLOCK)
- `CustomGlyphProvider` / `RepositoryCustomGlyphProvider` — supplies selected idle custom images to the composite toy
- `GlyphImageSerializer` — serializes custom 13x13 images as 169-character binary strings

This separation enables fast JVM unit tests via `./gradlew testDebugUnitTest`.

### App UI And Preview

- `MainActivity` requests microphone permission, shows the latest in-process `LiveGlyphPreview` frame, and falls back to the configured custom image when no live frame exists.
- `LiveGlyphPreview` is process-local memory. Force-stopping or reinstalling the app clears the latest frame until an active toy service publishes again.
- `ImageEditorActivity` edits `MaskedPixelGrid.createWithPhoneMask()` so only valid Phone (4a) Pro matrix pixels can be toggled.
- Saved images live in `GlyphImageRepository`; the active selection stores both `imageId` and `DisplayPriority`.
- `DisplayPriority.IDLE_ONLY` means select **Composite Glyph** in the AOD picker; calls and music override the image, otherwise it replaces the clock.
- `DisplayPriority.ALWAYS_ON` means select **Static Image** in the AOD picker; it keeps the selected image on the matrix until another system/toy priority overrides it.
- `GlyphMatrixView` renders previews with sharp square cells, proportional black gaps, off-pixels `#1C1C1C`, and on-pixels white. Gaps are always present; there is no `showGrid` toggle or separate grid-line resource.

### Manifest Registration

For Phone (4a) Pro, `aod_support=1` is required because the device only supports AOD toys.

```xml
<service
    android:name=".toys.MyToyService"
    android:exported="true"
    tools:ignore="ExportedService">
    <intent-filter>
        <action android:name="com.nothing.glyph.TOY" />
    </intent-filter>

    <meta-data
        android:name="com.nothing.glyph.toy.name"
        android:resource="@string/toy_name" />

    <meta-data
        android:name="com.nothing.glyph.toy.image"
        android:resource="@drawable/toy_preview" />

    <meta-data
        android:name="com.nothing.glyph.toy.summary"
        android:resource="@string/toy_summary" />

    <meta-data
        android:name="com.nothing.glyph.toy.introduction"
        android:value="com.yourPackage.YourIntroActivity" />

    <meta-data
        android:name="com.nothing.glyph.toy.longpress"
        android:value="1" />

    <meta-data
        android:name="com.nothing.glyph.toy.aod_support"
        android:value="1" />
</service>
```

Also required in `<manifest>`:

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />
<!-- Optional: only needed for audio visualization toys -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

Also required in `<application>`:

```xml
<meta-data android:name="NothingKey" android:value="test" />
```

`"test"` is acceptable for debug builds. Use a real key for production.

### Rendering

Inside `onServiceConnected()`, use the injected `GlyphMatrixManager` to render:

```kotlin
val bitmap = Bitmap.createBitmap(13, 13, Bitmap.Config.ARGB_8888)

val obj = GlyphMatrixObject.Builder()
    .setImageSource(bitmap)
    .setBrightness(200)
    .setScale(100)
    .setPosition(0, 0)
    .setOrientation(0)
    .build()

val frame = GlyphMatrixFrame.Builder()
    .addTop(obj)
    .build(context)

gmm.setMatrixFrame(frame)
```

Important distinction:

- `setMatrixFrame()` is for toy services and has higher display priority
- `setAppMatrixFrame()` is for foreground app UI and is overridden by toys
- `setAppMatrixFrame()` is only for app-driven matrix control and requires phone system version `20250801` or later; toy services should continue using `setMatrixFrame()`

### GlyphToy Events

Handled through `Messenger` and `Handler` in `GlyphToyBase`:

- `GlyphToy.EVENT_AOD`: fires every minute when active as the AOD toy
- `GlyphToy.EVENT_CHANGE`: long-press on Glyph Button
- `GlyphToy.EVENT_ACTION_DOWN`: Glyph Button pressed down
- `GlyphToy.EVENT_ACTION_UP`: Glyph Button released

Message format:

- `msg.what == GlyphToy.MSG_GLYPH_TOY`
- Event string is in `bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)`

### GlyphMatrixObject Properties

- `setBrightness()`: `0..255`, default `255`
- `setScale()`: `0..200`, default `100`
- `setOrientation()`: `0..360`, default `0`
- `setPosition()`: defaults to `0, 0`
- `setImageSource()`: requires a square bitmap
- `setText()`: displays text on the matrix

## Current Toys

### CompositeToyService (Clock, Music, Calls, Custom Idle)

Multi-mode toy with automatic display switching:

- **CLOCK mode**: 4×5 pixel digits showing HH:MM, refreshes every minute via `onAod()`
- **EQUALIZER mode**: 13-column audio-reactive equalizer using `Visualizer(0)` for real-time waveform data; bars smooth with 0.55 decay factor; graceful fallback to static bars if Visualizer fails
- **CALL mode**: Animated phone icon displayed during calls (detected via `AudioManager.OnModeChangedListener`)
- **CUSTOM_IDLE mode**: Selected `IDLE_ONLY` custom image shown when there is no call or media playback

Mode transitions are automatic: call → CALL, playback → EQUALIZER, idle with selected idle image → CUSTOM_IDLE, otherwise idle → CLOCK.

**Internal architecture:** The service delegates to `CompositeToyController` which operates on pure abstractions:

- `PixelGrid` — 13×13 boolean domain model (no Android dependencies)
- `FrameSink` — interface for rendering frames
- `GlyphDisplayAdapter` — converts `PixelGrid` → `Bitmap` → SDK
- `FrameBuilders` — static builders for clock, equalizer, call grids
- `EqualizerProcessor` — waveform → bar heights with decay
- `RepositoryCustomGlyphProvider` — reads the current `IDLE_ONLY` image from shared preferences
- `LiveGlyphPreview` — publishes the current rendered grid back to the launcher process while the service is alive

This separation allows JVM unit testing without Robolectric.

### StaticImageToyService

AOD toy for custom images selected with `DisplayPriority.ALWAYS_ON`:

- Loads the active image from `GlyphImageRepository`
- Renders the image through `GlyphDisplayAdapter`
- Re-renders on `onAod()` and relevant shared-preference changes
- Displays an empty grid when there is no active `ALWAYS_ON` image
- Publishes `LiveGlyphPreview` frames with `LiveGlyphMode.STATIC_IMAGE`

## Troubleshooting

### Equalizer Shows Static Bars Instead of Reacting to Audio

**Symptom:** EQUALIZER mode activates (playback detected correctly), but bars remain static at height 6 instead of animating with the music.

**Root causes:**
- `RECORD_AUDIO` permission not granted. The `Visualizer(0)` constructor succeeds but callbacks never fire, causing `lastWaveformTimestampMs` to become stale and triggering fallback bars.
- `Visualizer` startup state bug on some devices. `permissionGranted=true`, but `setCaptureSize()` throws `IllegalStateException` because the instance is already enabled when capture size is configured.

**Diagnosis:** Check logcat for:

```
permissionGranted=false
Waveform meter unavailable: RECORD_AUDIO permission missing
```

Or:

```
permissionGranted=true
Visualizer rejected captureSize=256 during startup; continuing with default capture size (IllegalStateException: setCaptureSize() called in wrong state: 2)
No waveform received within 1000ms of Visualizer startup; restarting meter
```

**Resolution:**
- For missing permission: open the GlyphToys app (MainActivity) to trigger the runtime permission request, or manually grant microphone permission via Settings → Apps → GlyphToys → Permissions.
- For the startup state exception: use the fixed startup order (`enabled = false`, then `captureSize`, `scalingMode`, listener, `enabled = true`). If callbacks still do not begin, the one-shot startup health check restarts the meter automatically.

**Note:** The toy service runs in the background and cannot request permissions itself. The permission must be granted via the launcher activity before the toy will work. Once granted, `Visualizer(0)` (global output mix) works correctly on Nothing Phone (4a) Pro, and the startup health check covers the case where the initial callback stream never begins.

## Key Constraints

- Min SDK is 34
- The app targets Nothing Phone (4a) Pro only
- The `app/libs/glyph-matrix-sdk-2.0.aar` SDK is intentionally committed despite common ignore patterns
- `GlyphMatrixFrame.Builder.build()` requires a `Context` in SDK 2.0+
- Brightness range is `0..255`, not `0..4095`
- A `GlyphMatrixFrame` supports at most 3 objects, one per layer: top, mid, low
- Use `Common.getDeviceMatrixLength()` to resolve the runtime matrix size
- Keep `NothingKey` metadata for compatibility, even though Android 16+ removed the API key restriction

## Agent Notes

- Prefer preserving device-specific constraints over making the code generic
- When adding a new toy, verify both manifest metadata and service wiring, not just rendering code
- When changing custom image behavior, verify both the editor/repository flow and the AOD toy selection path (`Composite Glyph` for idle-only, `Static Image` for always-on)
- When changing `GlyphMatrixView`, verify MainActivity previews, thumbnails, editor rendering, and touch toggling on device or with screenshots
- For behavior changes, run at least `./gradlew testDebugUnitTest` or `./gradlew test`, then verify the actual device activation path when hardware is available
- Finish any toy addition or behavior change by running `./gradlew installDebug` to deploy the updated app to the connected device
- After deployment, open the AOD toy picker with `adb shell am start -n com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity` so the updated toy can be selected immediately
- For real-device verification, prefer `./gradlew installDebug` over `assembleDebug`; a successful build alone does not update the phone
- If a toy change does not appear on-device after reinstall, treat the Nothing toy picker as potentially caching the old component; bump the app version, give the toy a fresh service identity if needed, and reselect it in `Settings -> Glyph Interface -> Always-on Glyph Toy`
