# Hearthplan — Home Design & Floor Planning Android App

Hearthplan is an architectural floor planning application for Android phones and tablets, built with Kotlin and Jetpack Compose. It allows users to start from an empty canvas or preview built-in starter templates, define plot boundaries across multi-level floors, position and resize rooms and doors/windows with synchronized touch drag handles and numerical dimension inputs, arrange top-down 2D architectural vector furniture, export genuine multi-page vector PDF planning sheets, and manage projects completely offline with on-device Room SQLite storage.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The implementation plan incorporates your final technical corrections:

- **No Embedded Private Keys & Honest AI Status**: In accordance with Android security best practices, private API keys will **never** be hardcoded or packaged into the client APK or BuildConfig. The "Generate Custom Furniture" dialog clearly communicates the setup status: explaining that AI generation requires an external secure backend proxy, while keeping the complete standard architectural furniture catalog 100% operational offline.
- **Core App Prioritization & Build Verification**: Execution will immediately implement and verify the core app: blank-project creation, accurate 2D canvas geometry, synchronized drag handles and numerical dimension inputs, multi-floor tab switching, architectural vector furniture, zoom, pan, undo/redo, auto-saving to local Room database, JSON import/export, and multi-page PDF generation.
- **Genuine Offline-First Architecture**: Completely independent of cloud dependencies or subscriptions. Projects are saved locally in Room SQLite, surviving restarts and reboots.
- **Planning Diagram Disclaimer**: Exported PDFs and canvas headers explicitly state that generated documents are planning diagrams for spatial visualization, not certified architectural or structural construction drawings.
- **Permanent Web Deployment Link**: A clearly visible, clickable link to `https://hearthplan-f0cywo.v2.appdeploy.ai/` anchored at the bottom of the welcome screen and settings view on both phone and tablet layouts.

---

## 1. Overview & Core Concept

- **What It Does**: Hearthplan delivers a clean, independent 2D floor-planning tool designed for mobile and tablet touchscreens. Users start with an empty plot footprint (specifying length, breadth, unit, and floors) or browse built-in starter templates. They draw rooms, adjust wall dimensions in meters or feet, position doors and windows, arrange architectural furniture symbols, and export printable PDF plans.
- **Target Audience**: Homeowners sketching renovations, DIY designers, architectural students, realtors, and interior planners.
- **Key Value**: Simple, responsive, and completely private on-device floor planning with zero mandatory cloud dependencies, no paywalls, and reliable PDF generation.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Welcome Screen**:
   - Clean, uncluttered layout with app brand icon and title.
   - Primary action buttons:
     - **Create a Blank Project**: Opens the plot specification modal.
     - **Starter Templates**: Opens the built-in template gallery (clearly labeled as on-device starter templates).
     - **Open / Import Project**: Pick and load a saved project or import a `.json` / `.hearthplan` file.
   - **Footer**: Clickable card with external link icon opening the live web deployment at `https://hearthplan-f0cywo.v2.appdeploy.ai/`.
2. **Blank Project Setup Sheet**:
   - Form inputs: Project Name, Plot Length, Plot Breadth, Measurement Unit (Meters `m` vs Feet `ft`), Initial Number of Floors (1 to 5).
   - Real-time dimension validation with clear explanation: *"Plot dimensions establish your ground footprint. Individual floor layouts and rooms are drafted inside."*
   - Tapping "Create Project" initializes an empty canvas containing only the dashed plot boundary, scale indicators, and optional grid—no preloaded rooms or furniture.
3. **Interactive 2D Workspace (Editor)**:
   - **Top Navigation Bar**: Editable Project Name, Undo / Redo buttons, Zoom In / Zoom Out / Reset View, Unit switcher (`m`/`ft`), Save status badge ("Saved on device"), and Export button.
   - **Floor Navigation Tabs (Chrome-Style)**:
     - Horizontal tabs: `Ground Floor | First Floor | Second Floor | +`.
     - Active floor highlighted; tabs allow renaming, duplicating, and deleting with confirmation.
     - Optional "Lower Floor Reference" toggle that renders a translucent ghost outline of the floor beneath to align plumbing stacks and exterior walls.
   - **Interactive Drawing Canvas**:
     - Metric / Imperial grid with adjustable snap-to-grid toggle.
     - Two-finger pan and pinch-to-zoom.
     - Single-finger tap to select room or furniture.
     - **Selection Bounding Box**: 8 draggable handles (4 corners + 4 edge midpoints) for intuitive resizing, plus a top rotation handle for furniture.
     - **Live Dimension Toolbar**: When a room is selected, an editable HUD displays Length and Breadth. Typing a value updates the canvas immediately; dragging a handle updates the numerical fields simultaneously.
     - Visual boundary checks alerting if a room extends outside the plot footprint.
   - **Floating Action Button (`+`) (Keep-Style)**:
     - Fixed in the bottom-right corner.
     - Expands into three clear actions: **Add Floor**, **Add Room**, **Add Furniture**.
4. **Room Creation & Geometry**:
   - Room catalog: Bedroom, Living Room, Kitchen, Bathroom, Dining Room, Study, Balcony, Corridor, Custom Room.
   - Supports attaching doors (with swing arc preview) and windows along wall segments.
5. **Furniture Catalog & AI Custom Furniture**:
   - Categorized library (Beds, Sofas & Armchairs, Dining & Desks, Storage & Wardrobes, Kitchen Counters, Sanitary Fixtures).
   - Search bar and category filters.
   - Top-down 2D architectural vector representations (crisp lines, room-proportional).
   - **Generate Custom Furniture Action**:
     - Displays prompt input and dimensions.
     - Honest security & setup message: Explains that direct client-side API keys are withheld to protect credentials, requiring a secure backend proxy server for generation. Allows custom parametric dimension input while keeping standard furniture instantly accessible.
6. **Export & Sharing Modal**:
   - **Download PDF**: Generates a real multi-page vector PDF (one floor per page) via Android `android.graphics.pdf.PdfDocument`. Includes project title, floor name, scale bar, room schedule, and standard planning diagram disclaimer (*"This document is a floor planning diagram for conceptual visualization and is not a certified structural engineering drawing"*).
   - **Export JSON**: Saves the full project data structure to device storage or shares via Android Share Sheet.
   - **Import JSON**: Loads an exported project file into the local Room database.
   - **Save as Template**: Copies the current project into the user's local template collection.

### Visual Identity & Theme

- **Aesthetic**: Modern architectural minimalism (blueprint precision meets clean contemporary software design).
- **Colors**:
  - Canvas Paper: `#FBFBFA` (soft blueprint off-white)
  - Grid: `#E5E7EB` with `#D1D5DB` major metric/imperial increments
  - Plot Boundary: `#64748B` (dashed perimeter line)
  - Architectural Walls: `#0F172A` (solid dark slate)
  - Room Tints: Delicate architectural washes (Living: `#F8FAFC`, Bed: `#EFF6FF`, Kitchen: `#FFFBEB`, Bath: `#F0FDF4`, Study: `#FAF5FF`)
  - Handles & Selection: `#2563EB` (vibrant architectural blue with 12dp touch targets)
  - Floating Action Button: `#0F172A` with `#FFFFFF` glyph
- **Responsive Layout**:
  - Phones: Compact top bar, scrollable floor tabs, full-screen canvas with bottom-sheet drawers.
  - Tablets: Split supporting drawer for furniture library and dimension inspector, wider tab bar, generous multi-touch workspace.

---

## 3. Key Product Decisions & Trade-Offs

- **Native Jetpack Compose Canvas Over WebView**:
  - *Chosen Approach*: Implement custom Compose Canvas rendering with matrix transformations (`scale`, `translationX`, `translationY`) and pointer input gestures (`detectTransformGestures`, `detectDragGestures`, `detectTapGestures`).
  - *Why*: Eliminates webview lag, supports smooth 60fps pinch-to-zoom, precise touch target detection, and direct state binding with ViewModel.
- **Room SQLite Persistence + JSON Serialization**:
  - *Chosen Approach*: Local SQLite database via Android Room, storing projects, floor structures, and furniture items with kotlinx.serialization converters.
  - *Why*: 100% offline, zero latency, resilient to app force-quits or reboots, and enables simple single-file JSON backup/restore.
- **Android Native `PdfDocument` Generation**:
  - *Chosen Approach*: Direct vector canvas rendering into `android.graphics.pdf.PdfDocument`.
  - *Why*: Completely self-contained, no external internet services required, prints crisp at any paper size (A4 / Letter), and exports instantly.
- **Architectural Vector Top-Down Geometry for All Furniture**:
  - *Chosen Approach*: Standard and custom furniture render as top-down 2D architectural vectors with accurate outlines and subtle internal divisions.
  - *Why*: Maintains consistent blueprint aesthetics, stays sharp at high zoom levels, and eliminates low-resolution bitmap artifacts.
- **Security-First AI Architecture**:
  - *Chosen Approach*: Never embed private API keys in client code or APKs. Display honest configuration status in the custom furniture dialog, ensuring core planning tools are immediately usable.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Hearthplan Architecture                         │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │                         Presentation                           │   │
│   │    WelcomeScreen  ◄──────►  EditorScreen  ◄──────► Templates   │   │
│   └───────────────────────────────┬────────────────────────────────┘   │
│                                   │                                    │
│   ┌───────────────────────────────┴────────────────────────────────┐   │
│   │                     HearthplanViewModel                        │   │
│   │  - project: StateFlow<Project>                                 │   │
│   │  - activeFloorIndex: Int                                       │   │
│   │  - selectedElement: CanvasElement?                             │   │
│   │  - undoStack / redoStack: List<ProjectSnapshot>                │   │
│   │  - zoomState: (scale: Float, offset: Offset)                   │   │
│   │  - saveStatus: StateFlow<SaveStatus>                           │   │
│   └───────┬───────────────────────┬────────────────────────────────┘   │
│           │                       │                                    │
│   ┌───────▼─────────────┐ ┌───────▼───────────────┐                    │
│   │ FloorPlanCanvas     │ │ HearthplanRepository  │                    │
│   │ - Multi-touch pan   │ │ - Room AppDatabase    │                    │
│   │ - Pinch-to-zoom     │ │ - ProjectDao          │                    │
│   │ - Drag handles (8)  │ │ - BuiltinTemplates    │                    │
│   │ - Snapping math     │ │ - JSON Importer/Export│                    │
│   │ - Vector furniture  │ │ - Native PdfExporter  │                    │
│   └─────────────────────┘ └───────────────────────┘                    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Step-by-Step Execution Plan

1. **Project Identity & Manifest Setup**:
   - Update `app_name` in `res/values/strings.xml` to `Hearthplan` and sync `metadata.json`.
   - Update `applicationId` to a unique ID in `app/build.gradle.kts`.
   - Add Internet and file sharing configs in `AndroidManifest.xml`.
2. **Data Persistence & Local Database**:
   - Create Room database (`HearthplanDatabase`), `ProjectDao`, and `ProjectRepository`.
   - Add built-in starter templates (Modern Villa, 2-BHK Family Apartment, Studio Flat) pre-seeded into local storage.
   - Implement JSON export and import for portable backup files.
3. **Interactive 2D Canvas Engine**:
   - Implement `FloorPlanCanvas` with coordinate conversion between real-world dimensions (meters/feet) and screen viewport pixels.
   - Implement touch gestures: two-finger pinch-to-zoom, two-finger pan, single-finger drag for selection and repositioning.
   - Implement 8-point resize handles (4 corners + 4 edges) and rotation knob.
   - Synchronize live drag handles with the floating numerical dimension toolbar (typing length/breadth updates immediately; dragging handles updates inputs).
   - Render grid, plot footprint, wall outlines, room labels with area calculation (`m²` / `sq ft`), door swing arcs, and window openings.
   - Render top-down architectural vector graphics for furniture (beds, sofas, dining tables, desks, sanitary fixtures, kitchen counters).
4. **Editor Shell & Navigation**:
   - Header bar with project title editing, undo/redo stack, zoom controls, unit toggle, save indicator, and export dialog trigger.
   - Chrome-style horizontal floor tabs (`Ground Floor | First Floor | +`) with rename/duplicate/delete actions.
   - Lower-floor ghost reference overlay for upper levels.
   - Floating `+` action button (Google Keep style) expanding into: Add Floor, Add Room, Add Furniture.
5. **Room & Furniture Libraries**:
   - Quick Room creation dialog with standard types and custom dimension sliders.
   - Searchable, categorized furniture drawer with instant placement.
   - Honest "Generate Custom Furniture" dialog explaining backend configuration requirement while allowing custom dimensioned parametric furniture insertion.
6. **Vector PDF Export Engine**:
   - Multi-page PDF generator using native Android `android.graphics.pdf.PdfDocument`.
   - Page layout: Title block, floor name, scale, room list, clean 2D vector drawing, and non-certified planning diagram disclaimer.
   - Open PDF or share via Android Intent.
7. **UI Polish & Web Version Link**:
   - Add permanent clickable link to `https://hearthplan-f0cywo.v2.appdeploy.ai/` on the welcome screen and settings dialog.
   - Polish tablet and phone responsiveness.
8. **Compilation & Verification**:
   - Run `compile_applet` and verify zero compilation errors.
   - Verify room dragging, dimension synchronization, undo/redo, Room autosave, JSON import/export, and PDF generation.
