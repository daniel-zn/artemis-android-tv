#!/bin/bash
# The checks .mercurius/integrate.json runs: build, unit, emulator. Each runs on the Android build machine through
# Mercurius (`mercurius-agent machine run`), in a copy of the checkout (scripts/remote-build.sh).
# Which machine, and how to start its emulator, come from the environment or from .mercurius/local.env in the
# main checkout (not in git), for example:
#   ARTEMIS_BUILD_MACHINE=builder   REMOTE_BUILD_WSL=Debian (a Windows machine: see remote-build.sh)
#   ARTEMIS_EMULATOR_START='emu start tv'   ARTEMIS_EMULATOR_STOP='emu stop tv'
# ARTEMIS_ONLY=cards runs only the emulator check's app-grid cards.
set -euo pipefail
cd "$(dirname "$0")/.."
for f in "${INTEGRATE_SOURCE:-.}/.mercurius/local.env" .mercurius/local.env; do
  [ -f "$f" ] && { set -a; . "$f"; set +a; break; }
done
machine=${ARTEMIS_BUILD_MACHINE:?set ARTEMIS_BUILD_MACHINE (or .mercurius/local.env)}
gradle='./gradlew --no-daemon -q'
case "${1:-}" in
  build) cmd="$gradle :app:assembleDebug :app:assembleRelease" ;;
  unit) cmd="$gradle :app:testDebugUnitTest" ;;
  emulator) cmd="$gradle :app:assembleDebug && ARTEMIS_EMULATOR_START=$(printf %q "${ARTEMIS_EMULATOR_START:-}") ARTEMIS_EMULATOR_STOP=$(printf %q "${ARTEMIS_EMULATOR_STOP:-}") ARTEMIS_ONLY=$(printf %q "${ARTEMIS_ONLY:-}") scripts/emulator-check.sh"
    export REMOTE_BUILD_FETCH=build/emulator-check ;;
  *) echo "usage: scripts/check.sh build|unit|emulator" >&2; exit 2 ;;
esac
# Every run uses the one copy there (remote-build.sh): runs of this repo take turns on its own line, so a
# second one doesn't sync over the first one's build (the machine admits several jobs at once).
t=${ARTEMIS_CHECK_TIMEOUT:-50}
exec mercurius-agent turn run "artemis-android-tv-$machine" --reason "Artemis TV: $1 check" --timeout $((t + 15)) -- \
  mercurius-agent machine run "$machine" --local --timeout "$t" --reason "Artemis TV: $1 check" -- scripts/remote-build.sh "$cmd"
