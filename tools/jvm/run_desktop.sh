#!/usr/bin/env bash
# Everything cheap, on the laptop, in the order it costs: every engine suite (jc.sh, the Gradle
# cache's compiler), then the static checkers over the app's Kotlin. The gate that settles a build
# is still `.\gradlew :engine:test assembleDebug lintDebug` and tools/verify_manifest.py after it.
# Name from ../squish-android/tools/jvm/run_desktop.sh (12 Nov 2026), which listed each suite's
# files by hand; here the engine is a module and every suite compiles against all of it.
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
fail=0
sh tools/jvm/jc.sh "$@" || fail=1
export PYTHONUTF8=1
for c in check_nesting check_modifier_imports check_dependencies; do
  python tools/$c.py || fail=1
done
[ $fail -eq 0 ] && echo "desktop gate: all passed" || { echo "desktop gate: FAILED (above)"; exit 1; }
