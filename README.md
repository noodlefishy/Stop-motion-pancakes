# Stop Motion Studio (Kotlin Desktop Application)

A professional, GPU-accelerated desktop video editor and studio designed specifically for **stop-motion animators**, built with **Kotlin** and **Compose Multiplatform (Jetpack Compose for Desktop)**.

---

## Features

### 1. Frame-by-Frame Doodle Studio (Rotoscoping & VFX)
- **Brush Styles**:
  - **Smooth Pen**: Precision cartoon outlines and hand-drawn accents.
  - **Neon Glow**: Lightsabers, laser bolts, magical auras, and Spider-Verse style energy trails.
  - **Sketch Chalk**: Textured, crayon/sketch lines.
  - **Dashed Speed Trail**: Comic puff trails and motion arcs.
- **One-Click Shape Stamps**: Star (★), Lightning (⚡), Speed Lines (💨), Heart (♥).
- **Copy Doodles to Next Frame (`Cmd+Shift+D`)**: Advance to the next frame while preserving your doodles so you can easily offset or animate them without redrawing from scratch.
- **Doodle Onion Skinning**: See your previous frame's doodle ghosted behind your current frame to maintain consistency and smooth motion arcs.
- **Undo Stroke & Clear**: Fast single-stroke undo and frame wipe.

### 2. Stop-Motion Specific Visual Inspection Tools
- **Advanced Onion Skinning**:
  - Ghost up to 5 past frames and 5 future frames.
  - Red / Green color tint mode (red for past, green for future) or true color.
  - Adjustable ghost opacity slider.
- **Difference / Flicker Checker**:
  - Highlights lighting inconsistencies and accidental camera bumps by computing absolute pixel differences.
- **Split-Screen Compare**:
  - Interactive slider comparing the current frame against the previous frame.
- **Composition Guides**:
  - Rule-of-thirds grid, center crosshair, and safe action/title margins.
  - Multi-aspect ratio masks: 16:9, 4:3, 1:1, 9:16 (vertical for Reels/TikTok), and 2.39:1 cinematic anamorphic.

### 3. Timeline & Exposure Holds (On 1s, 2s, 3s)
- **Filmstrip View**: High-resolution thumbnails with exposure hold badges (`1s`, `2s`, `3s`).
- **Batch Operations**: Multi-select frames to batch duplicate, delete, reverse, or re-time exposures.
- **Frame Reordering**: Shift frames left/right along the timeline.

### 4. Audio Engine & Lip Sync
- **Frame-Accurate Audio Scrubbing**: Hearing the exact phonetic audio slice as you scrub frame-by-frame (crucial for stop motion lip-sync animation).
- **Visual Audio Waveforms**: Aligned directly under the filmstrip frames.
- **Traditional Exposure Sheet (X-Sheet)**: Tabular dope sheet matching frame numbers to dialogue phonemes (`M`, `A`, `O`, `E`) and action notes.

### 5. Rig Removal (Clean Plate Inpainting)
- Erase armatures, clay support wires, or hands using a reference clean background plate or adjacent frame with a dedicated inpainting brush.

### 6. Auto De-Flicker & Color Grading
- One-click auto exposure matching to neutralize lighting flicker between shots.
- Per-frame brightness, contrast, and saturation adjustments.

### 7. Camera Capture Studio (with macOS Continuity Camera)
- **macOS Continuity Camera Support**: Seamlessly select and capture using your iPhone/iPad wireless or wired camera with special device detection badges (`📱 Continuity Camera`).
- **Resolution Control**: 1080p Full HD (Continuity Camera default), 720p HD, and 4K Ultra HD.
- **Viewfinder Ghosting**: Live onion skin overlay of your previous frame directly inside the camera viewfinder for millimeter-accurate positioning.
- **3-Second Countdown & Timelapse Intervalometer**: Hands-free capture for stop-motion sets.
- **Live Device Refresh**: One-click re-scan when an iPhone is brought near or connected.

### 8. Frame Deletion & Management
- **Single-Click Thumbnail Delete**: Red `✕` trash button on every filmstrip card.
- **Keyboard Delete (`Delete` / `Backspace`)**: Instant deletion of the active frame or all multi-selected frames.
- **Inspector Panel Action**: Clear "Delete Current Frame" button in the Frame Inspector.
- **Batch Deletion**: Select multiple frames (`Cmd+Click`) and wipe them in a single keystroke.
- **Full Undo/Redo (`Cmd+Z` / `Cmd+Shift+Z`)**: Safely recover any accidentally deleted frames.

### 9. Multi-Format Export
- **MP4 Video**: High-quality H.264 video with all audio tracks baked in.
- **Animated GIF**: 2-pass palette-optimized 256-color GIF.
- **Numbered Image Sequence ZIP**: Formatted `frame_00000.png` sequence for After Effects or DaVinci Resolve.
- **Project File**: Full `.smproj` JSON save & load.

---

## Keyboard Shortcuts

| Shortcut | Action |
| :--- | :--- |
| `Space` | Play / Pause (or Snap in Camera Studio) |
| `Delete` / `Backspace` | **Delete Selected Frame(s)** |
| `Left Arrow` | Step -1 Frame |
| `Right Arrow` | Step +1 Frame |
| `Home` / `End` | Jump to Start / End |
| `Cmd + Z` | Undo |
| `Cmd + Shift + Z` | Redo |
| `Cmd + D` | Duplicate Selected Frame |
| `Cmd + Shift + D` | Copy Doodles to Next Frame |
| `O` | Toggle Onion Skinning |
| `B` | Doodle Pen Tool |
| `C` | Open Camera Capture Studio |
| `Cmd + E` | Open Export Dialog |

---

## Running the Application

### Launch Dev App:
```bash
./gradlew run
```

### Run Automated Tests:
```bash
./gradlew test
```

### Build Native macOS `.dmg` Installer:
```bash
./gradlew packageDmg
```
The compiled macOS `.dmg` installer will be located in `build/compose/binaries/main/dmg/`.
