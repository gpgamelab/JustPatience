# JustPatience

An Android game written in **Kotlin** that lets you play classic solitaire (patience) on your mobile device.

---

## ? Features

- Classic solitaire gameplay
- Smooth card animations
- Touch-friendly drag & drop controls
- Automatic move validation
- Game state persistence
- Clean, modern Android UI

---

## ? Tech Stack

- **Language:** Kotlin
- **Platform:** Android
- **Architecture:** (e.g., MVVM / MVC / Clean Architecture -- describe yours)
- **Minimum SDK:** (e.g., 24)
- **Target SDK:** (e.g., 34)

---

## ? Roadmap / TODO (Prioritized)

<details> <summary><strong>PHASE 0: CORE DEVELOPER SUPPORT</strong></summary> <br>
- [ ] **Strengthen Move Validation Engine**
  - [ ] Add comprehensive unit tests for move validation logic </details>
<details> <summary><strong>PHASE 1: CORE UX FOUNDATION </strong></summary> <br>
- [X] **Home / Landing Page**
  - [X] Design non-game landing screen
  - [X] Implement navigation entry points
- [ ] **Sound Effects Framework**
  - [ ] Implement audio system foundation
  - [ ] Add core sound effects
- [ ] **Card Animations**
  - [ ] Implement card flip animations
  - [ ] Implement card slide animations
  - [ ] Add animation polish and refinement
- [ ] **Loading Experience**
  - [ ] Design loading screen
  - [ ] Implement loading state transitions
- [X] **Settings Page**
  - [X] Complete settings UI
  - [X] Connect settings to functional toggles </details>
<details> <summary><strong>PHASE 2: ENGAGEMENT </strong></summary> <br>
- [ ] **First-Time User Experience**
  - [ ] Design onboarding flow
  - [ ] Implement interactive tutorial
- [ ] **Win Celebration**
  - [ ] Implement win celebration animation
  - [ ] Add animation polish and effects
- [ ] **Home Screen Improvements**
  - [• ] Move stats button to home page
  - [• ] Add shopping button (stub)
- [ ] **Account System**
  - [ ] Implement account creation
  - [ ] Implement account login
  - [ ] Connect authentication to backend server
- [ ] **Appearance Options**
  - [ ] Add dark mode toggle </details>
<details> <summary><strong>PHASE 3: MONETIZATION PREP </strong></summary> <br>
- [X] **Ad Framework Integration**
  - [X] Integrate Google Mobile Ads SDK
  - [X] Implement ribbon/banner ads
  - [X] Implement full-screen/interstitial ads
- [ ] **In-App Purchases (IAP)**
  - [ ] Build basic shopping page
  - [ ] Integrate in-app purchase monetization
- [ ] **Premium Feature System**
  - [ ] Implement premium feature toggles
  - [•] Gate premium features behind ads/IAP </details>
<details> <summary><strong>PHASE 4: RETENTION FEATURES </strong></summary> <br>
- [ ] **Achievement System**
  - [ ] Design simple achievement framework
  - [ ] Implement achievement tracking
- [ ] **Daily Challenges**
  - [ ] Design daily challenge logic
  - [ ] Implement daily challenge UI
- [ ] **Performance Metrics**
  - [ ] Design stats/performance display
  - [ ] Implement metrics tracking
- [ ] **Themes & Customization**
  - [ ] Add alternative face card themes
  - [ ] Add alternative back card themes
  - [ ] Add alternative tabletop themes
  - [ ] Implement theme selection system </details>

---

## ? Getting Started

### Prerequisites

- Android Studio (latest stable version recommended)
- Android SDK installed
- Kotlin plugin (usually bundled with Android Studio)

### Build Instructions

1. Clone the repository:

   ```bash
   git clone git@github.com:gpgamelab/JustPatience.git
   ```

2. Open the project in Android Studio.
3. Let Gradle sync.
4. Click **Run** to launch on an emulator or device.

---

## ? Gameplay Rules

This game implements the classic solitaire rules:

- Build tableau columns in descending order alternating colors.
- Move cards to foundation piles by suit in ascending order.
- Win by moving all cards to the foundations.

(Expand this section if you implement specific solitaire variants.)

---

## ? Testing

Describe how the game logic is tested.

Example:
- Unit tests for card movement rules
- Integration tests for game state restoration
- Manual UI testing on multiple screen sizes

Run tests with:

```bash
./gradlew test
```

---

## ? Project Structure

Explain your package structure. Example:

```
com.gpgamelab.justpatience
+-- model        # Game logic and card rules
+-- view         # UI components
+-- viewmodel    # State management
+-- util         # Helpers and utilities
```

---

## ? Release Builds

This project uses a local `keystore.properties` file for release signing.

### 1) Configure local signing (one-time)

From the project root:

```bash
cp keystore.properties.example keystore.properties
```

Edit `keystore.properties` with your real values:

```properties
storeFile=/absolute/path/to/your-release-keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Security notes:
- `keystore.properties` is local-only and should not be committed.
- Keep your `.jks` file backed up in a secure location.

### 2) Build signed release artifacts

From the project root:

```bash
./gradlew :app:assembleRelease
./gradlew :app:bundleRelease
```

Outputs:
- Signed APK: `app/build/outputs/apk/release/`
- Signed AAB: `app/build/outputs/bundle/release/`

If `keystore.properties` is missing, Gradle falls back to debug signing for the release variant, which is not suitable for Play Store publishing.

## ? Pre-publish Checklist

Before uploading to Google Play, verify all of the following:

- [ ] **Package name is final** (`com.gnuparadigms.gameslab.justpatience`) and unchanged since internal testing.
- [ ] **Version updated** in `app/build.gradle.kts` (`versionCode` incremented, `versionName` updated).
- [ ] **Release signing is correct** (`./gradlew :app:signingReport` shows release store + alias, not debug).
- [ ] **Release ads behavior confirmed** (`useProductionAds=true` only when you are ready for production ad IDs).
- [ ] **Privacy policy available** (hosted URL preferred; About-page text can mirror it).
- [ ] **Core device testing complete** (portrait/landscape, undo/redo/restart, stats, home/settings navigation).
- [ ] **Monetization flow tested** (banner load, rewarded/interstitial trigger points, offline fallback behavior).
- [ ] **Build final AAB** with:
  - [ ] `./gradlew :app:bundleRelease`
  - [ ] artifact present at `app/build/outputs/bundle/release/app-release.aab`
- [ ] **Archive release metadata** (AAB hash, signing SHA-1/SHA-256, release notes, test checklist).

---

## ? Screenshots

(Add images here)

```
![Gameplay Screenshot](docs/screenshot1.png)
```

---

## ? Contributing

If you plan to accept contributions:

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

Coding guidelines:
- Follow Kotlin style conventions
- Keep game logic separate from UI code
- Add tests for new features

---

## ? Known Issues

- Rare crash when rotating screen during animation
- Layout scaling issues on very small screens

---

## ? License

This project is licensed under the terms described in the file:

```
LICENSE.md
```

Please review that file for full licensing details.

---

## ? Acknowledgements

- Inspired by classic solitaire implementations
- Thanks to the Android and Kotlin communities
