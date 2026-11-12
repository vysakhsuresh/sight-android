#!/usr/bin/env bash
# The laptop's quick runner: compiles engine/ and the suites with the Kotlin compiler already in
# the Gradle cache (nothing fetched) and runs the named suites, or all of them.
#   sh tools/jvm/jc.sh                 # every suite
#   sh tools/jvm/jc.sh fallback light  # those suites
#   IKSHANA_ENGINE_SRC=<scratch copy> sh tools/jvm/jc.sh fallback   # a negative test
# Shape from ../squish-android/tools/jvm/jc.sh (12 Nov 2026), which pinned the jars by hash; this
# one finds them by version, because the hash folders differ from machine to machine.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ARGS=("$@")  # kept: the jar loop below reuses the positional parameters
KV=2.2.21
C="$HOME/.gradle/caches/modules-2/files-2.1"
# The Gradle cache first, then the jars run.sh fetches (the cache need not hold kotlin-reflect at
# the compiler's version; ~/.stave-kt is the sibling's copy of the same 2.2.21 set). A failed
# lookup must say so: under pipefail a find over a missing folder used to end the script silently.
jar() {
  local hit
  hit="$(find "$C/$1/$2/$3" -name "$2-$3.jar" 2>/dev/null | head -1 || true)"
  if [ -z "$hit" ]; then
    for d in "${IKSHANA_KT_HOME:-$HOME/.ikshana-kt}" "$HOME/.stave-kt"; do
      case "$2" in kotlinx-coroutines-core-jvm) f="$d/coroutines.jar";; *) f="$d/$2.jar";; esac
      if [ -s "$f" ]; then hit="$f"; break; fi
    done
  fi
  printf %s "$hit"
}
JAVA="${JAVA_HOME:-$HOME/.jdks/jbr-21.0.11}/bin/java"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";"; P() { cygpath -m "$1"; } ;; *) SEP=":"; P() { printf %s "$1"; } ;; esac
CP=""
for spec in "org.jetbrains.kotlin kotlin-compiler-embeddable $KV" "org.jetbrains.kotlin kotlin-stdlib $KV" \
            "org.jetbrains.kotlin kotlin-script-runtime $KV" "org.jetbrains.kotlin kotlin-reflect $KV" \
            "org.jetbrains.kotlin kotlin-daemon-embeddable $KV" "org.jetbrains.intellij.deps trove4j 1.0.20200330" \
            "org.jetbrains annotations 13.0" "org.jetbrains.kotlinx kotlinx-coroutines-core-jvm 1.10.2"; do
  set -- $spec; j="$(jar "$1" "$2" "$3")"
  [ -n "$j" ] || { echo "$2 $3 is not in the Gradle cache: run ./gradlew :engine:test once, or use run.sh"; exit 1; }
  CP="$CP${CP:+$SEP}$(P "$j")"
  [ "$2" = kotlin-stdlib ] && STD="$(P "$j")"
done
OUT="${IKSHANA_JVM_OUT:-${TMPDIR:-/tmp}/ikshana-jvm}"
ENGINE="${IKSHANA_ENGINE_SRC:-$ROOT/engine/src/main/kotlin}"
rm -rf "$OUT"; mkdir -p "$OUT"
if grep -rn "^import android" "$ENGINE"; then echo "engine/ must not import android.*"; exit 1; fi
"$JAVA" -cp "$CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -nowarn -cp "$STD" -d "$(P "$OUT")" \
  $(find "$ENGINE" -name '*.kt') $(find "$ROOT/tools/jvm" -name '*.kt' ! -name 'EngineSuitesTest.kt') 2>&1 | grep -v "^Picked up" || true
[ -f "$OUT/MainKt.class" ] || { echo "compile failed"; exit 1; }
cd "$ROOT" && "$JAVA" -cp "$(P "$OUT")$SEP$STD" MainKt "${ARGS[@]}"
