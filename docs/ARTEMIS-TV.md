# Artemis for Android TV: spec

Artemis for Android TV is the TV member of the Artemis apps (the iPhone app
[artemis-ios](https://github.com/daniel-zn/artemis-ios) and the Mac app
[artemis-macos](https://github.com/daniel-zn/artemis-macos)), built on Moonlight for Android and made to pair
with Apollo hosts from [danielzn-Apollo](https://github.com/daniel-zn/danielzn-Apollo). It's driven by a TV
remote first: a D-pad, OK, Back and Home are enough for everything, in the app's screens and on the PC's desktop.
Game controllers work too, and pass through to the PC as gamepads.

Written before the build (2026-11-07) and kept up to date with it.

## What the remote does

### In Artemis's screens

| Remote | Does |
|---|---|
| D-pad | Moves the focus. The focused PC, app or button lifts, with a light ring around it. |
| OK | Opens the focused PC (its apps, pairing, or waking it), starts or resumes the focused app, presses a button. |
| Long-press OK | The focused PC's or app's menu (Wake, Pair, Stop, Hide, Remove, Details…). |
| Back | Back one screen; on a "Connecting…" card, Cancel. |
| Home | The TV's home screen. |

### During a session

A session starts in **Pointer** mode: the D-pad drives the PC's mouse pointer.

| Remote | Pointer (default) | Scroll | Arrow keys |
|---|---|---|---|
| D-pad | Moves the pointer. Tap for a small step; keep pressing or hold to glide, faster the longer it goes. Pushing against the top or bottom edge of the screen scrolls; against the left or right edge, sideways | Up/Down scroll, Left/Right scroll sideways | Arrow keys |
| OK | Left click | Left click | Enter |
| Long-press OK | Right click | Right click | Right click |
| Back | Opens the session menu | ← | ← |
| Home | The TV's home screen; the session ends, the app keeps running on the PC | ← | ← |

When a game on an Apollo host captures the pointer (hides it or holds it in its window; Apollo reports it,
`docs/cursor_state.md` there), the D-pad moves the mouse relatively, as a mouse would turn a game's camera, and the
edges don't scroll. When the game lets go, it's the desktop pointer again.

**The session menu** (Back) has big buttons, focused on Resume:

- **Resume** (Back again does the same)
- **Keyboard**: the TV's on-screen keyboard; what's typed goes to the PC
- **Pointer · Scroll · Arrow keys**: what the D-pad does
- **Hold left button**: the next OK lets go, so the D-pad can drag (windows, selections)
- **Keys**: Esc, Windows (Start menu), Alt+Tab, Ctrl+Alt+Delete
- **Microphone on/off** (when the microphone is on in Settings and the host takes it)
- **Performance stats** on/off
- **Disconnect** (the app keeps running on the PC)
- **Stop *app* on the PC** (asks first)

Nothing ends a session by accident: Back never disconnects, only Disconnect, Stop or Home does.

### Controllers

Game controllers (Bluetooth or USB) pass through to the PC as gamepads: buttons, sticks, triggers, rumble,
motion where the controller has it (upstream Moonlight's support). Upstream's combos stay: hold Start for a
moment to drive the mouse with the sticks, and Start+Select+L1+R1 disconnects. A controller's B / Back go to the
PC; the session menu is the remote's Back.

### Keyboards and mice

A Bluetooth or USB keyboard types on the PC; its arrow keys are arrow keys (only a remote's D-pad drives the
pointer). A mouse is a mouse.

## Features compared with the iPhone and Mac apps

✅ done · ➖ not on the TV (why) · ➕ TV only

| Feature | Mac | iPhone | TV | |
|---|---|---|---|---|
| Name Artemis, Artemis icon, Orbix font | ✅ | ✅ | ✅ the iPhone's icon, a full-tile TV banner (the Artemis art), Orbix with system digits | ✅ |
| Installs beside stock Moonlight | ➖ same bundle id | ✅ | ✅ `com.danielzn.artemis.tv` | ✅ |
| Computers: find (mDNS), add by address, remove, details, test network | ✅ | ✅ | ✅ upstream's | ✅ |
| Pair with Apollo (PIN) | ✅ | ✅ | ✅ the PIN shown large; Apollo's client name is the TV's name | ✅ |
| Per-client permissions, display scale | host | host | host (Apollo web UI › Clients) | ✅ |
| Wake-on-LAN, "Waking up…", opens once it's up, "Couldn't wake" after 90 s | ✅ | ✅ | ✅ | ✅ |
| "Connecting to *PC*…" with a red Cancel that stops every open of that PC; only status polls mark a PC offline | ✅ | ✅ | ✅ | ✅ |
| Apps: Active badge, resume, stop, hide | ✅ | ✅ | ✅ upstream's | ✅ |
| Rename PC, Connect automatically when opening PC | ✅ | ✅ | ➖ not in this first version (no use yet with one PC; upstream Android lacks them) | ➖ |
| Microphone to Apollo (v2, 48 kHz mono Opus, 20 ms) | ✅ | ✅ | ✅ off by default; any microphone Android offers (USB, Bluetooth headset, the TV's own); voice-communication capture with Android's echo cancellation and noise suppression when the TV has them; mute in the session menu | ✅ |
| Shared text clipboard | ✅ | ✅ | ➖ a TV has no text to copy between apps; text goes to the PC by typing (Keyboard) instead | ➖ |
| Automatic mouse mode from Apollo's cursor state | ✅ | ✅ (absolute) | ✅ the remote's pointer goes relative while a game holds the cursor | ✅ |
| Touch controls, on-screen gamepad | ➖ | ✅ | ➖ no touch screen | ➖ |
| Remote pointer, scroll and arrow-key modes, session menu | ➖ | ➖ | ➕ | ➕ |
| Controllers as gamepads | ✅ | ✅ | ✅ | ✅ |
| Settings (resolution, FPS, bitrate, codec, HDR, audio, controllers, host) | ✅ | ✅ | ✅ upstream's, defaulting to the TV's resolution | ✅ |
| Try the Demo | ➖ | App Store only | ➖ App Store only | ➖ |
| Home-screen channel / shortcuts | ➖ | quick actions | ✅ upstream's Android TV channel | ✅ |

## Decisions

- **Fork of moonlight-android** (`daniel-zn/artemis-android-tv`, `main`), code package and translations stay
  upstream's (`com.limelight`); the application id is `com.danielzn.artemis.tv` (debug builds add `.debug`), so it
  installs beside stock Moonlight. English text says Artemis and words things for a remote desktop (apps, PC,
  session); the other languages keep upstream's text. Only the non-root flavour is built.
- **moonlight-common-c with the microphone and cursor-state protocol** is vendored from the Mac fork (as the iPhone
  app does), with enet and nanors kept as submodules.
- **Remote input is decided in plain Java classes** (`com.limelight.artemis.remote`), unit-tested without Android:
  the pointer's motion and acceleration, the edge scrolling, the key mapping per mode and the long-press. The
  projector's Google TV remote is paired with a Raspberry Pi, which sends each press to Android as an injected
  key (down and up together; Left/Right repeat every 100 ms after 400 ms when held, Up/Down don't repeat; a held OK
  arrives as a long press). So a press moves the pointer for a short glide, and presses that keep coming (repeats or
  quick taps) keep it gliding and speed it up. A remote paired with the TV itself sends real holds (repeats until
  the key-up), and the same rules give a smooth glide that stops on release.
- **Which keys are the remote's:** D-pad and OK keys from anything that isn't a game controller or a full
  keyboard. Keys injected by the Pi come from Android's virtual keyboard, so they count as the remote's.
- **Pointer position:** absolute (the stream's coordinates, 1:1 with the PC's screen) while the desktop is free, so
  the pointer can't drift and the edges are known; relative while a game captures the cursor.
- **Pointer speed:** a tap moves 1.5 % of the screen's width; gliding starts at 25 % of the screen per second and
  rises to 110 % per second after about 1.2 s of continuous pressing.
- **Back opens the session menu** rather than ending the session. The menu is a full-height panel on the right,
  over the stream, so the PC's desktop stays visible.
- **Long-press OK = right click**, measured from Android's long-press flag (or repeats past 500 ms on a real hold).
- **Microphone:** `AudioRecord` with `VOICE_COMMUNICATION` (Android's AEC/NS where present), 48 kHz mono 16-bit,
  encoded with the libopus already in the app (64 kbit/s VBR, VOIP, 20 ms, in-band FEC), sent with
  `LiSendMicrophoneOpusDataEx`. It starts only when Apollo negotiated the microphone; without the permission it
  says so once and the session goes on.
- **Clipboard left out** (above). **Try the Demo left out** (App Store only). **Touch controls left out.**
- **Builds and tests on a Linux build machine** (`scripts/remote-build.sh`), never on hosted CI (GitHub Actions is disabled on the
  fork). The APK has armeabi-v7a (the projector), arm64-v8a, x86 and x86_64 native code.
- **"Connecting to *PC*…"** is the stream's connecting popup (upstream opens a PC's app list from its last
  status poll, without connecting first, so that's the only open that waits). Its red Cancel (or Back) ends the
  attempt; a failed or cancelled open never marks the PC offline, only the status polls do (upstream's rule).
- **Waking:** OK on an offline PC with a known MAC address sends Wake-on-LAN and shows "Waking up *PC*…" with a
  red Cancel; the first status poll that sees it online opens it (its apps, or pairing); after 90 s, "Couldn't
  wake the PC". Leaving the PC list stops waiting. The PC's long-press menu still has Wake-on-LAN on its own.
- **Release step:** none in integrate. There's no store; the APK is installed on the TV by hand (`adb install -r`)
  or downloaded from the repository's GitHub releases (asset `Artemis-TV.apk`, the release build).
  Tags are `tv-<versionName>` (first: `tv-1.0`), because upstream's `v10.x` tags are in the repository too. Each
  new release keeps the asset name `Artemis-TV.apk`, so `releases/latest/download/Artemis-TV.apk` always gets the newest
  one. Raise versionCode for every release, and scan the built APK for personal details before uploading.
- **Signing:** one permanent release key (the key the first installs were signed with, so updates install over
  them). It lives on the build machine and reaches Gradle as properties (`artemisTvKeystore`,
  `artemisTvKeystorePassword`, `artemisTvKeyAlias`, `artemisTvKeyPassword` in `~/.gradle/gradle.properties`);
  without them the release APK is left unsigned. The release build isn't debuggable. Version 1.0 (code 316, above
  upstream's 315).
- **TV banner:** full-bleed like an Apple TV tile: `art/artemis-banner-wide.jpg` cropped to 16:9 (the full
  figure, centred) by `tools/banner-from-image.sh`, 320 × 180 and 640 × 360, no name or corners (the launcher adds
  them). The launcher icon stays the iPhone's icon (`tools/icon-images.swift`).
- **The PC list's side buttons** (Settings, Help, Add PC) get the same gold focus ring as the PC tiles.

## Tests

- **Unit tests** (`app/src/test`, plain JUnit, no Android): the pointer's motion and acceleration
  (`PointerMotionTest`), the remote's keys per mode, clicks, long press, edge scrolling, hold-left, captured
  cursor (`RemoteControllerTest`), which keys are the remote's (`RemoteKeysTest`), the session menu's key combos
  (`KeyCombosTest`), waking (`WakeFlowTest`).
- **Emulator check** (`scripts/emulator-check.sh`, on an Android TV 14 emulator): the debug build's
  `ArtemisDemoSession` (the remote and session menu with no PC; what would go to the PC is logged) is driven with
  injected keys, as the TV's remote bridge sends them: a step from the middle, click, long press = right click,
  glide, Back opens the menu on Resume, the D-pad moves the menu's focus and not the pointer, Scroll mode, Back
  resumes, Disconnect, captured-cursor relative motion; and the PC list opens with the focus on something, and a
  focused side button wears the gold ring (gold pixels counted in a screenshot).
- `.mercurius/integrate.json` runs `scripts/check.sh build|unit|emulator`.
