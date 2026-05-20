#!/usr/bin/env bash
#
# Cold-start regression gate. Reads the Macrobenchmark JSON output
# produced by `:baselineprofile:connectedBenchmarkAndroidTest`,
# extracts the P50 of `startupMs` for the
# `CompilationMode.Partial(BaselineProfile)` variant, and compares
# it against the committed baseline at
# `baselineprofile/baseline-startup.json`.
#
# Exit codes:
#   0 - P50 within the allowed envelope (<= baseline * 1.10)
#   1 - P50 regressed beyond the 10% allowance
#   2 - couldn't find / parse the inputs
#
# Usage:
#   ./baselineprofile/scripts/check-startup-regression.sh \
#       <path-to-benchmarkData.json>
#
# The path defaults to the location Macrobenchmark writes to when
# run from the baselineprofile module:
#   baselineprofile/build/outputs/connected_android_test_additional_output/
#     benchmark/connected/<device-id>/com.calculator.baselineprofile-benchmarkData.json
# Pass an explicit path on CI where the device id is unpredictable.

set -euo pipefail

BASELINE_FILE="$(cd "$(dirname "$0")/.." && pwd)/baseline-startup.json"
ALLOWED_REGRESSION_PCT=10
TARGET_METHOD="startupCompilationBaselineProfiles"

if [[ ! -f "$BASELINE_FILE" ]]; then
    echo "ERROR: baseline file not found at $BASELINE_FILE" >&2
    exit 2
fi

# Locate the benchmark JSON. If an argument is provided, use that;
# otherwise glob for it under the standard Macrobenchmark output dir.
if [[ -n "${1:-}" ]]; then
    DATA_FILE="$1"
else
    DATA_FILE="$(find baselineprofile/build/outputs -name '*benchmarkData.json' -print -quit 2>/dev/null || true)"
fi

if [[ -z "${DATA_FILE:-}" || ! -f "$DATA_FILE" ]]; then
    echo "ERROR: benchmark data file not found (pass it as the first arg)" >&2
    exit 2
fi

# Pull the baseline (committed) P50 first.
baseline_p50=$(python3 -c "
import json, sys
with open('$BASELINE_FILE') as f:
    data = json.load(f)
print(data['startup_p50_ms'])
")

# Pull the measured P50 for the BaselineProfile compilation row.
# Macrobenchmark's benchmarkData.json schema (1.3.x):
#   {
#     'benchmarks': [
#       { 'name': 'startupCompilationBaselineProfiles',
#         'metrics': { 'timeToInitialDisplayMs': { 'minimum':..., 'maximum':..., 'median':..., 'runs': [...] } } },
#       ...
#     ]
#   }
# `StartupTimingMetric` historically emitted 'startupMs' but renamed
# to 'timeToInitialDisplayMs' (TTID) in 1.2+. Try both so the script
# survives a version bump in either direction.
measured_p50=$(python3 -c "
import json, sys
with open('$DATA_FILE') as f:
    data = json.load(f)
candidates = [
    b for b in data.get('benchmarks', [])
    if '$TARGET_METHOD' in b.get('name', '') or 'BaselineProfile' in b.get('name', '')
]
if not candidates:
    print('NO_MATCH', file=sys.stderr)
    sys.exit(2)
metrics = candidates[0].get('metrics', {})
metric = metrics.get('timeToInitialDisplayMs') or metrics.get('startupMs') or {}
# Macrobenchmark uses 'median' as the P50 (the value the gate is
# benchmarking against).
p50 = metric.get('median')
if p50 is None:
    print('NO_MEDIAN keys=' + ','.join(metrics.keys()), file=sys.stderr)
    sys.exit(2)
print(f'{p50:.1f}')
")

threshold=$(python3 -c "print(${baseline_p50} * (1 + ${ALLOWED_REGRESSION_PCT}/100))")

echo "Cold-start P50 gate:"
echo "  baseline (committed): ${baseline_p50} ms"
echo "  measured (this run):  ${measured_p50} ms"
echo "  threshold (+${ALLOWED_REGRESSION_PCT}%):  ${threshold} ms"

# Compare in floating point via Python (bash arithmetic is integer-only).
result=$(python3 -c "
print('FAIL' if ${measured_p50} > ${threshold} else 'OK')
")

if [[ "$result" == "FAIL" ]]; then
    echo "FAIL: cold-start P50 regressed beyond the ${ALLOWED_REGRESSION_PCT}% allowance." >&2
    echo "If the regression is expected, update baselineprofile/baseline-startup.json." >&2
    exit 1
fi

echo "OK: cold-start within the allowed envelope."
exit 0
