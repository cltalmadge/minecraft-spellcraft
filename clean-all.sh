#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
(cd "$ROOT/domain" && ./gradlew clean)
(cd "$ROOT/fabric" && ./gradlew clean)
(cd "$ROOT/neoforge" && ./gradlew clean)
