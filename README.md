# Mutatum — Modular Android System

A multi-module Android application built using a **single-activity + fragment-based architecture**, designed as a scalable system of independent “apps” within one container.

This project focuses on **real-world system design**, not just feature implementation — combining networking, media handling, hardware interaction, and secure storage into a unified structure.

---

##  Architecture

- **Single Activity (MainActivity)** → Acts as navigation controller
- **Fragment-based modules** → Each feature behaves like an independent app
- **Polylith-style structure** → Modules are loosely coupled but share a core shell

### Core Principles
- Separation of features into self-contained fragments  
- Centralized navigation via `loadFragment()`  
- Background threading for all heavy operations  
- Fail-safe and fallback-first design  

---

##  Theming System

- Custom **Day/Night engine** using `values/` + `values-night/`
- Removed default Material theme colors (no purple nonsense)
- Built a **dark-first UI** with:
  - Deep black / red base
  - High-contrast red accent (`@color/primary_accent`)
- Native theme switching without runtime color hacks  

---

##  Modules

### 1. Currency Converter 

- Live currency conversion using **Frankfurter API**
- Background execution via `ExecutorService`
- UI updates through `Handler (Main Thread)`
- **Offline fallback system** using hardcoded exchange rates

**Features**
- Smart currency swap logic  
- Styled spinner UI  
- Graceful failure handling  

---

### 2. Media Player

Dual-mode media system handling both local and streamed content.

**Audio**
- File picker integration  
- Album art extraction via metadata  
- MediaPlayer-based playback  

**Video**
- Stream playback via `VideoView`  
- Dynamic UI switching between modes  

**Controls**
- Unified transport system (play/pause, seek, skip)  
- Real-time progress tracking with Handler loop  

---

### 3. Hardware Telemetry

Real-time visualization of device sensors.

**Sensors Used**
- Accelerometer  
- Ambient Light  
- Proximity  

**Highlights**
- Low-pass filter smoothing for motion stability  
- Lux → semantic mapping (DARK / DIM / NORMAL / BRIGHT)  
- Proximity-based alert UI with animations  

---

### 4. Gallery + Camera 

A full storage pipeline using **modern Android SAF (Storage Access Framework)**.

**Storage**
- Folder mounting via `ACTION_OPEN_DOCUMENT_TREE`  
- Persistent URI permissions (`takePersistableUriPermission`)  
- Cached via SharedPreferences  

**Camera**
- Built with **CameraX**
- Lifecycle-aware binding (`ProcessCameraProvider`)  
- Writes directly to SAF using `DocumentFile + OutputStream`

**Gallery**
- Background file scanning (ExecutorService)  
- Dual validation (MIME + extension) for reliability  
- RecyclerView grid with Glide image loading  

**Image Details**
- Metadata display (size, date, path)  
- Safe deletion with confirmation dialog  

---

##  Tech Stack

- Java (Android SDK)
- AndroidX + Material Components  
- CameraX  
- Glide (image loading)  
- Native APIs:
  - `HttpURLConnection`
  - `SensorManager`
  - `MediaPlayer`
  - `DocumentFile`

---

##  Permissions

- `INTERNET` → API + streaming  
- `CAMERA` → CameraX capture  
- No legacy storage permissions (SAF-based system)

---

##  Key Engineering Decisions

- Avoided heavy frameworks → relied on native Android APIs  
- Built fallback systems for unstable dependencies (network/storage)  
- Used background threading consistently to protect UI performance  
- Prioritized **control over convenience** (manual implementations over abstractions)

---

##  Current State

- Navigation handled manually 
- Large fragments handle multiple responsibilities  
- No caching layer for network/media  
- Hardcoded configurations 

---

## Project Goal

This project is not just a collection of features — it is an attempt to build a **modular Android system** where each component behaves like an independent application while sharing a unified core.

---

## Creator 

Blaze

---
