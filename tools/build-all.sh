#!/usr/bin/env bash
# Builds and checks every edition. Usage: tools/build-all.sh [target ...]
# Targets: domain forge-1.20.1 neoforge-1.21.1 neoforge-26.1.2 neoforge-26.2 (default: all).
# Compile JDKs (17/21/25) are picked by Gradle toolchains per target; set JAVA_HOME to any JDK 17+
# to launch Gradle. Exits non-zero and names every target that failed.
set -u
root="$(cd "$(dirname "$0")/.." && pwd)"
targets=("$@")
[ ${#targets[@]} -eq 0 ] && targets=(domain forge-1.20.1 neoforge-1.21.1 neoforge-26.1.2 neoforge-26.2)
failed=()

run() { # name dir gradle-args...
  local name="$1" dir="$2"; shift 2
  echo "=== $name: ./gradlew $*"
  if ! (cd "$dir" && ./gradlew --no-daemon "$@"); then failed+=("$name"); fi
}

for t in "${targets[@]}"; do
  case "$t" in
    domain)
      run domain "$root/core-domain" check generateResources
      if ! git -C "$root" diff --quiet -- targets/*/src/generated; then
        echo "Generated resources are out of date; commit the output of generateResources." >&2
        failed+=("domain:generated-resources-stale")
      fi ;;
    forge-1.20.1|neoforge-1.21.1|neoforge-26.1.2|neoforge-26.2)
      run "$t" "$root/targets/$t" build ;;
    *) echo "Unknown target $t" >&2; failed+=("$t") ;;
  esac
done

if [ ${#failed[@]} -gt 0 ]; then
  echo "FAILED: ${failed[*]}" >&2
  exit 1
fi
echo "All requested targets passed: ${targets[*]}"
