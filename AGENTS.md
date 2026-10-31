# AGENTS.md

Kotlin Multiplatform **monorepo** (app + shared module + Go backend) built from the JetBrains KMP template. Gradle
KTS + version catalog (`gradle/libs.versions.toml`); versions there are the source of truth (Kotlin 2.4.10, AGP
9.0.1, Compose Multiplatform 1.11.1, compileSdk 36 / minSdk 24 / targetSdk 36).

## Commands

```sh
./gradlew :apps:leptos-agent:androidApp:assembleDebug   # build Leptos Agent Android app
./gradlew :apps:leptos-agent:androidApp:installDebug    # install to connected device/emulator
./gradlew allTests                                      # all module tests
./gradlew :apps:leptos-agent:shared:allTests            # tests for one module
```

- iOS: run from Xcode (`apps/leptos-agent/iosApp/iosApp.xcodeproj`) — no Gradle iOS app target. Its Gradle build
  phase does `cd "$SRCROOT/../../.."` (repo root) and runs
  `:apps:leptos-agent:shared:embedAndSignAppleFrameworkForXcode`.
- `org.gradle.configuration-cache=true` is set; if a task fails under config cache, retry with
  `--no-configuration-cache` before assuming a code error.
- The Gradle daemon JVM is pinned to Azul JDK 21 (`gradle/gradle-daemon-jvm.properties`, auto-provisioned); Kotlin
  and Java compilation targets JVM 11.

## Go services

`services/` is the Go monorepo; the **`go.mod` lives at the repo root** (module
`github.com/alwinsden/kmp-monorepo`), so all Go commands run from the repo root:

```sh
go run ./services/leptos-agent   # run the backend (listens on :8080)
go test ./...                    # tests for all services
go build ./...                   # NOTE: writes a `leptos-agent` binary at the repo root (gitignored)
```

- **The server exits at startup without a repo-root `.env`** (`godotenv.Load()` is fatal) and needs `OPENROUTER_KEY`
  set (consumed by `services/leptos-agent/openrouter`). `.env.dev` is the committed template — copy it to `.env` and
  fill in the key (`.env` is gitignored).
- Hot reload: `make run-server SERVICE=leptos-agent` (→ `AIR_SERVICE=leptos-agent go tool air`). Air watches all of
  `services/` **and `api.yaml`**; its build command runs `make go-types` first, so Go API types are regenerated on
  every rebuild.
- `go tool air` and `go tool oapi-codegen` need no global install — both are declared in `go.mod`'s `tool` block.

## API contract (OpenAPI-first)

`services/leptos-agent/api.yaml` is the single source of truth for request/response types; both sides are generated
from it:

```sh
make generate      # both generators (targets: go-types + kotlin-types)
make go-types      # Go types → services/leptos-agent/gen.go (strict std-http server + embedded spec)
make kotlin-types  # Kotlin models → apps/leptos-agent/shared/build/generated/openapi
                   # (package com.alwinsden.leptosagent.shared, kotlinx-serialization)
```

To add/modify an endpoint or payload, edit `services/leptos-agent/api.yaml` and rerun both generators — never
hand-edit `services/leptos-agent/gen.go` or the generated Kotlin under `apps/leptos-agent/shared/build`. Kotlin
generation also runs automatically before every `:shared` compilation. Current surface: `GET /health` and
`POST /premium/submit` (JSON body in, SSE `text/event-stream` out).

## Layout & module graph

```
apps/leptos-agent/androidApp    # Android entry (com.alwinsden.leptosagent.MainActivity)
apps/leptos-agent/iosApp        # pure SwiftUI; consumes the `Shared` framework (import Shared)
apps/leptos-agent/shared        # KMP module: android + iosArm64/iosSimulatorArm64
services/leptos-agent           # Go backend for the app (LLM responses streamed from OpenRouter)
services/commonStructs          # small Go structs package shared across services
```

- `:apps:leptos-agent:shared` — `ApiClient` (Ktor client; engine is expect/actual: OkHttp on Android, Darwin on iOS)
  plus the generated OpenAPI payload types (package `com.alwinsden.leptosagent.shared`). Produces the static iOS
  framework **`Shared`**; payload classes are visible from Swift (a module's own classes need no explicit
  `export()`).
- `:apps:leptos-agent:androidApp` is currently still the template Compose demo and does **not** depend on `:shared`
  — the iOS app is the only consumer right now.
- The iOS app hardcodes its API base URL to a LAN IP in `iosApp/App/ChatViewModel.swift` — point it at the machine
  running the Go server (an Android emulator would need `10.0.2.2`, not `localhost`).

## Gotchas

- **AGP 9 KMP plugin:** shared modules use `com.android.kotlin.multiplatform.library`, so Android config
  (namespace, compileSdk, minSdk) lives *inside* `kotlin { android { } }` in `shared/build.gradle.kts`, not in a
  top-level `android {}` block.
- Package base is `com.alwinsden.leptosagent`; iOS bundle id is `com.alwinsden.leptosagent.leptos-agent` (dash
  included, set in `apps/leptos-agent/iosApp/Configuration/Config.xcconfig`).
- `local.properties` (Android SDK path) is gitignored and machine-specific — never commit it.
- Ktor: the `ktorLibs` catalog is used by `:shared` for its HTTP client; the Ktor server plugin is declared
  `apply false` in the root `build.gradle.kts` but unused (the Go service uses net/http).
- No CI (no `.github/`) and no test source sets yet — `allTests` currently has nothing to run; `kotlin-test` is
  preconfigured in `:shared` for when tests are added.
- The README still references the removed `sharedUI` module, the old `SharedLogic` framework name, and
  `services/api.yaml` — trust this file and the build scripts instead.

## Adding a new app

1. Create `apps/<new-app>/androidApp` and `apps/<new-app>/shared` (copy from the existing app).
2. `include(":apps:<new-app>:androidApp")` (and the shared module) in `settings.gradle.kts`.
3. Depend on the shared module with its full path: `project(":apps:<new-app>:shared")`.
4. For iOS, copy `apps/leptos-agent/iosApp` and update its Xcode script phase: `cd "$SRCROOT/../../.."` to the repo
   root and the correct `:apps:<new-app>:shared:embedAndSignAppleFrameworkForXcode` task path.
