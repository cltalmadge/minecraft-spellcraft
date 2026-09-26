#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

echo '== Domain tests =='
(cd "$ROOT/domain" && ./gradlew test)

echo '== Fabric build =='
(cd "$ROOT/fabric" && ./gradlew build)

echo '== NeoForge build =='
(cd "$ROOT/neoforge" && ./gradlew build)

echo
printf 'Fabric jar:   %s\n' "$ROOT/fabric/build/libs/spellcraft-fabric-0.1.0.jar"
printf 'NeoForge jar: %s\n' "$ROOT/neoforge/build/libs/spellcraft-neoforge-0.1.0.jar"
