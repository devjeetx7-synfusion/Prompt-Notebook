# SnipNote

SnipNote is an offline-first Android workspace for saving, organising, searching and copying
**AI prompts, notes, code snippets and links** – with an encrypted **Safe** vault for private
items. It is a native Kotlin app built with Jetpack Compose + Material 3, MVVM, a repository
layer and Room.

* Package: `com.synfusion.snipnote`
* Min SDK 26 · Target/compile SDK 35
* Kotlin 2.0.21 · AGP 8.7.3 · Compose BOM 2024.12.01 · Room 2.6.1

---

## Features

### Content management
* Four content types: **Prompt**, **Note**, **Code**, **Link**, plus custom tags.
* Create, read, update, delete, favourite, pin, sort and filter – all persisted in Room.
* Heuristic content-type detection (URL shape, code structure/indentation, instruction-style
  wording). Detection is a **suggestion** shown in the editor; it never silently overrides a
  choice the user made.
* Multiline text, indentation, trailing whitespace and Unicode are stored and copied verbatim.
* One-tap copy with success/failure feedback; the copied string is never truncated.

### Organising
* Seven default categories (AI Prompts, Coding, Writing, Study, Business, Links, Personal)
  plus unlimited custom categories with name, icon, colour and manual ordering.
* Favourites, pinned and recent are **filters over the same rows**, never duplicated records.
* Deleting a category asks explicitly whether its notes are moved to another category or left
  uncategorized – notes are never deleted as a side effect.
* Bulk selection in list views: favourite, pin, delete.

### Search
* Case-insensitive `LIKE` search across titles and content with escaped wildcards
  (`%`/`_` are treated literally), debounced in the UI, indexed (`type`, `categoryId`,
  `updatedAt`, `createdAt`, `isSecure` composite indexes).
* Filters for type, category, tag, favourite and pinned; four sort orders.
* Safe content is excluded **in SQL** from every ordinary query.

### Safe vault (real encryption, not a hidden screen)
* Private items are encrypted with **AES-256-GCM** (`AES/GCM/NoPadding`, 12-byte random IV,
  128-bit tag) using a key created in the **Android Keystore** (`AndroidKeyStore`, alias
  `snipnote_safe_aes_v1`). The key never leaves the device and is never written to disk.
* Unlock requires **BiometricPrompt** with `BIOMETRIC_WEAK | DEVICE_CREDENTIAL` where the
  platform supports it (API 30+), otherwise biometrics alone. There is no in-app bypass.
* Session state is in-memory only; a cold start is always locked.
* Configurable auto-lock (immediately / 15 s / 1 min / 5 min / manual) applied on
  `ON_STOP`→`ON_START`, with suppression while an auth prompt is on screen.
* `FLAG_SECURE` protection for recents thumbnails and screenshots (toggleable).
* Safe rows are excluded from normal lists, search, counts and standard backups.
* Failed decryption (for example after the Keystore key is lost) is surfaced as
  “content unavailable”; the ciphertext is **never deleted**.

### Backup & restore
* JSON export/import through the Storage Access Framework (`CreateDocument` / `OpenDocument`).
* Exports are **plain JSON, not encrypted** – the UI says so.
* Safe items are excluded unless the user opts in (requires an unlocked Safe) and the file is
  then flagged `includesSecureContent` so the import preview can warn.
* Import is staged: validate → preview (counts, warnings, duplicates) → choose duplicate policy
  (skip / keep both / replace) → confirm. Malformed, foreign or checksum-mismatched files are
  rejected before anything is written. Per-item failures are reported, not hidden.

### Design
* Aurora Glass theme: blurred gradient backdrop (single backdrop blur on API 31+ non-low-RAM
  devices, gradient-only fallback otherwise), translucent hairlines cards, restrained shadows.
* Light, dark and system themes; Material 3 dynamic colour is intentionally not used so the
  brand palette stays consistent.
* Phone: glass bottom navigation. ≥600 dp: navigation rail. ≥840 dp: rail with labels and
  multi-column lists.
* Motion is limited to short fades/slides, colour animation on filters and pins.

---

## Architecture

```
app/src/main/java/com/synfusion/snipnote/
├── data/
│   ├── local/        Room entities, DAOs, converters, database
│   ├── repository/   NoteRepository, CategoryRepository, TagRepository, SettingsRepository
│   ├── security/     CipherBox, KeystoreCipherBox, Keystore key provider, SafeSession
│   └── backup/       Backup models, BackupCodec (pure validation), BackupRepository
├── domain/
│   ├── model/        Note, Category, Tag, NoteFilter, Settings
│   └── detect/       ContentTypeDetector (pure Kotlin heuristic)
├── ui/
│   ├── home/ categories/ safe/ settings/ search/ browse/ editor/ detail/
│   ├── components/   Glass surfaces, note cards, filters, empty states
│   ├── navigation/   Routes, NavHost, bottom bar + rail
│   └── theme/        Aurora palette, typography
├── util/             ClipboardService, TextUtils, TimeFormat
├── AppContainer.kt   Manual dependency container (no DI framework needed at this size)
├── SnipNoteApp.kt
└── MainActivity.kt   FragmentActivity (required by BiometricPrompt) + edge-to-edge
```

* **MVVM**: each screen has a `ViewModel` exposing an immutable `UiState` `StateFlow`; the
  ViewModels talk to repositories, never to DAOs or Android UI classes directly.
* **Repository pattern**: `NoteRepository` (interface) ← `RoomNoteRepository`; the Safe paths
  are guarded by `SafeSession` and throw `SafeLockedException` when locked.
* **Persistence**: Room (`snipnote.db`, schema exported to `app/schemas`), foreign keys with
  `CASCADE`/`SET NULL`, `Flow`-based observation.
* **Security boundary**: all Safe encryption/decryption happens in the repository mapping
  layer, so no UI code ever sees ciphertext or can bypass the unlock gate.
* **Search**: SQL `LIKE` with escaping; filters map to bound parameters; sort applied in the
  repository so pinned items always float to the top.

---

## Setup

1. Install **Android Studio Ladybug (2024.2)** or newer with **JDK 17** and Android SDK 35.
2. Clone the repository and open it as a Gradle project.
3. `./gradlew assembleDebug` (or run the `app` configuration).

If the Gradle wrapper JAR is missing from your checkout (it is a binary and is not committed
here), generate it once with a local Gradle 8.11+ installation:

```bash
gradle wrapper --gradle-version 8.11.1
```

To build from the command line you need `local.properties` with `sdk.dir=/path/to/Android/Sdk`
(Android Studio writes this automatically; it must not be committed).

---

## Build and test commands

```bash
# debug build
./gradlew :app:assembleDebug

# JVM unit tests (detection, copy integrity, backup validation, crypto, text/time utils)
./gradlew :app:testDebugUnitTest

# Instrumentation tests (Room CRUD, categories, search, Safe access control,
# real Keystore AES-GCM, clipboard, backup/restore, persistence, UI smoke test)
./gradlew :app:connectedDebugAndroidTest

# release build (R8 + resource shrinking)
./gradlew :app:assembleRelease
```

> **Verification status in this environment:** the work was authored in a sandbox with **no
> JDK, no Gradle distribution, no Android SDK and no access to `dl.google.com` /
> `repo1.maven.org`**, so no Gradle task, unit test or instrumentation test could be executed
> here. The commands above are the exact ones to run locally; results have not been claimed as
> passing anywhere in this document.

---

## Test coverage map

| Area | Where |
| --- | --- |
| Content-type detection | `test/.../ContentTypeDetectorTest.kt` |
| Copy content integrity (lossless transform, Unicode, long text) | `test/.../TextUtilsTest.kt`, `androidTest/.../ClipboardInstrumentedTest.kt` |
| Backup JSON validation, duplicates, integrity, unicode | `test/.../BackupCodecTest.kt` |
| AES-GCM round trip, random IV, tamper detection, wrong key, missing key | `test/.../KeystoreCipherBoxTest.kt` |
| Real Android Keystore path | `androidTest/.../KeystoreCipherInstrumentedTest.kt` |
| Repository CRUD, filters, search, sorting, bulk ops | `androidTest/.../NoteRepositoryInstrumentedTest.kt` |
| Category operations, counts, safe deletion policies, default seeding | `androidTest/.../NoteRepositoryInstrumentedTest.kt` |
| Safe access control, encryption at rest, key loss behaviour, auto-lock | `androidTest/.../SafeAccessControlInstrumentedTest.kt` |
| Backup/restore through Room (export → import, policies, Safe exclusion) | `androidTest/.../BackupRestoreInstrumentedTest.kt` |
| Persistence across a close/reopen cycle | `androidTest/.../PersistenceInstrumentedTest.kt` |
| UI smoke flow | `androidTest/.../MainActivityUiTest.kt` |
| Text/time/tag helpers | `test/.../TextUtilsTest.kt`, `TimeFormatTest.kt`, `TagTest.kt` |

---

## Security model and limitations

**What is protected.** Safe title and content columns are stored only as AES-GCM ciphertext.
Encryption keys are generated by the Android Keystore (`setRandomizedEncryptionRequired(true)`)
and are non-exportable. No key, password or secret is hard-coded, logged or included in exports.
Safe rows are excluded from ordinary SQL queries, search, counts and the default backup. Bulk
mutations of a Safe row also require an unlocked session.

**Key recovery is impossible.** If the Keystore entry disappears – factory reset, “clear
credentials”, a restored device backup, a Keystore reset by the OEM – existing Safe ciphertext
can never be decrypted again. SnipNote will keep the rows and show them as unreadable, but it
cannot recover them and there is no cloud escrow. This is a deliberate consequence of never
exporting the key.

**Backups are not encrypted.** JSON exports are readable by anyone who obtains the file.
Safe items are omitted by default; the opt-in writes them as plain text and warns you first.
Android Auto Backup is disabled (`allowBackup="false"`) because restoring ciphertext without
its Keystore key would produce permanently unreadable data.

**Locking.** Auto-lock runs while the process is alive. Force-stopping the app or process death
always locks Safe. The lock is a UI/access-control gate on top of encryption; the encryption
itself is what protects content at rest.

**Other limitations.**
* Unlocking needs enrolled biometrics or a device screen lock. A device with neither cannot
  open Safe (the UI explains this and offers a re-check).
* The PIN entry is delegated entirely to the system device-credential prompt; there is no
  separate app-managed PIN.
* Import is not a single transaction: a failure in item *N* leaves items 1…*N*-1 imported and
  the UI reports the failure and the count instead of pretending it rolled back.
* Detection heuristics are intentionally conservative – short snippets may stay “Note”.
* Search is `LIKE`-based (fine for tens of thousands of local rows); no FTS tokenizer yet.
* Clipboard reads are restricted to the focused app on Android 10+, which is why the clipboard
  test verifies the write and only asserts the read-back when the OS allows it.
* Export/import uses SAF URIs; no automatic scheduled backups.
* RTL support is inherited from Compose/Material but has not been visually audited.

---

## Repository notes

This repository previously contained only a placeholder README; the Android project in this
commit is the initial implementation. No previous work was removed.

Build artefacts, `local.properties`, `.idea/`, `build/` and the Gradle wrapper JAR are ignored
(`.gitignore`) and no credentials or private data are committed.
