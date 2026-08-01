#!/usr/bin/env bash
# End-to-end wall-clock benchmark of the map generator shadow jar, comparing the scalar
# fallback path against the Vector API (SIMD) path. Requires hyperfine (https://github.com/sharkdp/hyperfine).
#
# Usage: scripts/benchmark-e2e.sh path/to/NeroxisGen_x.y.z.jar [map-size] [seed] [num-to-generate] [runs]
#
# Build the jar first: ./gradlew :generator:shadowJar  (output in generator/build/libs/)
set -euo pipefail

JAR=${1:?Usage: $0 path/to/NeroxisGen.jar [map-size] [seed] [num-to-generate] [runs]}
MAP_SIZE=${2:-512}
SEED=${3:-0}
NUM=${4:-3}
RUNS=${5:-5}

if ! command -v hyperfine >/dev/null; then
    echo "hyperfine not found; install it (e.g. 'brew install hyperfine' / 'apt install hyperfine')" >&2
    exit 1
fi

COMMON_ARGS="--map-size $MAP_SIZE --seed $SEED --num-to-generate $NUM"

# Each invocation writes into a fresh temp dir so repeated runs with the same seed never collide.
hyperfine --warmup 1 --runs "$RUNS" \
    --command-name scalar "java -jar '$JAR' $COMMON_ARGS --out-path \"\$(mktemp -d)\"" \
    --command-name vector "java --add-modules jdk.incubator.vector -Dneroxis.vector.enabled=true -jar '$JAR' $COMMON_ARGS --out-path \"\$(mktemp -d)\""
