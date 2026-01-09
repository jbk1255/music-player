# 🎵 Playra — Desktop Music Player (JavaFX)

**Playra** is a desktop music player built with **Java 17**, **JavaFX**, and **Maven**.  
It supports local music playback, playlist management, persistent storage, and a clean, usability‑focused UI.

This project was developed as both:
- a **practical, fully working desktop application**, and  
- a demonstration of **software engineering principles** learned at university (architecture, data structures, design patterns, complexity awareness, and HCI).

---

## ✨ Features

- 🎶 Import and play local audio files (`.mp3`, `.wav`, `.m4a`)
- 📁 Persistent music library (saved between sessions)
- 📜 Playlist management (create, add, remove, delete)
- 🔍 Search by title, artist, or album
- ⏯️ Playback controls (play / pause / next / previous)
- ⏱️ Interactive playback progress bar
- 🎨 Modern JavaFX UI with accessibility and usability considerations

---

## 🖥️ Tech Stack

- **Language:** Java 17  
- **UI:** JavaFX (Controls + Media)  
- **Build Tool:** Maven  
- **Persistence:** JSON (Gson)  
- **Architecture:** Layered / MVC‑inspired

---

## ▶️ Running the App Locally

### Prerequisites

- **Java 17 or newer**
- **Maven 3.8+**

> JavaFX is handled automatically via Maven — no manual JavaFX installation required.

---

### Clone the Repository

```bash
git clone https://github.com/jbk1255/music-player.git
```

This repository contains a multi‑directory layout.  
The JavaFX application lives inside the `musicplayer/` directory.

```bash
cd music-player/musicplayer
```

⚠️ **Important:**  
The app will only run correctly when executed from inside the `musicplayer/` directory (where `pom.xml` is located).

---

### Run the Application

```bash
mvn clean javafx:run
```

The Playra desktop window will open automatically.

---

## 🎧 Using the App

1. Click **Import Folder**
2. Select a directory containing audio files
3. Songs will appear in the library instantly
4. Select a song and press **Play**

Library and playlists are saved automatically and restored on the next launch.

---

## 📂 Test Music (Optional)

For recruiter/demo convenience, the test folder below is included:

```bash
sample-music/
```

### How to Test

1. Launch Playra
2. Click **Import Folder**
3. Select the `sample-music` directory within `musicplayer/`
4. Songs will load immediately

Sample files are included only for demonstration/testing purposes. All credit 
for music goes to "PLUGGING AI".

---

## 🧠 Software Engineering Concepts Demonstrated

### 1. Separation of Concerns (Architecture)

The codebase is organised into clear layers:

- **UI:** `MainView` (JavaFX layout + event wiring)
- **Domain:** `Song`
- **Services:** `LibraryService`, `PlaylistService`
- **Playback:** `AudioPlayer`, `PlaybackQueue`
- **Persistence:** `JsonStore`

This follows an MVC‑inspired layered architecture, improving readability, maintainability, and extensibility.

---

### 2. Data Structures & Complexity Awareness

- `Map<String, Song>` for **O(1)** song lookup by ID
- `LinkedHashSet<String>` for playlists (preserves order, avoids duplicates)
- `ObservableList<Song>` for efficient JavaFX UI updates
- Search and filtering operations run in **O(n)** time, appropriate for interactive UI usage

These choices reflect conscious performance and design trade‑offs.

---

### 3. Design Patterns

- **Service Layer / Facade Pattern**  
  UI interacts with services instead of internal data structures.

- **Playback Queue Abstraction**  
  Navigation logic (`next`, `prev`, `playAt`) is encapsulated away from the UI.

- **Extensible Design (Strategy‑ready)**  
  The architecture supports future strategies (sorting, filtering, playback behaviour) without major refactoring.

---

### 4. Human–Computer Interaction (HCI)

UI design decisions were informed by HCI principles such as:

- Visual hierarchy (emphasising “Now Playing” state)
- Reduced cognitive load via grouped controls
- Immediate feedback (button enable/disable logic)
- Consistency and error prevention

---

## 📁 Project Structure

```
music-player/
 └─ musicplayer/
    ├─ pom.xml
    └─ src/
       ├─ main/
       │  ├─ java/
       │  │  └─ com/johnk/musicplayer/
       │  │     ├─ ui/
       │  │     ├─ service/
       │  │     ├─ player/
       │  │     ├─ domain/
       │  │     └─ persistence/
       │  └─ resources/
       │     └─ com/johnk/musicplayer/ui/
```

---

## 🛠️ Build Details

- **Main class:** `com.johnk.musicplayer.App`
- **Java version:** 17
- **JavaFX managed via:** `javafx-maven-plugin`

---

## 🚀 Future Improvements

- Sorting strategies (title / artist / album)
- Drag‑and‑drop file import
- Keyboard media shortcuts
- Album artwork support
- Standalone runnable JAR or installer

---

## 💻 Platform Compatibility

Playra is a cross-platform JavaFX desktop application and runs on:

- Windows
- macOS (Intel & Apple Silicon)
- Linux

Note: Audio format support depends on the host operating system’s media codecs.

---

## 👤 Author

**John Kollannur**  
Computer Science Student

GitHub: https://github.com/jbk1255

---

## ⭐ Note for Recruiters

This project demonstrates:

- clean architecture
- thoughtful data structure usage
- UI/UX awareness
- practical JavaFX experience
- production‑ready organisation

Thank you for taking the time to review it.

