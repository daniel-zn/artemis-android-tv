#!/bin/bash
# Runs a command in a copy of this checkout on another machine (a Linux build box with the Android SDK),
# so Gradle and the NDK don't have to run here. The machine is $REMOTE_BUILD_HOST (an ssh destination)
# and the copy lives in ~/integrate/<repo> there; build outputs and Gradle's caches stay between runs.
#   REMOTE_BUILD_HOST=builder scripts/remote-build.sh ./gradlew --no-daemon :app:assembleDebug
# REMOTE_ANDROID_HOME is the SDK there (default /opt/android-sdk).
# REMOTE_BUILD_WSL=<distro>: the machine is Windows and the SDK lives in that WSL distro (ssh lands in cmd.exe,
# so every command, rsync included, goes through wsl.exe, and the command itself over stdin).
# REMOTE_BUILD_FETCH="app/build/outputs/apk …": those paths (relative to the checkout) come back here
# after the command, whatever its exit code.
set -euo pipefail
host=${REMOTE_BUILD_HOST:-${MERCURIUS_MACHINE_HOST:-}}
[ -n "$host" ] || { echo "remote-build: set REMOTE_BUILD_HOST" >&2; exit 2; }
sdk=${REMOTE_ANDROID_HOME:-/opt/android-sdk}
root=$(cd "$(dirname "$0")/.." && pwd)
name="$(basename "$(git -C "$root" remote get-url origin 2>/dev/null || echo "$root")" .git)"
dir="integrate/$name"
ssh=(ssh -o BatchMode=yes -o ConnectTimeout=15 -o ServerAliveInterval=30)
wsl=; rsync_path=()
if [ -n "${REMOTE_BUILD_WSL:-}" ]; then
  wsl="wsl.exe -d $REMOTE_BUILD_WSL --cd ~ --"
  rsync_path=(--rsync-path="$wsl rsync")
fi
# A script for the machine's shell: over stdin, so cmd.exe never sees its quotes; bash reads the whole { } block
# before running it, and </dev/null keeps the command's own programs (adb install…) from eating the script.
remote() { printf '{ %s\n} </dev/null\n' "$1" | "${ssh[@]}" "$host" "${wsl:+$wsl }bash -l -s"; }
# The copy from before the rename (~/land/<repo>, 2026-11-13) moves here once; CMake's caches in it
# name the old place, so they go (rebuilt on the next build).
remote "if [ -d 'land/$name' ] && [ ! -e '$dir' ]; then mkdir -p integrate && mv 'land/$name' '$dir' && find '$dir' -name .cxx -type d -prune -exec rm -rf {} +; rmdir land 2>/dev/null; fi; mkdir -p '$dir'"
rsync -a --delete -e "${ssh[*]}" ${rsync_path[@]+"${rsync_path[@]}"} --exclude .git/ --exclude build/ --exclude .gradle/ \
  --exclude .cxx/ --exclude local.properties "$root/" "$host:$dir/"
code=0
remote "cd '$dir' && export MERCURIUS_MACHINE_RUN='${MERCURIUS_MACHINE_RUN:-}' ANDROID_HOME='$sdk' ANDROID_SDK_ROOT='$sdk' && $*" || code=$?
for p in ${REMOTE_BUILD_FETCH:-}; do
  mkdir -p "$root/$(dirname "$p")"
  rsync -a -e "${ssh[*]}" ${rsync_path[@]+"${rsync_path[@]}"} "$host:$dir/$p" "$root/$(dirname "$p")/" \
    || echo "remote-build: couldn't bring back $p" >&2
done
exit $code
