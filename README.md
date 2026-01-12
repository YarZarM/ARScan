# AR Defect Marker Application

An Android POC application for marking and tracking defects in physical spaces using Augmented Reality.

---

## Table of Contents

1. [Overview](#overview)
2. [Platform & Stack](#platform--stack)
3. [Demo](#demo)
4. [Features](#features)
5. [Architecture](#architecture)
6. [Setup Instructions](#setup-instructions)
7. [Usage Guide](#usage-guide)
8. [Assumptions & Limitations](#assumptions--limitations)

---

## Overview

RendinXR allows users to scan physical environments using their device's camera, detect surfaces (floors, walls, ceilings, tables), and place visual markers at defect locations. Each marked defect captures:

- **3D World Position**: Precise X, Y, Z coordinates in AR space
- **Surface Type**: Automatically classified based on plane orientation
- **Photo Evidence**: Camera snapshot at the moment of marking
- **User Description**: Text annotation describing the defect

Defects can be reviewed in a traditional list view or visualized in an interactive 3D spatial view showing their relative positions.

---

## Platform & Stack

### Target Platform
| Component | Specification             |
|-----------|---------------------------|
| **Platform** | Android                   |
| **Minimum SDK** | API 24 (Android 7.0)      |
| **Target SDK** | API 36 (Android 16)       |
| **Tested Device** | Samsung S23+ (Android 16) |

### Core Technologies

| Category | Technology | Version        | Purpose |
|----------|------------|----------------|---------|
| **Language** | Kotlin | 2.3.0          | Primary development language |
| **UI Framework** | Jetpack Compose | BOM 2024.09.00 | Declarative UI |
| **AR Engine** | Google ARCore | Latest         | Surface detection, plane tracking, anchor management |
| **3D Rendering** | SceneView | 2.3.1          | Compose-friendly wrapper for Filament 3D engine |
| **3D Engine** | Filament | (bundled)      | Low-level 3D rendering via SceneView |
| **Architecture** | MVVM + Clean Architecture | -              | Separation of concerns |
| **DI Framework** | Hilt | 2.57.1         | Dependency injection |
| **Database** | Room | 2.8.4          | Local SQLite persistence |
| **Image Loading** | Coil 3 | 3.3.0          | Async image loading for Compose |
| **Navigation** | Navigation Compose | 2.9.6          | Screen navigation |
| **Build Tool** | Gradle (Kotlin DSL) | 8.1.3          | Build system |

### ARCore Requirements

This application requires ARCore-supported devices. Key requirements:

- **ARCore Supported Device**: See [Google's supported devices list](https://developers.google.com/ar/devices)
- **Google Play Services for AR**: Must be installed (prompted automatically)
- **Camera Permission**: Required for AR functionality
- **Adequate Lighting**: Surface detection works best in well-lit environments

---

## Demo

[▶ Watch the demo](./demo.mp4)

---

## Features

### Implemented Features

#### AR Scanning Mode
-  Real-time camera feed with AR overlay
-  Automatic surface detection (horizontal and vertical planes)
-  Visual plane rendering showing detected surfaces
-  Tap-to-place defect markers on detected surfaces
-  Surface type classification (Floor, Wall, Ceiling, Table)
-  Automatic image capture at marker placement
-  Color-coded markers by surface type
-  Real-time tracking status indicator
-  Defect counter overlay

#### Defect Management
-  Description dialog for each defect
-  Local database persistence (Room)
-  Image storage with thumbnail generation
-  View all defects in list format
-  Defect detail view with full image
-  Delete individual defects
-  Delete all defects
-  Timestamp tracking

#### 3D Spatial Review
-  3D visualization of defect positions
-  Colored spheres representing defects
-  Surface type color coding
-  Interactive orbit camera (drag to rotate)
-  Pinch-to-zoom
-  Tap sphere to select defect
-  Floor grid for spatial reference
-  Selection highlighting

---

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Screens    │  │  ViewModels │  │  UI State/Events    │  │
│  │  (Compose)  │  │  (Hilt)     │  │  (StateFlow)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  ┌─────────────┐  ┌─────────────────────────────────────┐   │
│  │   Models    │  │         Repository Interfaces        │   │
│  │  (Defect)   │  │         (DefectRepository)           │   │
│  └─────────────┘  └─────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │    Room     │  │   Mappers   │  │   Image Storage     │  │
│  │  Database   │  │             │  │   (File System)     │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Module Structure

```
com.example.rendinxr/
├── core/
│   ├── data/                    # Database, ImageStorage
│   ├── domain/model/            # Defect, SurfaceType
│   └── presentation/navigation/ # NavGraph, Screen
├── feature/
│   ├── scan/
│   │   ├── data/               # Repository impl, DAO, Entity
│   │   ├── domain/             # Repository interface
│   │   └── presentation/       # ScanScreen, ViewModel
│   └── review/
│       └── presentation/       # ReviewScreen, Spatial3DView
├── di/                         # Hilt modules
└── ui/theme/                   # Material3 theming
```

---

## Setup Instructions

### Prerequisites

1. **Android Studio** Otter 2 Feature Drop | 2025.2.2 Patch 1 or newer
2. **Android SDK** with API 36 installed
3. **ARCore-supported Android device** (emulator not recommended for AR features)

### Step 1: Clone/Extract Project

```bash
git clone <repository-url>
```

### Step 2: Open in Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Wait for Gradle sync to complete


### Step 3: Build and Run

1. Connect to an ARCore-supported Android device via USB
2. Enable USB debugging on the device
3. Click "Run" in Android Studio
4. Select your connected device
5. Wait for installation to complete

### Step 4: Grant Permissions

On first launch:
1. Allow camera permission when prompted
2. If Google Play Services for AR needs updating, follow the prompts

---

## Usage Guide

### Scanning for Defects

1. **Launch the app** and grant camera permission
2. **Move your device slowly** to allow ARCore to detect surfaces
3. **Wait for "Tracking" status** (green indicator)
4. **Look for detected planes** - they appear as grid overlays on surfaces
5. **Tap on a detected surface** to place a defect marker
6. **Enter a description** in the dialog that appears
7. **Save** the defect

### Surface Type Classification

| Surface Type | Color | Detection |
|--------------|-------|-----------|
| **Floor** | Green | Horizontal upward-facing plane below -0.5m |
| **Table** | Orange | Horizontal upward-facing plane above -0.5m |
| **Wall** | Blue | Vertical plane |
| **Ceiling** | Purple | Horizontal downward-facing plane |

### Reviewing Defects

1. **Tap "Review"** button (shows count of saved defects)
2. **List View**: Scroll through defects with thumbnails
3. **3D View**: Switch to tab showing spatial positions
4. **Tap a defect** to see full details and image
5. **Delete** individual defects or all at once

### 3D Spatial View Controls

- **Drag** to rotate the view
- **Pinch** to zoom in/out
- **Tap sphere** to select a defect
- Selected defects highlight in yellow

---

## Assumptions & Limitations

### Assumptions

1. **Single Session**: All defects are assumed to be marked within a single AR session. Positions are relative to the session's origin.

2. **Indoor Use**: Optimized for indoor environments with good lighting and feature-rich surfaces.

3. **Device Capability**: Assumes device meets ARCore requirements and has adequate processing power.

4. **Local Storage Only**: All data stored locally on device. No cloud sync or multi-device support.

### Intentional Limitations (Prototype Scope)

| Feature | Status | Notes |
|---------|--------|-------|
| **Room Capture / 3D Mesh** | Platform limitation | Android lacks iOS RoomPlan equivalent; ARCore only provides plane detection |
| **Wall Plane Visualization** | Library limitation | SceneView PlaneRenderer only shows horizontal planes; walls detected but not visualized |
| **Cloud Sync** | Not implemented | Data is device-local only |
| **Multi-user** | Not implemented | Single-user application |
| **Export/Import** | Not implemented | Cannot export defect reports |
| **PDF Reports** | Not implemented | No report generation |
| **Session Persistence** | Not implemented | AR positions lost when app closes |
| **Anchor Cloud Hosting** | Not implemented | Cannot share anchors across devices |
| **Defect Editing** | Not implemented | Cannot edit after saving |
| **Categories/Tags** | Not implemented | Only surface type classification |
| **Search/Filter** | Not implemented | No search functionality |
| **Offline Maps** | Not implemented | No floor plan integration |
| **Measurements** | Not implemented | No distance/area measurements |
| **AR Relocalization** | Not implemented | Cannot return to previous session positions |

### Technical Limitations
1. **No Room Capture / 3D Mesh Scanning**: Unlike iOS with RoomPlan API and LiDAR, Android/ARCore does not provide a built-in room capture or 3D mesh reconstruction API. Our app can only detect flat planes (floors, walls, ceilings) but cannot generate a full 3D mesh of the environment. This is a platform limitation, not an implementation choice.

2. **Wall & Ceiling Plane Visualization**: While ARCore **does detect** vertical planes (walls) when `planeFindingMode = HORIZONTAL_AND_VERTICAL` is set, **SceneView's built-in PlaneRenderer only visualizes horizontal upward-facing planes** (floors/tables). This is a SceneView library limitation. The app can still place markers on walls via hit-testing when they are detected, but you won't see the visual grid overlay on vertical surfaces. Ceiling detection (downward-facing planes) is also rarely detected by ARCore itself.

3. **SceneView/Filament Crashes**: Navigation between AR and 3D views can cause Filament material crashes if not handled carefully. Current implementation includes workarounds but edge cases may exist.

4. **Memory Usage**: Large numbers of defects with high-resolution images may impact performance. Thumbnails are generated but original images are kept.

5. **Plane Detection Accuracy**: Surface classification (Floor vs Table) uses a simple height threshold (-0.5m). May misclassify in some scenarios.

6. **Image Quality**: Camera images are captured from AR frame, which may have lower resolution than standard camera capture.

7. **3D View Scaling**: Very spread-out defects may appear clustered due to normalization. Very close defects may overlap.

8. **No Persistent AR**: ARCore anchors are session-based. When the app closes, the spatial relationship between defects and the physical environment is lost. Cloud Anchors could address this but are not implemented.
---

