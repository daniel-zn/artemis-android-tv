#!/bin/bash
# The emulator check: installs the debug APK on an Android TV emulator and drives the remote with injected keys
# (as a TV remote bridge sends them), reading what the app would send to the PC from logcat (the debug-only
# ArtemisDemoSession logs it under "ArtemisRemote"). Also opens the app's PC list. Screenshots go to
# build/emulator-check/.
#   ARTEMIS_EMULATOR_START / ARTEMIS_EMULATOR_STOP: commands that start and stop the emulator (optional; without
#   them an already running emulator is used). ARTEMIS_ADB_SERIAL: which device (default: the only one).
set -uo pipefail
cd "$(dirname "$0")/.."
adb=${ANDROID_HOME:+$ANDROID_HOME/platform-tools/}adb
out=build/emulator-check
mkdir -p "$out"
started=0
stop() {
  if [ "$started" = 1 ] && [ -n "${ARTEMIS_EMULATOR_STOP:-}" ]; then sh -c "$ARTEMIS_EMULATOR_STOP" >/dev/null 2>&1; fi
}
trap stop EXIT
if [ -n "${ARTEMIS_EMULATOR_START:-}" ]; then
  sh -c "$ARTEMIS_EMULATOR_START" || { echo "emulator-check: couldn't start the emulator"; exit 1; }
  started=1
fi
serial=${ARTEMIS_ADB_SERIAL:-}
for _ in $(seq 1 90); do
  [ -n "$serial" ] || serial=$("$adb" devices | awk 'NR>1 && $2=="device" {print $1; exit}')
  [ -n "$serial" ] && [ "$("$adb" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
  sleep 2
done
[ -n "$serial" ] || { echo "emulator-check: no device"; exit 1; }
a() { "$adb" -s "$serial" "$@"; }
apk=$(find . -name 'app-debug.apk' -not -path './.gradle/*' | head -1)
[ -n "$apk" ] || { echo "emulator-check: no app-debug.apk (build it first)"; exit 1; }
pkg=com.danielzn.artemis.tv.debug
a install -r "$apk" >/dev/null || { echo "emulator-check: install failed"; exit 1; }
# Keep the emulator awake; no sound needed
a shell input keyevent KEYCODE_WAKEUP

fails=0
check() { # name, then the lines expected (each a regex, in order) in the log since the last mark
  local name=$1; shift
  local log; log=$(a logcat -d -s ArtemisRemote:I | sed -n 's/.*ArtemisRemote: //p' | tr -d '\r')
  local rest=$log ok=1 want
  for want in "$@"; do
    local line; line=$(printf '%s\n' "$rest" | grep -n -E -m1 "^($want)\$" | cut -d: -f1)
    if [ -z "$line" ]; then ok=0; break; fi
    rest=$(printf '%s\n' "$rest" | tail -n +"$((line + 1))")
  done
  if [ $ok = 1 ]; then echo "PASS $name"; else
    echo "FAIL $name: wanted [$*] in order, got:"; printf '%s\n' "$log" | sed 's/^/    /'; fails=$((fails + 1)); fi
}
absent() { # name, regex that must not be in the log
  if a logcat -d -s ArtemisRemote:I | grep -q -E "ArtemisRemote: ($2)"; then echo "FAIL $1: saw $2"; fails=$((fails + 1)); else echo "PASS $1"; fi
}
mark() { a logcat -c; }
key() { a shell input keyevent "$@"; sleep 0.4; }
focused() { # the focused view's text, from uiautomator
  a shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  a shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'focused="true"' | grep -o 'text="[^"]*"' | head -1 | sed 's/text="//; s/"$//'
}
focused_id() { # the focused view's resource id (without the package)
  a shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  a shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'focused="true"' | grep -o 'resource-id="[^"]*"' | head -1 | sed 's/.*:id\///; s/"$//'
}
bounds_of() { # "x1 y1 x2 y2" of the view with that id
  a shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  a shell cat /sdcard/ui.xml | tr '>' '\n' | grep "id/$1\"" | grep -o 'bounds="[^"]*"' | head -1 | tr -c '0-9\n' ' '
}
gold_in() { # how many of the screen's pixels in "x1 y1 x2 y2" are Artemis gold (#E8B84A, give or take)
  [ -n "$1" ] || { echo 0; return; }
  a exec-out screencap > "$out/raw.bin"
  python3 -I - "$out/raw.bin" $1 <<'PY'
import sys, struct
d = open(sys.argv[1], 'rb').read(); w, h, f = struct.unpack('<III', d[:12])
off = 16 if len(d) - 16 == w * h * 4 else 12
x1, y1, x2, y2 = map(int, sys.argv[2:6]); n = 0
for y in range(max(0, y1 - 8), min(h, y2 + 8)):
    row = off + y * w * 4
    for x in range(max(0, x1 - 8), min(w, x2 + 8)):
        r, g, b = d[row + x * 4], d[row + x * 4 + 1], d[row + x * 4 + 2]
        if abs(r - 0xE8) < 40 and abs(g - 0xB8) < 40 and abs(b - 0x4A) < 50: n += 1
print(n)
PY
}
demo() {
  a shell am force-stop $pkg
  mark
  a shell am start -W -n $pkg/com.limelight.artemis.ArtemisDemoSession "$@" >/dev/null
  for _ in $(seq 1 20); do a logcat -d -s ArtemisRemote:I | grep -q "demo ready" && break; sleep 0.5; done
  sleep 0.5
  mark
}

demo
key KEYCODE_DPAD_RIGHT
check "a tap moves the pointer one step from the middle" "moveTo 989 540"
mark; key KEYCODE_DPAD_CENTER
check "OK is a left click" "left down" "left up"
mark; a shell input keyevent --longpress KEYCODE_DPAD_CENTER; sleep 0.5
check "a long press of OK is a right click" "right down" "right up"
absent "a long press isn't also a left click" "left (down|up)"
mark; for _ in 1 2 3 4 5 6 7 8; do a shell input keyevent KEYCODE_DPAD_DOWN; done; sleep 0.5
check "presses that keep coming glide" "moveTo 989 [0-9]+" "moveTo 989 [0-9]+" "moveTo 989 [0-9]+"
mark; key KEYCODE_BACK
check "Back opens the session menu" "menu" "menu shown"
a exec-out screencap -p > "$out/menu.png"
f=$(focused); [ "$f" = "Resume" ] && echo "PASS the menu opens on Resume" || { echo "FAIL the menu opens on Resume (focused: $f)"; fails=$((fails + 1)); }
key KEYCODE_DPAD_DOWN
f=$(focused); [ "$f" = "Keyboard" ] && echo "PASS the D-pad moves the menu's focus" || { echo "FAIL the D-pad moves the menu's focus (focused: $f)"; fails=$((fails + 1)); }
absent "the D-pad doesn't move the pointer while the menu is up" "moveTo .*"
key KEYCODE_DPAD_DOWN
f=$(focused)
if [ "$f" = "Pointer" ]; then key KEYCODE_DPAD_RIGHT; fi
f=$(focused); [ "$f" = "Scroll" ] && echo "PASS Scroll is next to Pointer" || { echo "FAIL Scroll is next to Pointer (focused: $f)"; fails=$((fails + 1)); }
mark; key KEYCODE_DPAD_CENTER
check "choosing Scroll sets the mode and closes the menu" "action mode SCROLL" "menu hidden"
mark; key KEYCODE_DPAD_UP
check "in Scroll mode Up scrolls up" "scroll 120"
mark; key KEYCODE_BACK; key KEYCODE_BACK
check "Back again resumes" "menu shown" "menu hidden"
# Disconnect: down until it's focused
mark; key KEYCODE_BACK
for _ in $(seq 1 14); do [ "$(focused)" = "Disconnect" ] && break; a shell input keyevent KEYCODE_DPAD_DOWN; sleep 0.3; done
key KEYCODE_DPAD_CENTER
check "Disconnect from the menu" "action disconnect"

demo --ez captured true
key KEYCODE_DPAD_LEFT
check "a captured cursor moves relatively" "moveBy -28 0"

# The PC list opens and takes the focus
a shell am force-stop $pkg
a shell am start -W -n $pkg/com.limelight.PcView >/dev/null; sleep 3
win=$(a shell dumpsys window | grep mCurrentFocus | tr -d '\r')
case "$win" in *com.limelight.PcView*) echo "PASS the PC list opens";; *) echo "FAIL the PC list opens ($win)"; fails=$((fails + 1));; esac
a exec-out screencap -p > "$out/pc-list.png"
# Something has the focus at once (a TV shows nothing until a press otherwise), and a focused side button wears the
# gold ring like the PC tiles (Android's grey highlight is hard to see from the sofa).
fid=$(focused_id); [ -n "$fid" ] && echo "PASS the PC list opens with the focus on something ($fid)" || {
  echo "FAIL the PC list opens with the focus on something (nothing focused)"; fails=$((fails + 1)); }
a shell input keyevent KEYCODE_DPAD_LEFT; sleep 0.3; for _ in 1 2 3; do a shell input keyevent KEYCODE_DPAD_UP; done; sleep 0.6
fid=$(focused_id)
g=$(gold_in "$(bounds_of settingsButton)")
if [ "$fid" = settingsButton ] && [ "${g:-0}" -ge 200 ]; then echo "PASS a focused side button is ringed in gold ($g gold pixels)"
else echo "FAIL a focused side button is ringed in gold (focused: $fid, ${g:-0} gold pixels)"; fails=$((fails + 1)); fi
a exec-out screencap -p > "$out/pc-list-settings.png"
# Settings: the resolution list starts with Match this TV, naming the TV's mode
key KEYCODE_DPAD_CENTER; sleep 2; key KEYCODE_DPAD_CENTER; sleep 1
a shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
mt=$(a shell cat /sdcard/ui.xml | tr '>' '\n' | grep -o 'text="Match this TV ([^"]*)"' | head -1 | sed 's/text="//; s/"$//')
a exec-out screencap -p > "$out/settings-resolution.png"
[ -n "$mt" ] && echo "PASS the resolution list offers $mt" || { echo "FAIL the resolution list offers Match this TV"; fails=$((fails + 1)); }
a shell am force-stop $pkg

# Match this TV: the launch request asks for the TV's own mode (Apollo makes its virtual display at it), for a new
# install and for one still on upstream's 720p default; a chosen mode stays. The TV's mode, read independently:
size=$(a shell wm size | tr -d '\r' | sed -n 's/^Physical size: //p')
hz=$(a shell dumpsys SurfaceFlinger | tr -d '\r' | sed -n 's/.*refresh-rate *: *\([0-9.]*\).*/\1/p' | head -1)
want=$(python3 -I -c '
import sys
w, h = sorted(map(int, sys.argv[1].split("x")), reverse=True); hz = float(sys.argv[2] or 60)
s = next(((a, b) for a, b in [(3840, 2160), (2560, 1440), (1920, 1080), (1280, 720)] if a <= w and b <= h), (1280, 720))
r = next((r for r in [24, 25, 30, 48, 50, 60, 72, 75, 90, 100, 120] if abs(hz - r) <= r * 0.02), round(hz))
f = 60 if r < 48 else min(r, 120); ff = (f if f <= 60 else (f / 60) ** 0.5 * 60) / 30
print("%dx%dx%d %d" % (s[0], s[1], f, round({720: 5, 1080: 10, 1440: 20, 2160: 40}[s[1]] * ff) * 1000))' \
  "${size:-1920x1080}" "${hz:-60}")
wantbr=${want#* }; want=${want% *}
probe() {
  a shell am force-stop $pkg; mark
  a shell am start -W -n $pkg/com.limelight.artemis.ArtemisModeProbe --es seed "$1" >/dev/null
  for _ in $(seq 1 20); do a logcat -d -s ArtemisRemote:I | grep -q "launch mode=" && break; sleep 0.5; done
}
echo "the TV: ${size:-?} at ${hz:-?} Hz, so $want"
probe fresh
check "a new install's launch request asks for the TV's mode ($want, $wantbr kbps)" "launch mode=$want bitrate=$wantbr"
probe old
check "an install on upstream's 720p default moves to the TV's mode and bitrate" "launch mode=$want bitrate=$wantbr"
probe manual
check "a chosen mode stays (720p at 30 FPS, its own bitrate)" "launch mode=1280x720x30 bitrate=5000"
a shell am force-stop $pkg

echo "emulator-check: $fails failed"
[ $fails = 0 ]
