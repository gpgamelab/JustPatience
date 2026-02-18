````markdown
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

### ? High Priority
- [ ] Fix foundation pile put validation
- [ ] Fix edge-case bug when dragging multiple stacked cards
- [ ] Improve win detection logic
- [ ] Reduce display size of the cards
- [ ] Add unit tests for move validation engine

### ? Medium Priority
- [ ] Add account login and creation with the backend server
- [ ] Add sound effects
- [ ] Add animation polish
- [ ] Improve tablet layout support

### ? Low Priority
- [ ] Add dark mode toggle
- [ ] Add statistics tracking
- [ ] Add alternative card themes

### ? Wish list to be prioritized
- [ ] Add monitization with ribbon ads
- [ ] Add monitization with full screen ads
- [ ] Add monitization with in app purchases

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
````
