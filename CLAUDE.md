# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Geo Chat App — a single-module Android chat client (Kotlin, Jetpack Compose) talking to a Node.js backend at
`ruinkogr.ru` over REST (Retrofit) and a raw WebSocket (OkHttp), with Firebase for auth/push and Room for local
message caching. There is a Ktor client dependency (`data/remote/ktor/KtorClient.kt`) present but currently unused
by the wired dependency graph — Retrofit/OkHttp is what's actually injected.

## Build & run

Windows PowerShell needs UTF-8 console output set before invoking Gradle, otherwise Cyrillic log lines
(interceptor/logging text is Russian) get mangled:

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
.\gradlew.bat build
```

CMD equivalent: `chcp 65001` then `gradlew.bat build`.

Other common tasks:
- `.\gradlew.bat assembleDebug` — build debug APK only
- `.\gradlew.bat test` — JVM unit tests (module: `chatapp`, currently just the default `ExampleUnitTest`)
- `.\gradlew.bat connectedAndroidTest` — instrumented tests (needs a device/emulator)
- `.\gradlew.bat test --tests "com.ruinkogr.chatapp.SomeTest"` — run a single unit test
- `.\gradlew.bat lint` — Android lint

Toolchain: Gradle 9.7.1 wrapper, JDK 25 toolchain (`gradle-daemon-jvm.properties`), AGP 9.3.2, Kotlin 2.4.10,
compileSdk/targetSdk 37, minSdk 26. Configuration cache is enabled (`gradle.properties`).

### `google-services.json`
`chatapp/build.gradle.kts` has a custom task (`checkAndCopyGoogleServices`) that, on `preBuild`, copies
`../../_secrets_/google-services.json` (a sibling directory outside the repo) into `chatapp/google-services.json`
if the latter doesn't already exist. If Firebase-dependent builds fail with a missing/invalid
`google-services.json`, this task — not the Firebase plugin — is where to look first.

## Architecture

Single Gradle module `:chatapp`. Package root: `com.ruinkogr.chatapp`.

**DI**: Hilt. Two modules under `di/`:
- `NetworkModule` — wires two named `OkHttpClient`/`Retrofit` graphs: `"AuthOkHttp"` (plain client, used for
  login/refresh calls and the WebSocket handshake — deliberately has no `AuthInterceptor` to avoid recursive auth)
  and `"MainOkHttp"`/`"MainRetrofit"` (has `AuthInterceptor` attached, used for `UsersService`/`MessagesService`).
  Also provides the singleton `WebSocketManager`.
- `DatabaseModule` — provides the Room `AppDatabase`, `MessageDao`, and `SettingsManager`.

**Auth/session flow**:
- `TokenStorage` is the interface; **`EncryptedPrefsTokenStorage`** (backed by `EncryptedSharedPreferences`) is the
  bound implementation in `NetworkModule`. `DataStoreTokenStorage` also exists implementing the same interface but
  is not currently wired into DI — don't assume it's active without checking.
- `AuthInterceptor` attaches the access token to outgoing requests, and on a 401/403 synchronously calls
  `AuthService.refreshTokensSync(...).execute()` to refresh under a `synchronized` block (re-checking whether
  another thread already refreshed) before retrying the original request. It always passes through requests to
  `/api/auth/refresh` unmodified to avoid recursion.
- `SessionManager` is a singleton `SharedFlow<AuthEvent>` — `AuthInterceptor` emits `AuthEvent.Logout` when refresh
  fails so the UI layer (observed from `MainActivity`/`AuthViewModel`) can navigate back to the login screen.
- Sync (blocking) variants of the token storage methods exist alongside the suspend ones specifically so
  `AuthInterceptor` (an OkHttp `Interceptor`, not a suspend context) and `WebSocketManager`'s token provider lambda
  can read tokens synchronously.

**Data layer** (`data/`):
- `repository/ChatRepository` follows an offline-first pattern: emit cached Room rows first, then fetch network,
  write results into Room, then re-emit from Room's live `Flow` so the UI always reflects DB state. Two variants
  exist (`getMessagesWithCache` vs `getMessagesWithCacheAlt`) with slightly different emit/error semantics — check
  which one call sites actually use before assuming behavior.
- `repository/BaseRepository.safeApiCall` is the shared wrapper turning a Retrofit `Response<T>` into
  `Resource<Success|Error>`, distinguishing `IOException` (no connection) from other failures.
- Room: single entity `MessageEntity` (table `cached_messages`) with a Dto↔Entity mapper in the same file
  (`toEntity()`/`toDto()`). Single DAO `MessageDao`. DB name `chat_app_database`, no migrations defined
  (`exportSchema = false`, version 1).

**Realtime**: `websocket/WebSocketManager` opens a raw OkHttp WebSocket to `wss://ruinkogr.ru` with the access
token in the `Authorization` header, parses incoming JSON as `MessageEntity` via Gson, writes straight to Room
(so Compose observing the Room `Flow` updates automatically), and sends an `ACK` back. It reconnects with
exponential backoff (2s → capped at 64s) on `onClosed`/`onFailure`. Connect/disconnect lifecycle is driven by
`websocket/AppLifecycleObserver`, a `DefaultLifecycleObserver` registered on `ProcessLifecycleOwner` in
`ChatApplication.onCreate` — the socket opens on app foreground and closes on app background, not per-screen.

**Push notifications**: `service/AppFirebaseMessagingService` handles two `data`-payload shapes distinguished by
`type`: `SERVER_STATUS` (toggles `ui/users/ServerStatusMonitor`, a process-wide `StateFlow<Boolean?>`, and shows a
system notification only when the app isn't foregrounded) and `CHAT_MESSAGE`/anything with an `id` key (inserts
directly into Room, then shows an advanced notification with inline "Mark as Read" and "Reply" actions handled by
`receiver/NotificationActionReceiver`). Foreground detection is via polling `ActivityManager.runningAppProcesses`
importance, not a bound-service/callback mechanism. Notification channels (`chat_messages_channel`,
`server_status_channel`) are created once in `ChatApplication.onCreate`.

**Navigation**: Single-Activity (`ui/MainActivity`) with Compose Navigation (`NavHost`), string routes:
`login_screen`, `register_screen`, `users_list`, `chat_screen/{chatWithUserId}`, `settings_screen`. Start
destination is computed once from `EncryptedPrefsTokenStorage.getCurrentUserIdSync()` (logged-in vs not) and from
an `OPEN_CHAT_WITH_USER_ID` intent extra set by push-notification taps; `onNewIntent` handles the warm-start case
via a `mutableStateOf` + `LaunchedEffect` bridge into the NavController.

**UI** (`ui/`): Compose + Material3, MVVM with Hilt-injected `ViewModel`s per feature folder (`auth`, `chat`,
`settings`, `users`). No fragments/XML views.

## Notable gotchas for changes in this repo

- Comments and some UI strings are in Russian; don't be surprised by mixed-language code.
- Because there are two `OkHttpClient`/`Retrofit` graphs (`"AuthOkHttp"` vs `"MainOkHttp"`/`"MainRetrofit"`), when
  adding a new Retrofit service decide deliberately whether it needs the `AuthInterceptor` (most things do) or must
  avoid it (auth endpoints, to prevent refresh recursion) — use the matching `@Named` qualifier.
- `WebSocketManager` and `AuthInterceptor` both cast `TokenStorage` to `EncryptedPrefsTokenStorage` to reach the
  sync accessor methods; if you ever swap the bound implementation in `NetworkModule`, these casts break silently
  at runtime (`ClassCastException`), not at compile time.
- Base URL (`https://ruinkogr.ru`) and WebSocket URL (`wss://ruinkogr.ru`) are hardcoded string literals in
  `RetrofitClient` and `WebSocketManager` respectively, not build-config/env driven — there's no separate
  debug/release or local backend target.
