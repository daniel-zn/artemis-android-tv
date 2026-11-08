# Artemis for Android TV

Artemis for Android TV streams a PC's desktop, apps and games to an Android TV or Google TV, driven by the TV's
remote. It's the TV member of the Artemis apps ([iPhone](https://github.com/daniel-zn/artemis-ios),
[Mac](https://github.com/daniel-zn/artemis-macos)), made for [Apollo](https://github.com/daniel-zn/danielzn-Apollo)
hosts, and a fork of Moonlight for Android (below), under the same GPL v3 licence.

## About this fork

- **The remote drives the PC.** During a session the D-pad moves the PC's pointer (tap for a step, keep pressing
  to glide faster; push against an edge to scroll), OK clicks, a long press of OK right-clicks, and Back opens a
  session menu: Resume, Keyboard, Pointer / Scroll / Arrow keys, Hold left button, Esc / Windows / Alt+Tab /
  Ctrl+Alt+Delete, Microphone, Performance stats, Disconnect, Stop the app. When a game captures the cursor
  (Apollo's cursor state), the D-pad moves the mouse relatively. Controllers pass through as gamepads.
- **Apollo features:** microphone passthrough (Opus, 48 kHz mono, off by default in Settings), automatic mouse
  mode from the cursor state; per-client permissions and display scale are set on the host.
- **Wake-on-LAN:** OK on an offline PC wakes it ("Waking up…") and opens it once it's up.
- **"Connecting to *PC*…"** with a red Cancel.
- Artemis's name, icon, TV banner, navy and gold look and Orbix font; installs beside Moonlight
  (`com.danielzn.artemis.tv`). Only the non-root build.

The full spec, with what's left out and why: [docs/ARTEMIS-TV.md](docs/ARTEMIS-TV.md).

**Building:** `./gradlew :app:assembleDebug` with the Android SDK and NDK 27 (`ndkVersion` in `app/build.gradle`).
`scripts/remote-build.sh` runs Gradle on another machine with the SDK; `scripts/check.sh build|unit|emulator` are
the checks (`scripts/emulator-check.sh` drives the remote on an Android TV emulator). GitHub Actions is off for
this fork.

---

# Moonlight Android

[![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/232a8tadrrn8jv0k/branch/master?svg=true)](https://ci.appveyor.com/project/cgutman/moonlight-android/branch/master)
[![Translation Status](https://hosted.weblate.org/widgets/moonlight/-/moonlight-android/svg-badge.svg)](https://hosted.weblate.org/projects/moonlight/moonlight-android/)

[Moonlight for Android](https://moonlight-stream.org) is an open source client for NVIDIA GameStream and [Sunshine](https://github.com/LizardByte/Sunshine).

Moonlight for Android will allow you to stream your full collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Moonlight also has a [PC client](https://github.com/moonlight-stream/moonlight-qt) and [iOS/tvOS client](https://github.com/moonlight-stream/moonlight-ios).

You can follow development on our [Discord server](https://moonlight-stream.org/discord) and help translate Moonlight into your language on [Weblate](https://hosted.weblate.org/projects/moonlight/moonlight-android/).

## Downloads
* [Google Play Store](https://play.google.com/store/apps/details?id=com.limelight)
* [Amazon App Store](https://www.amazon.com/gp/product/B00JK4MFN2)
* [F-Droid](https://f-droid.org/packages/com.limelight)
* [APK](https://github.com/moonlight-stream/moonlight-android/releases)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).
