#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "usage: $0 <fabric|neoforge> <absolute-prism-mods-directory>" >&2
    exit 2
fi

loader="$1"
mods_dir="$2"
root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

case "$loader" in
    fabric)
        libs="$root/fabric/build/libs"
        pattern='spellcraft-fabric-*.jar'
        ;;
    neoforge)
        libs="$root/neoforge/build/libs"
        pattern='spellcraft-neoforge-*.jar'
        ;;
    *)
        echo "loader must be 'fabric' or 'neoforge'" >&2
        exit 2
        ;;
esac

if [[ ! -d "$mods_dir" ]]; then
    echo "mods directory does not exist: $mods_dir" >&2
    exit 1
fi

jar="$(find "$libs" -maxdepth 1 -type f -name "$pattern" ! -name '*-sources.jar' -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n1 | cut -d' ' -f2-)"

if [[ -z "$jar" ]]; then
    echo "no built jar found in $libs; build the $loader project first" >&2
    exit 1
fi

# Remove older scaffold jars for this loader so Prism does not see duplicates.
find "$mods_dir" -maxdepth 1 -type f -name "$pattern" -delete
cp -f "$jar" "$mods_dir/"

echo "installed: $(basename "$jar")"
echo "into:      $mods_dir"

if [[ "$loader" == "fabric" ]]; then
    echo "remember: the Fabric instance also needs Fabric API for Minecraft 26.3"
fi
