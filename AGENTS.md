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

## Makefile (Go/app dev loop)

All Make targets reference `$(SERVICE)` and default it to **empty** — always pass
`SERVICE=leptos-agent`, or paths will be malformed:

```sh
make generate SERVICE=leptos-agent            # both generators: go-types + kotlin-types
make services-autogenerate SERVICE=…          # go-types + ent codegen (ent-schema-generator)
make migrations SERVICE=leptos-agent          # apply ent schema to the DB (sops-decrypted, see below)
make run-server SERVICE=leptos-agent          # hot-reload server (sops-decrypted secrets → go tool air)
```

- `run-server` → `AIR_SERVICE=$(SERVICE) go tool air` wrapped in sops. Air watches all of `services/` **and**
  `services/leptos-agent/api.yaml`; its build command runs `make go-types` first, so Go API types regenerate on
  every rebuild.
- `ENV=DEV` (Makefile default) selects the secrets file `secrets.$(ENV).env` — only `secrets.DEV.env` exists so far.

## Secrets (sops + age)

- `secrets.DEV.env` at the repo root is **sops-encrypted and intentionally committed** — it is the vault, not a
  leak (verify with `go tool sops filestatus secrets.DEV.env` → `{"encrypted":true}`). Never commit a decrypted
  copy; plain `.env` is gitignored.
- `.sops.yaml` binds encrypted files to the repo's age recipient. The matching private key lives at
  `~/.config/sops/age/keys.txt` (`chmod 600`) and is backed up outside the machine — losing it makes the committed
  secrets unrecoverable.
- **macOS quirk:** sops does *not* auto-scan `~/.config/sops/age/keys.txt` on macOS. The Makefile exports
  `SOPS_AGE_KEY_FILE` for its targets; for bare `go tool sops -d …` invocations in a shell, export it manually
  first.
- Keys inside: `OPENROUTER_KEY`, `POSTGRES_DB_HOST/USER/PORT/PASS`, `SERVER_ENDPOINT`.

## Go services

`services/` is the Go monorepo; the **`go.mod` lives at the repo root** (module
`github.com/alwinsden/kmp-monorepo`), so all Go commands run from the repo root:

```sh
go run ./services/leptos-agent   # run the backend (listens on :8080)
go test ./...                    # tests for all services
go build ./...                   # NOTE: writes a `leptos-agent` binary at the repo root (gitignored)
```

- **No godotenv anymore** — services read plain `os.Getenv` only. Without secrets injected the server still boots
  and serves `/health`, but OpenRouter/DB calls fail. `make run-server` / `make migrations` inject them via
  `go tool sops exec-env`.
- `go tool air`, `go tool oapi-codegen`, `go tool ent`, and `go tool sops` need no global install — all are
  declared in `go.mod`'s `tool` block.

## API contract (OpenAPI-first)

`services/leptos-agent/api.yaml` is the single source of truth for request/response types; both sides are generated
from it:

```sh
make go-types SERVICE=leptos-agent      # Go types → services/leptos-agent/gen.go (strict std-http server + embedded spec)
make kotlin-types SERVICE=leptos-agent  # Kotlin models → apps/leptos-agent/shared/build/generated/openapi
                                        # (package com.alwinsden.leptosagent.shared, kotlinx-serialization)
```

To add/modify an endpoint or payload, edit `services/leptos-agent/api.yaml` and rerun both generators — never
hand-edit `services/leptos-agent/gen.go` or the generated Kotlin under `apps/leptos-agent/shared/build`. Kotlin
generation also runs automatically before every `:shared` compilation. Current surface: `GET /health`,
`POST /premium/submit` (JSON body in, SSE `text/event-stream` out), and `GET /models/listing`.

## Database (Ent + Supabase)

- Schema source: `services/leptos-agent/schema/*.go` (currently `ModelListing` → table `model_listings`). Generated
  code lives in `services/leptos-agent/ent/` — regenerate with
  `go tool ent generate ./services/<svc>/schema --target ./services/<svc>/ent` (via `make services-autogenerate`).
- Migrations are a **standalone command** (`services/<svc>/cmd/migrate`) calling `client.Schema.Create(ctx)` —
  additive only: creates missing tables/columns, never drops or renames. Apply with
  `make migrations SERVICE=leptos-agent`.
- The table list is **compiled into the binary** (`ent/migrate/schema.go`): add/edit a schema file → regenerate →
  then migrate. Skipping regen makes migration silently no-op for the new table.
- Postgres is **Supabase**; DSN is built in `cmd/migrate/main.go` (`dbname=postgres`, POSTGRES_DB_* env). Direct
  host `db.<ref>.supabase.co` is **IPv6-only** — from IPv4-only networks (e.g. under some VPNs) use the session
  pooler `aws-<n>-<region>.pooler.supabase.com:5432` with user `postgres.<project-ref>`.

## Layout & module graph

```
apps/leptos-agent/androidApp    # Android entry (com.alwinsden.leptosagent.MainActivity)
apps/leptos-agent/iosApp        # pure SwiftUI; consumes the `Shared` framework (import Shared)
apps/leptos-agent/shared        # KMP module: android + iosArm64/iosSimulatorArm64
apps/eight-params/androidApp    # Android entry (com.alwinsden.eightparams.MainActivity) — depends on :apps:eight-params:shared
                                # Apple Health–style tracker UI: Summary home, metric detail (D/W/M/6M/Y line charts),
                                # logging sheet; weight, height, blood pressure, sleep
apps/eight-params/iosApp        # SwiftUI hello-world; consumes the `Shared` framework (import Shared)
apps/eight-params/shared        # KMP: health domain shared with a future iOS UI — Room DB (HealthEntryEntity/
                                # HealthEntryDao/HealthDatabase, Room3 KMP), MetricType, ChartAggregation (D/W/M/6M/Y
                                # bucketing, weekly ticks, pannable windows via backSteps, AVERAGE header, nice y-axis),
                                # formatters; commonTest covers it. The Android chart UI renders it with Vico 3.3.1.
services/leptos-agent           # Go backend for the app (LLM responses streamed from OpenRouter)
services/commonStructs          # small Go structs package shared across services
```

- `:apps:leptos-agent:shared` — `ApiClient` (Ktor 3.5.2 client; engine is expect/actual: OkHttp on Android, Darwin
  on iOS), generated OpenAPI payload types, and `ServerEndpoint` (expect/actual over `BuildKonfig.SERVER_ENDPOINT`).
  Produces the static iOS framework **`Shared`**.
- **API base URL is injected at build time**: `shared/build.gradle.kts` reads `SERVER_ENDPOINT` from the repo-root
  `.env` via a Gradle `ValueSource` (Dotenv, `ignoreIfMissing`) into BuildKonfig; Swift reads
  `kmp_serverEndpoint().currentServerUrl()`. ⚠️ Since the sops migration removed `.env`, KMP apps currently build
  with an **empty endpoint** unless a root `.env` (or the ValueSource) provides it.
- `:apps:leptos-agent:androidApp` is currently still the template Compose demo and does **not** depend on `:shared`
  — the iOS app is the only consumer right now.

## Gotchas

- **AGP 9 KMP plugin:** shared modules use `com.android.kotlin.multiplatform.library`, so Android config
  (namespace, compileSdk, minSdk) lives *inside* `kotlin { android { } }` in `shared/build.gradle.kts`, not in a
  top-level `android {}` block.
- Package base is `com.alwinsden.leptosagent`; iOS bundle id is
  `com.alwinsden.leptosagent.leptos-agent$(TEAM_ID)` (the `$(TEAM_ID)` suffix is an Xcode variable, set in
  `apps/leptos-agent/iosApp/Configuration/Config.xcconfig`).
- `local.properties` (Android SDK path) is gitignored and machine-specific — never commit it.
- Ktor: the `ktorLibs` catalog (`io.ktor:ktor-version-catalog:3.5.2`, created in `settings.gradle.kts`) is used by
  `:shared` for its HTTP client; the Ktor server plugin is declared `apply false` in the root `build.gradle.kts`
  but unused (the Go service uses net/http).
- No CI (no `.github/`) and no Kotlin test source sets yet — `allTests` currently has nothing to run;
  `kotlin-test` is preconfigured in `:shared` for when tests are added.
- The README still references the removed `sharedUI` module, the old `SharedLogic` framework name, and
  `services/api.yaml` — trust this file and the build scripts instead.

## Adding a new app

1. Create `apps/<new-app>/androidApp` and `apps/<new-app>/shared` (copy from the existing app).
2. `include(":apps:<new-app>:androidApp")` (and the shared module) in `settings.gradle.kts`.
3. Depend on the shared module with its full path: `project(":apps:<new-app>:shared")`.
4. For iOS, copy `apps/leptos-agent/iosApp` and update its Xcode script phase: `cd "$SRCROOT/../../.."` to the repo
   root and the correct `:apps:<new-app>:shared:embedAndSignAppleFrameworkForXcode` task path.
5. Add `schema/` + `cmd/migrate/` to the new `services/<new-app>` if it needs a database, register its secrets in
   the sops-encrypted file, and extend the Makefile's `SERVICE`-based targets (they already take `SERVICE=`).
