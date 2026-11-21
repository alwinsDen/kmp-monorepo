---
name: New KMP App
description: Scaffold a new KMP app (androidApp + shared + iosApp) under apps/ following this monorepo's conventions, with a centered hello-world on both platforms. Use when the user asks to create/add a new app or new KMP project in this repo.
---

# Creating a new KMP app in this monorepo

**Base template: `apps/eight-params`** — the minimal hello-world app, already wired to the
monorepo build. Copy it, then apply the rename map below.

Do **not** copy `apps/leptos-agent` as a starting point: it carries backend plumbing
(OpenAPI codegen, Room, BuildKonfig, Ktor client) a fresh app should not inherit.
Consult it only when *adding* those features later.

## 0. Confirm inputs

- **Folder name** → `apps/<name>` (kebab-case, e.g. `eight-params`).
- **Package base** → `com.alwinsden.<flattened>`: lowercase, no underscores/hyphens,
  cannot *start* with a digit (folder names may — Gradle paths accept `8-params`,
  but Kotlin packages and iOS bundle IDs do not).
  Example: `eight-params` → `com.alwinsden.eightparams`.

## 1. Copy the template

```sh
rsync -a --exclude 'build' --exclude '.gradle' apps/eight-params/ apps/<name>/
```

## 2. Rename map

| File | Change |
| --- | --- |
| `shared/build.gradle.kts` | `namespace = "com.alwinsden.eightparams.shared"` → `com.alwinsden.<pkg>.shared` |
| `shared/src/**/com/alwinsden/eightparams/` | move dir to `com/alwinsden/<pkg>/` and update `package` lines (Greeting.kt, GreetingUtil.kt, Platform.kt, Platform.android.kt, Platform.ios.kt) |
| `androidApp/build.gradle.kts` | dep → `implementation(project(":apps:<name>:shared"))` (always the **full path**); `namespace` + `applicationId` → `com.alwinsden.<pkg>` |
| `androidApp/src/main/kotlin/com/alwinsden/eightparams/` | move dir to `com/alwinsden/<pkg>/`, update `package` in MainActivity.kt |
| `androidApp/src/main/res/values/strings.xml` | `app_name` |
| `iosApp/Configuration/Config.xcconfig` | `PRODUCT_BUNDLE_IDENTIFIER=com.alwinsden.<pkg>.<name>$(TEAM_ID)` (`PRODUCT_NAME` stays the folder name) |
| `iosApp/iosApp.xcodeproj/project.pbxproj` | script phase: `./gradlew :apps:eight-params:shared:embedAndSignAppleFrameworkForXcode` → `:apps:<name>:shared:…` (keep `cd "$SRCROOT/../../.."` — repo root is 3 levels up) |
| `iosApp/iosApp/ContentView.swift` | no change — keep `import Shared` (framework name stays `Shared`) |

## 3. Register the modules

Root `settings.gradle.kts`, next to the other apps:

```kotlin
include(":apps:<name>:androidApp")
include(":apps:<name>:shared")
```

## 4. Conventions baked into the template

- AGP 9 KMP plugin: Android config lives **inside** `kotlin { android { } }` in
  `shared/build.gradle.kts` — never add a top-level `android {}` block there.
- Versions come from the **root** catalog (`gradle/libs.versions.toml`, `ktorLibs` in
  root settings) — module build files must not pin their own versions.
- Android app depends on `:shared`; iOS consumes the exported framework via `import Shared`.
- Android manifest has no permissions beyond the defaults for a base app; add
  `INTERNET` etc. only when actually needed.
- `TEAM_ID=` in `Config.xcconfig` stays empty until device signing is required.

## 5. Verify before calling it done

```sh
./gradlew :apps:<name>:androidApp:assembleDebug :apps:<name>:shared:compileKotlinIosSimulatorArm64
xcodebuild -project apps/<name>/iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO
```

All three must pass. Gradle runs with configuration cache; on a suspicious task
failure retry once with `--no-configuration-cache` before debugging code.

## 6. Bookkeeping

- Add the new app's lines to the layout section of `AGENTS.md` (keep it accurate —
  agents trust that file over the README).
- Point the user at how to run: `./gradlew :apps:<name>:androidApp:installDebug`,
  and open `apps/<name>/iosApp/iosApp.xcodeproj` in Xcode for iOS.

## Extending later (out of scope for the base scaffold)

- **Backend/API**: follow `apps/leptos-agent` + `services/leptos-agent` — Go service,
  `services/<name>/api.yaml`, `openapiGenerator` + `buildkonfig` plugins on `:shared`,
  `make generate SERVICE=<name>`.
- **Database**: `schema/` + `cmd/migrate` in the service, `make migrations SERVICE=<name>`.
