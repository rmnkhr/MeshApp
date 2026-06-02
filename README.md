# MashApp — Mesh Gradient Editor

An Android (Jetpack Compose) playground for creating and editing **mesh gradients**,
using the new `MeshGradientPainter` API.

## Features

- Interactive mesh gradient with draggable control points
- Per-vertex color editing and harmony-based palette generation
- Adjustable grid size (2–5 points per side)
- Smooth, bicubic (Catmull-Rom) color interpolation
- Reset to a built-in default layout

## Requirements

- Android Studio (with a recent Canary/alpha toolchain)
- Android SDK 33+ (the app targets a preview SDK and uses Compose alpha APIs)

## Getting started

```bash
git clone <your-repo-url>
cd MashApp
./gradlew assembleDebug
```

Then open the project in Android Studio and run it on a device or emulator.

> Note: `local.properties` is intentionally not committed. Android Studio
> generates it automatically with your local SDK path on first open.

## Tech

- Kotlin + Jetpack Compose (Material 3)
- `androidx.compose.ui.graphics.MeshGradientPainter` (Compose UI alpha)
