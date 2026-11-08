#!/bin/bash
# Screenshots of the screens that need no PC (for looking at layout, focus and text at TV size): the launcher row
# with the banner, the PC list, Settings, Add PC, and an unreachable PC's menu. Runs on the emulator like
# emulator-check.sh (same ARTEMIS_EMULATOR_START / STOP / ADB_SERIAL); shots and ui dumps in build/ui-walk/.
set -uo pipefail
cd "$(dirname "$0")/.."
adb=${ANDROID_HOME:+$ANDROID_HOME/platform-tools/}adb
out=build/ui-walk; rm -rf "$out"; mkdir -p "$out"
started=0
stop() { [ "$started" = 1 ] && [ -n "${ARTEMIS_EMULATOR_STOP:-}" ] && sh -c "$ARTEMIS_EMULATOR_STOP" >/dev/null 2>&1; true; }
trap stop EXIT
if [ -n "${ARTEMIS_EMULATOR_START:-}" ]; then sh -c "$ARTEMIS_EMULATOR_START" || exit 1; started=1; fi
serial=${ARTEMIS_ADB_SERIAL:-}
for _ in $(seq 1 90); do
  [ -n "$serial" ] || serial=$("$adb" devices | awk 'NR>1 && $2=="device" {print $1; exit}')
  [ -n "$serial" ] && [ "$("$adb" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ] && break
  sleep 2
done
a() { "$adb" -s "$serial" "$@"; }
pkg=com.danielzn.artemis.tv.debug
apk=$(find . -name 'app-debug.apk' -not -path './.gradle/*' | head -1)
a uninstall $pkg >/dev/null 2>&1; a install "$apk" >/dev/null || { echo "install failed"; exit 1; }
a shell input keyevent KEYCODE_WAKEUP
n=0
shot() { n=$((n + 1)); local f; f=$(printf '%02d-%s' $n "$1"); a exec-out screencap -p > "$out/$f.png"
  a shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; a shell cat /sdcard/ui.xml > "$out/$f.xml"
  echo "$f: $(a shell dumpsys window | grep mCurrentFocus | tr -d '\r' | sed 's/.*{//; s/}.*//')"; }
key() { for k in "$@"; do a shell input keyevent "$k"; sleep 0.6; done; }
key KEYCODE_HOME; sleep 3; shot launcher
a shell am start -W -n $pkg/com.limelight.PcView >/dev/null; sleep 4; shot pc-list
key KEYCODE_DPAD_DOWN; shot pc-list-down
key KEYCODE_DPAD_UP KEYCODE_DPAD_UP KEYCODE_DPAD_UP; shot pc-list-top
# Settings (the gear is the first of the side buttons)
a shell am start -W -n $pkg/com.limelight.preferences.StreamSettings >/dev/null; sleep 2; shot settings
key KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN KEYCODE_DPAD_DOWN; shot settings-down
for _ in $(seq 1 25); do a shell input keyevent KEYCODE_DPAD_DOWN; done; sleep 1; shot settings-end
key KEYCODE_BACK; sleep 1; shot back-from-settings
# Add PC by hand: an address nobody answers
a shell am start -W -n $pkg/com.limelight.preferences.AddComputerManually >/dev/null; sleep 2; shot add-pc
a shell input text 192.0.2.1; sleep 0.5; shot add-pc-typed
key KEYCODE_ENTER; sleep 12; shot add-pc-result
key KEYCODE_BACK; sleep 1; shot back-from-add
key KEYCODE_BACK; sleep 1; shot back-twice
echo "ui-walk: $n shots in $out"
