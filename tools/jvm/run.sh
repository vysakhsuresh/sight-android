#!/usr/bin/env bash
# Copied from ../stave-android/tools/jvm/run.sh (12 Nov 2026). Changed: the classpath separator
# and paths follow the platform (Stave's CLAUDE.md: on Windows the separator must be ';'), and
# the environment names are IKSHANA_*.
# Compiles the pure engine (engine/src/main/kotlin) together with every suite under tools/jvm
# using the standalone Kotlin compiler - NO Android classes on the path - and runs the suites.
#
# What this proves on every run, on any machine with a JDK and no Android SDK:
#   1. Nothing under engine/ imports android.* - the compile fails if it does. The engine is
#      where every number the app shows and every sample it changes is decided; keeping it pure
#      is what lets those decisions be executed in seconds rather than on a phone.
#   2. Every suite passes. A suite is an object implementing Suite (tools/jvm/Checks.kt) and is
#      listed in Registry; RunnerChecks fails if a *Checks.kt file exists that Registry does not
#      name, because a suite nothing runs reads as cover and is none.
#
#   sh tools/jvm/run.sh            # every suite
#   sh tools/jvm/run.sh fft eq     # the named suites only (by Suite.name)
#   sh tools/jvm/run.sh --list
#
# Jars are fetched once from Maven Central into ~/.ikshana-kt (IKSHANA_KT_HOME overrides).
# The desktop runs the same suites through Gradle: ./gradlew :engine:test.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
K="${IKSHANA_KT_HOME:-$HOME/.ikshana-kt}"
KV=2.2.21
MC=https://repo.maven.apache.org/maven2
mkdir -p "$K"
is_jar() { [ -s "$1" ] && [ "$(head -c 2 "$1")" = "PK" ]; }
fetch() { # name path
  if ! is_jar "$K/$1"; then
    for try in 1 2 3 4 5; do
      if curl -sSL -f -o "$K/$1.part" "$MC/$2" && is_jar "$K/$1.part"; then mv "$K/$1.part" "$K/$1"; break; fi
      sleep $((try * 5))
    done
  fi
  is_jar "$K/$1" || { echo "could not fetch $1"; exit 1; }
}
fetch kotlin-compiler-embeddable.jar "org/jetbrains/kotlin/kotlin-compiler-embeddable/$KV/kotlin-compiler-embeddable-$KV.jar"
fetch kotlin-stdlib.jar "org/jetbrains/kotlin/kotlin-stdlib/$KV/kotlin-stdlib-$KV.jar"
fetch kotlin-script-runtime.jar "org/jetbrains/kotlin/kotlin-script-runtime/$KV/kotlin-script-runtime-$KV.jar"
fetch kotlin-reflect.jar "org/jetbrains/kotlin/kotlin-reflect/$KV/kotlin-reflect-$KV.jar"
fetch kotlin-daemon-embeddable.jar "org/jetbrains/kotlin/kotlin-daemon-embeddable/$KV/kotlin-daemon-embeddable-$KV.jar"
fetch trove4j.jar "org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar"
fetch annotations.jar "org/jetbrains/annotations/13.0/annotations-13.0.jar"
fetch coroutines.jar "org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.2/kotlinx-coroutines-core-jvm-1.10.2.jar"

# Java on Windows takes ';' between classpath entries and Windows paths; Git Bash hands it ':'
# and /c/... unless told. Everything java sees goes through P.
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";"; P() { cygpath -m "$1"; } ;; *) SEP=":"; P() { printf %s "$1"; } ;; esac
KP="$(P "$K")"
# java from JAVA_HOME when it is set: on the laptop Git Bash has no java on its PATH.
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
COMPILER_CP="$KP/kotlin-compiler-embeddable.jar${SEP}$KP/kotlin-stdlib.jar${SEP}$KP/kotlin-script-runtime.jar${SEP}$KP/kotlin-reflect.jar${SEP}$KP/kotlin-daemon-embeddable.jar${SEP}$KP/trove4j.jar${SEP}$KP/annotations.jar${SEP}$KP/coroutines.jar"
OUT="${IKSHANA_JVM_OUT:-$ROOT/tools/out}"
rm -rf "$OUT" && mkdir -p "$OUT"

ENGINE="${IKSHANA_ENGINE_SRC:-$ROOT/engine/src/main/kotlin}"
SUITES="$ROOT/tools/jvm"

if grep -rn "^import android" "$ENGINE"; then
  echo "engine/ must not import android.* (see above)"; exit 1
fi

echo "compiling engine/ and the suites"
# The JUnit shim in tools/jvm is for Gradle; here the suites are driven by MainKt.
"$JAVA" -cp "$COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -nowarn -cp "$KP/kotlin-stdlib.jar" -d "$(P "$OUT")" \
  $(find "$ENGINE" -name '*.kt') $(find "$SUITES" -name '*.kt' ! -name 'EngineSuitesTest.kt') 2>&1 | grep -v JAVA_TOOL_OPTIONS || true
[ -f "$OUT/MainKt.class" ] || { echo "compile failed"; exit 1; }

(cd "$ROOT" && "$JAVA" -cp "$(P "$OUT")${SEP}$KP/kotlin-stdlib.jar" MainKt "$@") 2>&1 | grep -v JAVA_TOOL_OPTIONS
