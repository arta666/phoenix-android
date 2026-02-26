# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build the Go binary for Android (must run before building APK)
make android-client
# Output: android/app/src/main/jniLibs/arm64-v8a/libphoenixclient.so

# Verify Go binary compiles without running a full build
make android-check

# Run Go tests
make test                         # go test ./...
go test ./pkg/config/...          # single package

# Build Android APK
cd android
./gradlew assembleDebug
./gradlew assembleRelease         # requires signing config

# Install on connected device
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

For release signing, set `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in `android/local.properties` (never commit this file) or pass as Gradle `-P` flags.

## Architecture

This repo contains two tightly coupled components that must be kept in sync:

### 1. Go binary (`cmd/android-client/`)

Compiled for `linux/arm64` as `libphoenixclient.so` and placed in `jniLibs/arm64-v8a/`. Android puts it in `nativeLibraryDir` which is always executable (bypasses the W^X policy that would block executables extracted from `assets/`). `android:extractNativeLibs="true"` in the manifest is required.

The binary accepts these flags:
- `-config <path>` — path to TOML config written by `ConfigWriter.kt`
- `-files-dir <path>` — where key files are stored (`Context.getFilesDir()`)
- `-gen-keys` — generates Ed25519 keypair, prints `PUBLIC_KEY=<b64>` to stdout
- `-tun-socket <name>` — VPN mode: abstract Unix socket name to receive TUN fd

**VPN mode flow**: Kotlin creates a TUN interface via `VpnService.Builder`, sends the TUN fd to Go over an abstract Unix socket using `SCM_RIGHTS`, then tun2socks routes all TUN packets through the local SOCKS5 listener (`127.0.0.1:10080`) → HTTP/2 tunnel → server.

Shared Go packages under `pkg/` are used by both this binary and the desktop client:
- `pkg/transport/` — HTTP/2 multiplexing (core tunnel)
- `pkg/config/` — TOML parsing; TOML keys must match Go struct tags exactly (e.g. `private_key` not `private_key_path`)
- `pkg/adapter/socks5/` — SOCKS5 handshake + UDP
- `pkg/crypto/` — Ed25519 key generation

### 2. Android app (`android/`)

Standard MVVM + Clean Architecture with Hilt DI:

```
UI (Compose screens) → ViewModel → Repository → DataStore
                    ↓
              Service (launches Go binary as child process)
                    ↓
              ServiceEvents (SharedFlow event bus)
                    ↑
              ViewModel (collects events)
```

**Key architectural decisions:**
- `ServiceEvents` (singleton `object`) uses `SharedFlow` instead of broadcasts — broadcasts are unreliable on Samsung devices
- `SharingStarted.Eagerly` on config flow — ensures config survives screen navigation
- `HomeViewModel` starts a 20-second timeout coroutine on connect; cancelled when a `Connected` event arrives
- `onCancelClicked()` and `onMainButtonClicked()` are separate — prevents accidental cancel during the `CONNECTING` state

### Service modes

| Mode | Service | Flag |
|------|---------|------|
| SOCKS5 proxy | `PhoenixService` | (none) |
| VPN (all traffic) | `PhoenixVpnService` | `-tun-socket <name>` |

`HomeViewModel` branches based on `config.useVpnMode` to start the correct service.

### Config TOML format

`ConfigWriter.kt` writes this file to `filesDir/client.toml` before launching the binary. Field names must match Go struct tags:

```toml
remote_addr = "host:port"
server_public_key = "base64"   # blank = h2c mode
private_key = "/abs/path/to/client.private.key"  # blank = no mTLS

[[inbounds]]
protocol = "socks5"
local_addr = "127.0.0.1:10080"
enable_udp = false
```

### CI

`.github/workflows/android.yml` triggers on `v*` tags. Requires four GitHub Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The workflow decodes the keystore and passes signing values via Gradle `-P` flags.

## Important constraints

- `android/local.properties` must never be committed (contains local SDK path and signing credentials)
- `android/app/src/main/jniLibs/` is in `.gitignore` — run `make android-client` after cloning
- tun2socks log level must be `"warn"` (zap level) — `"warning"` causes a fatal error at startup
- minSdk is 26 (Android 8.0); architecture is ARM64 only — the binary will not run on x86 emulators
