# KMP-GO Monorepo

This is a Kotlin Multiplatform monorepo containing multiple apps, shared modules, and Go backend
services (`services/`).

## Structure

- [apps/leptos-agent](./apps/leptos-agent/) — the Leptos Agent app
    - [androidApp](./apps/leptos-agent/androidApp) — Android entry point (`com.alwinsden.leptosagent.MainActivity`)
    - [iosApp](./apps/leptos-agent/iosApp/iosApp) — iOS entry point (SwiftUI). This is where you add SwiftUI code for
      the app. It is pure SwiftUI and consumes only the `SharedLogic` Kotlin framework.
    - [shared/sharedLogic](apps/leptos-agent/shared/src) — non-UI logic shared between the app's targets.
      The most important subfolder is [commonMain](apps/leptos-agent/shared/src/commonMain/kotlin). If
      preferred, you can add code to the platform-specific folders here too. Also produces the static iOS framework
      `SharedLogic`, which carries the OpenAPI-generated payload types (visible from Swift).
    - [shared/sharedUI](./apps/leptos-agent/shared/sharedUI/src) — Compose Multiplatform UI (used by Android; the iOS
      app does not render it). Code that’s common for all targets goes in
      [commonMain](./apps/leptos-agent/shared/sharedUI/src/commonMain/kotlin).
- [shared/](./shared/) — currently empty; repo-level shared modules would live here (each app's shared modules live
  inside the app folder under `apps/<app>/shared/`).
- [services/](./services/) — Go backend services; the `go.mod` lives at the repo root, so all Go commands run from
  there. [services/leptos-agent](./services/leptos-agent) is the backend for the Leptos Agent UI (listens on `:8080`).

## Commands

### Kotlin / Android

```sh
./gradlew :apps:leptos-agent:androidApp:assembleDebug      # build the Android app
./gradlew :apps:leptos-agent:androidApp:installDebug       # install to a connected device/emulator
./gradlew allTests                                         # run all module tests
./gradlew :apps:leptos-agent:shared:allTests   # run tests for one module
```

### iOS

Open [apps/leptos-agent/iosApp](./apps/leptos-agent/iosApp) in Xcode and run it from there — there is no Gradle iOS
app target. The Xcode build phase runs
`:apps:leptos-agent:shared:embedAndSignAppleFrameworkForXcode` from the repo root.

### Go services

```sh
go run ./services/leptos-agent   # run the backend (listens on :8080)
go build ./...                   # build all services
go test ./...                    # test all services
```

#### Hot reload with air

[.air.toml](./.air.toml) is configured for the whole `services/` monorepo: pick the service to run/watch with the
`AIR_SERVICE` env var (there is no default — it must be set). It builds `./services/$AIR_SERVICE` and rebuilds on any
change under `services/`:

```sh
AIR_SERVICE=leptos-agent go tool air   # run the backend with hot reload (listens on :8080)
```

## API contract (OpenAPI-first)

[services/api.yaml](./services/api.yaml) is the single source of truth for request/response types; both sides are
generated from it:

```sh
make generate                                                                            # regenerate both sides
go tool oapi-codegen -config services/leptos-agent/oapi-codegen.yaml services/api.yaml  # Go types → services/leptos-agent/gen.go
./gradlew :apps:leptos-agent:shared:openApiGenerate                         # Kotlin models → :shared:sharedLogic
```

`make generate` (see the [Makefile](./Makefile)) runs both generators; the individual commands are its `go-types`
and `kotlin-models` targets.

To add or modify an endpoint/payload, edit `services/api.yaml` and rerun both generators — never hand-edit
`services/leptos-agent/gen.go` or the generated Kotlin models under
`apps/leptos-agent/shared`. (The Kotlin generation also runs automatically before every
`:shared:sharedLogic` compilation.)

### Adding a new app

1. Create the app modules under `apps/<new-app>/` (e.g. `apps/<new-app>/androidApp` and
   `apps/<new-app>/shared/<module>`).
2. Add them to [settings.gradle.kts](./settings.gradle.kts) via `include(":apps:<new-app>:androidApp")`.
3. Depend on the shared modules with `project(":apps:<new-app>:shared:sharedLogic")` /
   `project(":apps:<new-app>:shared:sharedUI")`.
4. For iOS, copy `apps/leptos-agent/iosApp` as a starting point and make sure its Gradle script phase points at
   the repo root (`cd "$SRCROOT/../../.."`) and the correct `embedAndSignAppleFrameworkForXcode` task path.

Use the run configurations provided by the run widget in your IDE's toolbar to run the apps.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
