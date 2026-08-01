# 🛡️ PharmaGuard: AI & Blockchain-Powered Counterfeit Medicine Detection System

PharmaGuard is a comprehensive Android application and backend authentication server designed to verify medicine authenticity, combat counterfeit pharmaceuticals, and inform consumers about drug safety.

By combining **Blockchain Verification (SHA-256 Hash Chaining)**, **AI-powered Vision & OCR (OpenAI GPT-4o / Google Gemini 2.5 Flash)**, **ML Kit Barcode Scanning**, and real-time open database integration (**OpenFDA** & **Firebase Realtime Database**), PharmaGuard offers a multi-layered defense against counterfeit drugs.

---

## 📸 Key Features

- 🔍 **Real-Time Barcode & QR Scanner**: Uses Google ML Kit to scan barcodes/QR codes on packaging and verify medicine records against the Firebase Realtime Database.
- ⛓️ **Blockchain-Resonant Verification**: Validates medicine batch numbers, previous block hashes, and current SHA-256 hashes (`HashUtil.sha256`) to ensure records haven't been tampered with in the supply chain.
- 📷 **AI Photo Recognition & OCR**: Captures photos of medicine packages, extracts text via ML Kit OCR, and uses Google Gemini / OpenAI Vision models to correct OCR errors and identify medicine details.
- 🏛️ **OpenFDA API Integration**: Automatically queries the U.S. FDA drug database for official brand names, generic names, dosages, compositions, and adverse reaction disclosures.
- 📊 **Interactive User Dashboard**: Tracks verification stats (verified scans, total scan history, reported counterfeits) and provides quick navigation to all app functions.
- 🚩 **Counterfeit Reporting System**: Allows users to flag suspicious medicines, submit notes, and contribute to public health awareness.
- 🔒 **Secure JWT Backend Server**: Node.js & Express authentication server supporting email/password registration, password hashing with `bcryptjs`, and Google OAuth 2.0.

---

## 🛠️ Architecture & Tech Stack

### Mobile App (Android)
- **Language**: Java
- **UI & Layouts**: Android XML Layouts, Material Design Components
- **Camera & Vision**: CameraX, ML Kit Barcode Scanning, ML Kit Text Recognition
- **Database & Auth**: Firebase Realtime Database, Firebase Admin SDK
- **AI Integrations**: Google Generative AI Android SDK (`gemini-2.5-flash`), OpenAI API (`gpt-4o-mini`) via OkHttp3
- **HTTP Client**: OkHttp3, Retrofit / HttpURLConnection

### Backend Server (Node.js)
- **Runtime**: Node.js, Express.js
- **Authentication**: JSON Web Tokens (`jsonwebtoken`), `bcryptjs`, Google Auth Library (`google-auth-library`)
- **Database**: Firebase Admin SDK (`firebase-admin`)

---

## 📁 Repository Structure

```
PharmaGuard/
├── app/                              # Android Application Module
│   ├── src/main/java/com/example/miniprojectapp/
│   │   ├── AddMedicineActivity.java   # Add new medicine records to database
│   │   ├── ApiClient.java             # Shared OkHttp client wrapper
│   │   ├── AuthManager.java           # Authentication helper functions
│   │   ├── BarcodeScanActivity.java   # CameraX + ML Kit Barcode scanning & blockchain verification
│   │   ├── DashboardActivity.java     # User dashboard and activity counters
│   │   ├── HashUtil.java              # SHA-256 cryptographic hashing utility
│   │   ├── HistoryRecord.java         # Data model for user scan history
│   │   ├── MainActivity.java          # Login activity
│   │   ├── Medicine.java              # Core Medicine data model & hash properties
│   │   ├── MedicineDatabase.java      # Firebase DB, OpenFDA API & AI integration handler
│   │   ├── MedicineDetailActivity.java# Detailed view of scanned medicine
│   │   ├── PhotoScanActivity.java     # Camera photo capture + OCR + Gemini AI analysis
│   │   ├── ReportCounterfeitActivity.java # Submit counterfeit medicine alerts
│   │   ├── ScanAdapter.java           # RecyclerView adapter for scan history
│   │   ├── ScanHistoryActivity.java   # User scan history listing
│   │   ├── SessionManager.java        # SharedPreferences session manager
│   │   ├── SignUp.java                # Registration activity
│   │   ├── SplashScreen.java          # App launch splash screen
│   │   └── Users.java                 # User data model
│   └── src/main/res/                  # Layouts, drawables, menus, and themes
│
├── server/                            # Node.js JWT Authentication Server
│   ├── .env.example                   # Environment variable template
│   ├── package.json                   # Server dependencies
│   └── server.js                      # Express server routes & Firebase Admin setup
│
├── build.gradle                       # Top-level build configuration
├── settings.gradle                    # Gradle settings
└── README.md                          # Project documentation
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** (Jellyfish / Koala or newer recommended)
- **JDK 17** or higher
- **Android Device / Emulator** with API Level 24+ (Android 7.0+)
- **Node.js** v18+ (for running the authentication backend server)

---

## 🔧 Setup & Configuration

### 1. Firebase Setup (Android App)
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Register an Android App with package name `com.example.miniprojectapp`.
3. Download `google-services.json` and place it into the `app/` folder:
   ```
   app/google-services.json
   ```
4. Enable **Realtime Database** in test or production mode.

### 2. API Keys Configuration
Add your API keys to `local.properties` (which is gitignored) at the root of the project:
```properties
API_BASE_URL=http://10.0.2.2:3000
OPENAI_API_KEY=your_openai_api_key_here
GEMINI_API_KEY=your_gemini_api_key_here
```
Keys and the backend base URL will automatically be injected into Gradle `BuildConfig`.

### 3. Backend Server Setup (Node.js)
1. Navigate to the `server/` directory:
   ```bash
   cd server
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Generate a Firebase Service Account Key from Firebase Console (`Project Settings > Service accounts`), download the JSON file, and save it as:
   ```
   server/serviceAccountKey.json
   ```
4. Create a `.env` file based on `.env.example`:
   ```env
   PORT=3000
   JWT_SECRET=your-custom-jwt-secret-key
   GOOGLE_CLIENT_ID=your-google-client-id
   FIREBASE_DATABASE_URL=https://your-firebase-db.firebaseio.com
   ```
5. Start the server:
   ```bash
   npm start
   ```

---

## 📲 Building & Running the App

1. Open the project directory in **Android Studio**.
2. Sync the project with Gradle files (`File > Sync Project with Gradle Files`).
3. Connect an Android device or launch an emulator.
4. Click **Run** (`Shift + F10`).

---

## 📜 License

This project is created for educational and research purposes as part of an anti-counterfeit pharmaceutical verification initiative.
