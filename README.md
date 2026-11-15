# SnagOps — Offline-First Multi-User Construction Quality Suite

**SnagOps** is a complete, production-grade quality management suite for construction engineering teams. It features:
1. **Offline-First Android App:** Kotlin, Jetpack Compose, Material 3, Room SQLite (v3 schema), WorkManager, CameraX/FileProvider, native `PdfDocument` A4 NCR generator, Apache POI `.xlsx` generator.
2. **Offline-First iOS & Web PWA (`web-pwa/`):** React 18, TypeScript, Vite, Tailwind CSS, Dexie.js (IndexedDB), HTML5 Canvas image compressor, jsPDF A4 NCR generator, ExcelJS 3-tab `.xlsx` generator, iOS Safari Home Screen optimization.
3. **Multi-User Collaboration & Collision-Free IDs:** Device engineer profiles (`FullName`, `Initials`), dual-layer ID system (`Record UUID` primary key + human-readable sequential display IDs `OBS-<Initials>-001` / `NCR-<Initials>-001`).
4. **Two-Way Google Sheets Master Data Sync:** Project-scoped synchronization for master hierarchy tabs (`Master_Config`, `Master_Blocks`, `Master_Floors`, `Master_Presets`, `Master_Vendors`, `Master_CustomFields`) and observation ledger deduplication via `Record UUID`.
5. **Google Apps Script Companion Backend (`backend/Code.gs`):** Single-threaded vendor emails, 10-minute automated NCR PDF dispatch triggers, scheduled batch reports, and RESTful `doGet`/`doPost` endpoints.
6. **Automated CI/CD:** GitHub Actions workflow (`.github/workflows/deploy-pwa.yml`) automatically builds and deploys the PWA to GitHub Pages on push to `main`.

---

## 🏗 Architecture & Key Highlights

### 1. 100% Offline Capability (Android & iOS PWA)
- **Local Persistence:** All project setups, location hierarchies, vendor directories, custom fields, observations, and NCRs are persisted in SQLite (Room) on Android and IndexedDB (Dexie) on iOS/Web.
- **Client-Side Image Compression:** Every captured photo is compressed locally to a max dimension of 1024px longest edge (JPEG 75 quality, ~150 KB) with rotation correction before saving.
- **Direct Camera & Gallery Pickers:** Native camera capture (`ActivityResultContracts.TakePicture()` with `FileProvider` on Android, `<input capture="environment">` on iOS) side-by-side with photo library picker.
- **Offline Sync Queue:** Persistent `SyncOutbox` queue stores pending records, photos, and PDFs. Sync occurs automatically when network connectivity is restored (`WorkManager` on Android, online event listeners in PWA) or via manual **Sync Now** triggers.

### 2. Multi-User Team Collaboration & Collision-Free IDs
- **Engineer Profile:** Configured on first launch and editable in Settings. Stores Engineer Full Name and 2–4 letter Initials (e.g., `RK`, `AS`).
- **Two-Layer Collision-Free ID Architecture:**
  1. **Primary Sync Key (`record_uuid`):** Globally unique UUIDv4 generated locally on device for every observation, NCR, and master data entity. Records logged simultaneously offline by multiple team members never collide.
  2. **Display ID:** Formatted with the engineer's initials and sequence—e.g., `OBS-RK-001` for Normal Observations and `NCR-AS-001` for NCRs.
- **Logged By & Digitally Signed:** Author initials and name are tracked across all database entities, Google Sheets columns, Excel exports, and formal A4 NCR PDF sign-off boxes (`Issued By: <Initials>`).

### 3. Multi-Project Management & Instant Onboarding
- **Active Project Switcher:** Top app bar dropdown/pill enables engineers to switch between active projects in one tap. All observations, NCRs, master data, and Google Sheets sync targets filter instantaneously.
- **Share / Join Project Config Snippets:** Export or import compact JSON snippets containing project code, name, client name, and Google Spreadsheet ID so team members can onboard onto a project in seconds without manual entry.

### 4. Dynamic Location Hierarchy & Responsive Wrapping Chips
- Multi-tier cascading hierarchy:
  - **Blocks / Zones:** Tower A, Tower B, Podium & Amenities, External / Civil Works, etc.
  - **Floors / Levels:** Basement 1, Ground Floor Lobby, 1st Floor, 2nd Floor, etc.
  - **Presets / Sub-locations:** Preset chips alongside free-text input on site.
- **Wrapping Chips (`FlowRow`):** Location selector chips wrap cleanly onto multiple lines in portrait orientation with zero clipping or awkward horizontal scrollbars.

### 5. Local A4 NCR PDF Generator
- Instantly compiles a formal, print-ready A4 Non-Conformance Report (`NCR_<Number>.pdf`) containing:
  - Project metadata, Project Code, and Client/Main Contractor banner
  - Defect description, Root Cause Analysis, Corrective Action Required (CAPA)
  - Embedded defect photo and rectification photo
  - Formal sign-off and verification boxes with engineer initials
  - Footer with document sync UUID

### 6. Multi-Tab `.xlsx` Export with Executive Management Dashboard
Generates standalone 3-tab `.xlsx` workbooks locally:
1. **`Management_Dashboard` (Tab 1):** Executive KPI cards (Total Observations, Open Snags, Rectified, Closure Rate %) powered by live Excel formulas, Priority breakdown, and Record Type breakdown.
2. **`Observations` (Tab 2):** Tabular register for Normal Observations including Entry ID, Record UUID, Date Logged, Logged By, Block, Floor, Specific Location, Vendor, Trade, Priority, Target Date, Status, Rectified Date, Issue Description, and dynamic custom fields.
3. **`NCR_Register` (Tab 3):** Dedicated Non-Conformance Register with Root Cause, Corrective Action, and photo evidence references.

### 7. Google Apps Script Companion Backend (`backend/Code.gs`)
- **Master Data Synchronization:** Auto-creates and syncs `Master_Config`, `Master_Blocks`, `Master_Floors`, `Master_Presets`, `Master_Vendors`, and `Master_CustomFields`.
- **Threaded Vendor Communications:** Manages persistent Gmail thread IDs per vendor for notifications, instant NCR PDF attachments, and weekly/daily batch summary reports.
- **RESTful Sync Endpoints:** `doGet` and `doPost` handlers with `Record UUID` deduplication and upsert.

---

## 🧪 Testing & Verification

### Android Unit Tests
```bash
./gradlew testDebugUnitTest
```
All 13 unit tests pass across 4 test suites:
- `CustomFieldSerializationTest`: Dynamic custom field JSON serialization and type conversion.
- `NcrRoutingAndPdfTest`: Sequential ID generation (`OBS-RK-001`, `NCR-RK-001`) and PDF sanitization.
- `ExcelWorkbookGenerationTest`: Apache POI multi-tab workbook generation and formula integrity.
- `MultiProjectScopingTest`: Project isolation and foreign-key scoping verification.

### Android Debug APK Build
```bash
./gradlew assembleDebug
```
Output:
```
app/build/outputs/apk/debug/app-debug.apk
```

### iOS / Web PWA Build
```bash
cd web-pwa
npm install
npm run build
```
Output:
```
web-pwa/dist/
├── index.html
├── manifest.webmanifest
├── registerSW.js
├── sw.js
└── assets/
```

---

## 🚀 GitHub Actions Deployment (PWA)

Pushing changes to `main` triggers `.github/workflows/deploy-pwa.yml`, which automatically builds `web-pwa` and deploys it to GitHub Pages.
To access the PWA on iOS:
1. Open the GitHub Pages URL in Safari on your iPhone/iPad.
2. Tap the **Share** button (box with upward arrow).
3. Tap **Add to Home Screen**.
4. The SnagOps icon will appear on your Home Screen and run full-screen offline.
