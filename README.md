# PharmaGuard

**PharmaGuard** is a full-stack counterfeit medicine detection platform combining an Android mobile app with a Node.js backend. It uses barcode/GS1 scanning, SHA-256 hash-chain verification, AI-powered medicine extraction (Gemini / OpenAI), OpenFDA enrichment, Firebase-backed scan history, counterfeit reporting with geo-tagging, and a web-based admin portal with a live heatmap and smart-contract batch management.

---

## Features

### Android App
- **JWT Authentication** — Email/password registration & login with secure token-based sessions
- **Google Sign-In** — One-tap Google authentication exchanged for a server-issued JWT
- **Barcode Scanning** — Real-time barcode scanning via CameraX + ML Kit Barcode API
- **GS1 Data Parsing** — Extracts GTIN (`01`), batch/lot (`10`), expiry (`17`), and serial (`21`) from GS1-128 / DataMatrix barcodes
- **SHA-256 Hash-Chain Verification** — Tamper detection by validating cryptographic hash chains against the server ledger
- **Photo-Based Medicine Scan** — Capture medicine packaging photos, run on-device OCR (ML Kit Text Recognition), then extract structured data via Google Gemini generative AI
- **AI-Powered Verification** — Server-side AI proxy supporting both OpenAI (GPT-4o-mini) and Google Gemini for medicine verification and image analysis
- **OpenFDA Lookup** — Enriched medicine detail screen pulling data from the FDA drug database
- **Scan History** — Firebase Realtime Database-backed history with local Room DB offline cache
- **Counterfeit Reporting** — Submit counterfeit reports with GPS location tagging (latitude/longitude)
- **Blockchain Supply-Chain Audit** — Multi-node chain-of-custody timeline (Manufacturer → Distributor → Pharmacy → Consumer)

### Admin Web Portal (`server/public/`)
- **Counterfeit Heatmap** — Leaflet.js geo-tagged map of reported counterfeit incidents
- **Incident Reports Table** — Tabular log of all flagged reports with status tracking
- **Smart Batch Registrar** — Register new medicine batches with SHA-256 genesis block signing
- **Chain of Custody Inspector** — Look up and verify the multi-node audit trail for any barcode or medicine

---

## Tech Stack

### Android App
| Category | Technology |
|---|---|
| Language | Java 17 |
| UI | Android XML layouts, Material Design Components |
| Camera | CameraX (`camera-core`, `camera2`, `lifecycle`, `view`) |
| ML / Vision | ML Kit Barcode Scanning, ML Kit Text Recognition (OCR) |
| AI | Google Generative AI SDK (Gemini `0.9.0`) |
| Networking | OkHttp `4.12.0` |
| JSON | Gson `2.10.1` |
| Images | Glide `4.16.0` |
| Database | Room `2.6.1` (local offline cache) |
| Backend DB | Firebase Realtime Database (BOM `32.7.0`) |
| Auth | Google Play Services Auth `20.7.0` |
| Location | Google Play Services Location `21.1.0` |
| Concurrency | Guava `32.1.3-android` (ListenableFuture) |
| Testing | JUnit 4, Robolectric `4.11.1`, Espresso, AndroidX Test |

### Backend (`server/`)
| Category | Technology |
|---|---|
| Runtime | Node.js 18+ |
| Framework | Express `4.18.2` |
| Auth | `jsonwebtoken` `9.0.2`, `bcryptjs` `2.4.3` |
| Google Auth | `google-auth-library` `9.4.1` |
| Database | Firebase Admin SDK `12.0.0` (Realtime Database) |
| Config | `dotenv` `16.3.1` |
| CORS | `cors` `2.8.5` |
| AI Proxies | OpenAI API (GPT-4o-mini), Google Gemini API (`gemini-2.0-flash`) |
| Admin UI | Vanilla HTML/CSS/JS + Leaflet.js maps |

### Build System
| Tool | Version |
|---|---|
| Gradle Plugin | `7.4.2` |
| Google Services Plugin | `4.4.0` |
| Compile SDK | 34 |
| Min SDK | 21 |
| Target SDK | 34 |
| JDK | 17 |

---

## Repository Structure

```text
PharmaGuard/
├── app/                                          # Android application module
│   ├── build.gradle                              # App-level Gradle config & dependencies
│   ├── google-services.json                      # Firebase config (git-ignored)
│   ├── proguard-rules.pro                        # ProGuard / R8 rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml               # Permissions, activities, app config
│       │   ├── java/com/example/miniprojectapp/
│       │   │   ├── SplashScreen.java             # Launch splash screen
│       │   │   ├── MainActivity.java             # Login screen (email + Google Sign-In)
│       │   │   ├── SignUp.java                   # Registration screen
│       │   │   ├── DashboardActivity.java        # Main dashboard with scan options
│       │   │   ├── BarcodeScanActivity.java      # CameraX barcode scanner + GS1 parsing
│       │   │   ├── PhotoScanActivity.java        # Photo capture → OCR → Gemini extraction
│       │   │   ├── MedicineDetailActivity.java   # Medicine info + verification result
│       │   │   ├── ScanHistoryActivity.java      # List of past scans
│       │   │   ├── AddMedicineActivity.java      # Admin: register medicine batch
│       │   │   ├── ReportCounterfeitActivity.java# Report counterfeit with GPS location
│       │   │   ├── GS1Parser.java                # GS1 Application Identifier parser
│       │   │   ├── HashUtil.java                 # SHA-256 hashing utility
│       │   │   ├── MedicineDatabase.java         # OpenFDA lookup + verification logic
│       │   │   ├── ApiClient.java                # OkHttp REST client wrapper
│       │   │   ├── AuthManager.java              # JWT auth + Google Sign-In manager
│       │   │   ├── SessionManager.java           # SharedPreferences session storage
│       │   │   ├── Medicine.java                 # Medicine data model
│       │   │   ├── MedicineEntity.java           # Room entity for offline cache
│       │   │   ├── MedicineDao.java              # Room DAO interface
│       │   │   ├── AppDatabase.java              # Room database singleton
│       │   │   ├── HistoryRecord.java            # Scan history data model
│       │   │   ├── ScanAdapter.java              # RecyclerView adapter for history
│       │   │   ├── ScanStatusUtil.java           # Scan status helper
│       │   │   └── Users.java                    # User data model
│       │   └── res/
│       │       ├── layout/                       # 11 XML layouts (activities + list items)
│       │       ├── drawable/                     # Icons, backgrounds, shapes
│       │       ├── menu/                         # Dashboard options menu
│       │       ├── values/                       # Colors, strings, themes, styles
│       │       ├── values-night/                 # Dark theme overrides
│       │       ├── color/                        # Color state lists
│       │       └── mipmap-*/                     # Launcher icons (all densities)
│       ├── test/                                 # JVM unit tests (JUnit + Robolectric)
│       └── androidTest/                          # Instrumented tests (Espresso)
│
├── server/                                       # Node.js backend
│   ├── server.js                                 # Express app — all API routes
│   ├── package.json                              # NPM dependencies & scripts
│   ├── .env.example                              # Environment variable template
│   ├── serviceAccountKey.json                    # Firebase Admin credentials (git-ignored)
│   └── public/                                   # Admin web portal (static files)
│       ├── index.html                            # Admin dashboard HTML
│       ├── styles.css                            # Admin portal styles
│       └── app.js                                # Admin portal client-side logic
│
├── build.gradle                                  # Root Gradle config
├── settings.gradle                               # Gradle project settings
├── gradle.properties                             # JVM args, AndroidX flags, JDK path
├── gradlew / gradlew.bat                         # Gradle wrapper scripts
├── local.properties                              # Local SDK path & API keys (git-ignored)
├── .gitignore                                    # Git ignore rules
└── README.md                                     # This file
```

---

## Prerequisites

- **Android Studio** (Arctic Fox or newer) with JDK 17
- **Android device or emulator** (minSdk 21 / Android 5.0+)
- **Node.js** 18+ and npm
- **Firebase project** with Realtime Database enabled
- Google Cloud project with **Generative AI API** enabled (for Gemini features)

---

## Setup

### 1. Firebase Configuration (Android)

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Register an Android app with package name: `com.example.miniprojectapp`.
3. Download `google-services.json` and place it in `app/google-services.json`.
4. Enable **Realtime Database** in the Firebase console.

### 2. Android Local Configuration

Edit the root `local.properties` file:

```properties
# Backend URL injected into BuildConfig.API_BASE_URL
# For emulator:
API_BASE_URL=http://10.0.2.2:3000
# For physical USB device (with adb reverse):
# API_BASE_URL=http://localhost:3000

# AI API keys (used client-side for Gemini photo scan)
OPENAI_API_KEY=your_openai_api_key
GEMINI_API_KEY=your_valid_gemini_api_key
```

> **Note:** `GEMINI_API_KEY` must be a valid key for `generativelanguage.googleapis.com`. An invalid key will cause "Gemini Error" during photo scan.

### 3. Backend Configuration

Navigate to the `server/` directory:

1. **Install dependencies:**
   ```bash
   cd server
   npm install
   ```

2. **Add Firebase Admin credentials:**
   - Place your Firebase Admin SDK service account JSON file as `server/serviceAccountKey.json`.
   - Alternatively, set the `FIREBASE_SERVICE_ACCOUNT` environment variable to the JSON string.

3. **Create `.env`** (copy from `.env.example`):
   ```env
   PORT=3000
   JWT_SECRET=replace-with-a-strong-random-secret
   GOOGLE_CLIENT_ID=your-google-oauth-client-id
   FIREBASE_DATABASE_URL=https://your-project-id-default-rtdb.firebaseio.com

   # Optional: server-side AI proxy keys
   OPENAI_API_KEY=
   GEMINI_API_KEY=
   ```

4. **Start the server:**
   ```bash
   npm start
   ```

5. **Health check:**
   ```
   GET http://localhost:3000/api/health
   → { "status": "ok", "service": "PharmaGuard JWT Server" }
   ```

---

## Running the App

### Android Studio

1. Open the project root in Android Studio.
2. Sync Gradle.
3. Run the `app` module on an emulator or connected device.

### Physical Device via USB

If the backend runs on your development machine:

```bash
adb reverse tcp:3000 tcp:3000
```

Then set `API_BASE_URL=http://localhost:3000` in `local.properties`, rebuild, and reinstall.

### Admin Web Portal

Once the server is running, open in a browser:

```
http://localhost:3000
```

The admin portal provides the heatmap, batch registrar, and chain-of-custody inspector.

---

## Backend API Reference

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user (email, password, name, phone) |
| `POST` | `/api/auth/login` | Login with email & password, returns JWT |
| `POST` | `/api/auth/google` | Exchange Google ID token for a JWT |
| `GET` | `/api/auth/me` | Get authenticated user profile (requires Bearer token) |

### AI Proxy
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/ai/verify` | AI-powered medicine name verification (OpenAI / Gemini) |
| `POST` | `/api/ai/analyze-image` | AI vision analysis of medicine packaging image (base64) |

### Blockchain & Supply Chain
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/blockchain/verify-chain` | Verify multi-node supply chain audit trail |
| `GET` | `/api/admin/batches` | List all registered medicine batches |
| `POST` | `/api/admin/batches` | Register a new medicine batch with SHA-256 genesis hash |

### Reporting
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/reports/heatmap` | Get geo-tagged counterfeit incident report data |

### System
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health` | Server health check |

---

## App Screens

| Screen | Activity | Description |
|--------|----------|-------------|
| Splash | `SplashScreen` | Animated launch screen |
| Login | `MainActivity` | Email/password login + Google Sign-In |
| Sign Up | `SignUp` | New user registration |
| Dashboard | `DashboardActivity` | Main hub — scan, history, report options |
| Barcode Scan | `BarcodeScanActivity` | CameraX live barcode scanner with GS1 parsing |
| Photo Scan | `PhotoScanActivity` | Camera capture → OCR → Gemini AI extraction |
| Medicine Detail | `MedicineDetailActivity` | Full medicine info + verification status |
| Scan History | `ScanHistoryActivity` | RecyclerView list of past scans |
| Add Medicine | `AddMedicineActivity` | Admin batch registration form |
| Report Counterfeit | `ReportCounterfeitActivity` | Submit counterfeit report with GPS coordinates |

---

## Testing

### Unit Tests (JVM)

```bash
./gradlew testDebugUnitTest
```

### Instrumented Tests (Device/Emulator)

```bash
./gradlew connectedAndroidTest
```

### Build Debug APK

```bash
./gradlew assembleDebug
```

### Install on Connected Device

```bash
./gradlew installDebug
```

---

## Security Notes

- `android:usesCleartextTraffic="true"` is enabled in the manifest for local HTTP development. **Disable this for production.**
- `serviceAccountKey.json`, `google-services.json`, `.env`, and `local.properties` are git-ignored. **Never commit real credentials.**
- The default `JWT_SECRET` in code is a placeholder — **rotate all secrets before production use.**
- The blockchain verification is a **simulation** for demonstration purposes and does not connect to an actual blockchain network.

---

## License

Educational / research project.
