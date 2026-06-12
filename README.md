# ShizuNet (Shizuku Proxy & Configuration Link Generator)

[Читать на русском языке](readme_ru.md) | [Download Latest Artifact](https://github.com/zabeiua/shizunet/actions/runs/27436986916/artifacts/7600343173)

**ShizuNet** is a handy Android utility designed to simplify sharing VPN and proxy connections over a mobile hotspot (SoftAP / Tethering). The app automates detecting your hotspot's gateway IP and generates fully configured profiles and QR codes for the popular **Sing-Box** proxy client.

By integrating with **Shizuku**, ShizuNet requests privileged access to the Android system shell to instantly and accurately read routing tables. This eliminates the need to manually search for wireless interface router addresses using third-party terminal emulators.

---

## Latest Build Download

You can download the compiled app package directly from GitHub Actions build artifacts:
**[Download Latest Compiled Artifact](https://github.com/zabeiua/shizunet/actions/runs/27436986916/artifacts/7600343173)** *(requires logging into GitHub)*

---

## Key Features

* **Shizuku Integration (`ip r`)**:
  Securely and instantly reads Android routing tables from a privileged shell to automatically resolve the hotspot wireless interface's IP (`ap0`, `wlan1`, or `wlan0`).
* **Sing-Box HTTP Profile Generation**:
  Dynamically builds structured, fully valid, and ready-to-import JSON profiles for Sing-Box client devices.
* **Flexible Rule-Based Routing (GeoIP/Geosite)**:
  Supports customizable target country routing bypass. By default, rules are configured for the Russian segment (`ru`): geo-regions for `ru` (geoip/geosite rules) are bypassed **directly** (`direct`) without going through the proxy, while all other traffic goes through the HTTP proxy.
* **One-Click Imports & QR Codes**:
  * One device generates a QR code with the `sing-box://import?config=...` scheme.
  * The client device (laptop, Android TV, console, PC) connected to your hotspot scans the QR code or copies the link to import the configuration instantly.
* **Modern Jetpack Compose UI**:
  A minimalist, dark theme (Slate-900) interface displaying Shizuku service status, custom settings, and step-by-step console logs.

---

## How it Works (VPN/Proxy Sharing Scenario)

When your Android device is connected to a VPN, Android's default behavior prevents sharing that VPN connection over a Wi-Fi Hotspot with other devices—client devices bypass the VPN and connect directly through your carrier. 

ShizuNet resolves this using the following bypass scheme:
1. Start a local HTTP or Socks proxy server on your phone (such as v2rayNG, Nekobox, Sing-Box, etc.) binding to port `12334`.
2. Turn on your **Mobile Hotspot**.
3. Connect your client devices (smartphones, PC, laptops) to your phone's hotspot Wi-Fi.
4. Launch **ShizuNet** and tap **Auto-Detect Hotspot Gateway IP**.
5. The utility identifies your wireless hotspot gateway (e.g., `192.168.43.1`), pairs it with your specified port (`112334` / `12334`), and instantly generates a config QR code or shareable link.
6. The client scans the generated QR code or imports the link directly into their Sing-Box client application to enjoy full-featured VPN forwarding with selective routing rules!

---

## Step-by-Step Guide

1. **Activate Shizuku**:
   Make sure the [Shizuku](https://shizuku.rikka.app/) service is active on your device (via Wireless Debugging/LADB or ROOT access).
2. **Authorize ShizuNet**:
   Open **ShizuNet** and tap **Grant Shizuku Privilege Shell** (or "Check / Connect Shizuku"). Approve the permission prompt when requested.
3. **Detect Hotspot IP**:
   Enable your phone's hotspot, then tap **Auto-Detect Hotspot Gateway IP**. ShizuNet will resolve the gateway address (e.g., `192.168.43.1` or `192.168.44.1`) and output it to the console log.
4. **Customize Config (Optional)**:
   * Adjust the gateway IP or port (defaults to HTTP port `12334`).
   * Choose/edit your bypass rule country code (e.g., `ru` or `ir`) for direct connections.
5. **Export & Connect**:
   * Tap **Copy Link** for immediate clipboard transfer of the `sing-box://import?...` URI.
   * Tap **Copy JSON** to copy the raw structured configuration.
   * Or simply present the on-screen QR code for client devices to scan directly.

---

## Build & Release (GitHub Actions CI/CD)

The repository includes a ready-to-use continuous integration workflow using GitHub Actions (`.github/workflows/build.yml`).

### Local Compilation
To compile the APK locally via Gradle, execute the following in your root workspace:
```bash
gradle assembleDebug
# Or for release compiling
gradle assembleRelease
```

### GitHub Automated Build Workflow
Upon push or pull request to the `main` or `master` branches, GitHub Actions will:
1. Build the debug version and release packages.
2. Store the compiled standalone APK packages inside compilation build artifacts (named `ShizuNet-build-artifacts`).

### Automated Releases & Signing
To draft full GitHub Releases automatically:

1. **Push a Version Tag**:
   Add and push a tag matching the version pattern `v*` (e.g., `v1.0.0`, `v1.1.2`).
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. **Keystore Signing (Optional)**:
   To sign release versions automatically, supply the following GitHub Repository **Secrets**:
   * `SIGNING_KEY` — Your keystore file encoded in Base64 string.
   * `ALIAS` — Key alias.
   * `KEYSTORE_PASSWORD` — Keystore decryption password.
   * `KEY_PASSWORD` — Key password.
   
   *Note: If these credentials aren't configured, GitHub Actions gracefully uploads and publishes an unsigned package labeled `app-release-unsigned.apk`.*
3. **Draft Release Handling**:
   The workflow generates a pre-formatted **Draft Release** with change reports and compiles. Simply inspect, review, and tap **Publish** inside GitHub Releases.
