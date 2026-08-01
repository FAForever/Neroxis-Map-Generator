#!/usr/bin/env bash
# Verifies that map generation is bit-identical between the scalar fallback path and the
# Vector API (SIMD) path: generates the same seeded map under both modes and diffs the
# sha256 checksums of every output file. Exits non-zero on any difference.
#
# Usage: scripts/check-determinism.sh path/to/NeroxisGen.jar [map-size] [seed]
set -euo pipefail

JAR=${1:?Usage: $0 path/to/NeroxisGen.jar [map-size] [seed]}
MAP_SIZE=${2:-512}
SEED=${3:-0}

SCALAR_DIR=$(mktemp -d)
VECTOR_DIR=$(mktemp -d)
trap 'rm -rf "$SCALAR_DIR" "$VECTOR_DIR"' EXIT

COMMON_ARGS=(--map-size "$MAP_SIZE" --seed "$SEED" --num-to-generate 1)

echo "Generating with scalar path (vector disabled)..."
java -Dneroxis.vector.enabled=false -jar "$JAR" "${COMMON_ARGS[@]}" --out-path "$SCALAR_DIR" >/dev/null

echo "Generating with vector path (opt-in + --add-modules jdk.incubator.vector)..."
java --add-modules jdk.incubator.vector -Dneroxis.vector.enabled=true -jar "$JAR" "${COMMON_ARGS[@]}" --out-path "$VECTOR_DIR" >/dev/null

hash_tree() {
    if command -v sha256sum >/dev/null; then
        (cd "$1" && find . -type f -print0 | sort -z | xargs -0 sha256sum)
    else
        (cd "$1" && find . -type f -print0 | sort -z | xargs -0 shasum -a 256)
    fi
}

if diff <(hash_tree "$SCALAR_DIR") <(hash_tree "$VECTOR_DIR"); then
    echo "OK: scalar and vector outputs are bit-identical (map-size=$MAP_SIZE seed=$SEED)"
else
    echo "FAIL: scalar and vector outputs differ (map-size=$MAP_SIZE seed=$SEED)" >&2
    exit 1
fi
