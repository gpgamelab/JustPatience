
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

This section tracks work currently in progress and upcoming improvements.



:::writing{variant="standard" id="48291"} ## 🗺 Roadmap / TODO (Prioritized) This section tracks work currently in progress and upcoming improvements.
--- <details> <summary><strong>PHASE 0: CORE DEVELOPER SUPPORT</strong></summary> <br>
- [ ] **Strengthen Move Validation Engine**
  - [ ] Add comprehensive unit tests for move validation logic </details>
--- <details> <summary><strong>PHASE 1: CORE UX FOUNDATION </strong></summary> <br>
- [ ] **Home / Landing Page**
  - [ ] Design non-game landing screen
  - [ ] Implement navigation entry points
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
- [ ] **Settings Page**
  - [ ] Complete settings UI
  - [ ] Connect settings to functional toggles </details>
--- <details> <summary><strong>PHASE 2: ENGAGEMENT </strong></summary> <br>
- [ ] **First-Time User Experience**
  - [ ] Design onboarding flow
  - [ ] Implement interactive tutorial
- [ ] **Win Celebration**
  - [ ] Implement win celebration animation
  - [ ] Add animation polish and effects
- [ ] **Home Screen Improvements**
  - [ ] Move stats button to home page
  - [ ] Add shopping button (stub)
- [ ] **Account System**
  - [ ] Implement account creation
  - [ ] Implement account login
  - [ ] Connect authentication to backend server
- [ ] **Appearance Options**
  - [ ] Add dark mode toggle </details>
--- <details> <summary><strong>PHASE 3: MONETIZATION PREP </strong></summary> <br>
- [ ] **Ad Framework Integration**
  - [ ] Integrate Google Mobile Ads SDK
  - [ ] Implement ribbon/banner ads
  - [ ] Implement full-screen ads
- [ ] **In-App Purchases (IAP)**
  - [ ] Build basic shopping page
  - [ ] Integrate in-app purchase monetization
- [ ] **Premium Feature System**
  - [ ] Implement premium feature toggles
  - [ ] Gate premium features behind ads/IAP </details>
--- <details> <summary><strong>PHASE 4: RETENTION FEATURES </strong></summary> <br>
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
  - [ ] Add alternative card themes
  - [ ] Implement theme selection system </details> :::

ZYZZX








ZYZZX  PROOF OF CONCEPT
ZYZZX  - [ ] Main task
ZYZZX    - [ ] Subtask 1
ZYZZX    - [ ] Subtask 2
ZYZZX      - [ ] Sub-subtask
ZYZZX  - [x] Completed main task
ZYZZX    - [x] Completed subtask
ZYZZX    
ZYZZX  PHASE 0: CORE DEVELOPER SUPPORT
ZYZZX  - [ ] Add unit tests for move validation engine
ZYZZX  
ZYZZX  PHASE 1: CORE UX FOUNDATION (Week 1-2)
ZYZZX  ├─ 1. Home/Landing Page (non-game screen)
ZYZZX  ├─ 2. Sound Effects Framework + Core SFX
ZYZZX  - [ ] Add sound effects
ZYZZX  ├─ 3. Card Animations (flip, slide)
ZYZZX  - [ ] Add animation polish
ZYZZX  ├─ 4. Loading Screen
ZYZZX  └─ 5. Complete Settings Page
ZYZZX  
ZYZZX  PHASE 2: ENGAGEMENT (Week 3-4)
ZYZZX  ├─ 1. First-Time User Experience (Tutorial/Onboarding)
ZYZZX  ├─ 2. Win Celebration Animation
ZYZZX  - [ ] Add animation polish
ZYZZX  ├─ 3. Move Stats Button to Home Page
ZYZZX  └─ 4. Shopping Button (stub)
ZYZZX  - [ ] Add account login and creation with the backend server
ZYZZX  - [ ] Add dark mode toggle
ZYZZX  
ZYZZX  PHASE 3: MONETIZATION PREP (Week 5-6)
ZYZZX  ├─ 1. Ad Framework Integration (Google Mobile Ads)
ZYZZX  - [ ] Add monitization with ribbon ads
ZYZZX  - [ ] Add monitization with full screen ads
ZYZZX  ├─ 2. IAP Shopping Page (basic)
ZYZZX  - [ ] Add monitization with in app purchases
ZYZZX  └─ 3. Premium Feature Toggles
ZYZZX  
ZYZZX  PHASE 4: RETENTION FEATURES (Week 7+)
ZYZZX  ├─ 1. Simple Achievement System
ZYZZX  ├─ 2. Daily Challenges
ZYZZX  ├─ 3. Performance Metrics Display
ZYZZX  └─ 4. Visual Themes/Customization
ZYZZX  - [ ] Add alternative card themes



















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

To generate a signed APK or App Bundle:

1. In Android Studio: **Build -> Generate Signed Bundle / APK**
2. Follow the signing wizard steps.
3. Securely store your keystore file.

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
