#!/usr/bin/env python3
"""Summarizes Backtrace native-startup Perfetto traces.

Emits JSON with the BacktraceQualification#tryEnableNativeIntegration section sample count,
median/p95 duration, baseline-versus-fixture delta, and forbidden-symbol matches where callstack
data exists. When traces carry no callstack samples, callstack_coverage is reported as false
rather than claiming a clean runtime stack: the static source guard remains the hard automated
proof that no APK archive reader runs during native initialization.
"""

import argparse
import glob
import json
import statistics
import subprocess
import sys

TRACE_SECTION = "BacktraceQualification#tryEnableNativeIntegration"
FORBIDDEN_SYMBOLS = (
    "ZipFile$Source.initCEN",
    "java.util.zip.ZipFile",
    "java.util.zip.ZipInputStream",
    "java.util.jar.JarFile",
    "java.util.jar.JarInputStream",
    "apkContains",
)


def query(trace_processor: str, trace: str, sql: str) -> list:
    completed = subprocess.run(
        [trace_processor, "-q", "/dev/stdin", trace],
        input=sql.encode(),
        capture_output=True,
        check=True,
    )
    rows = completed.stdout.decode().strip().splitlines()
    return rows[1:] if len(rows) > 1 else []


def section_durations_ms(trace_processor: str, trace: str) -> list:
    sql = (
        'select dur from slice where name = "' + TRACE_SECTION + '"'
    )
    durations = []
    for row in query(trace_processor, trace, sql):
        try:
            durations.append(int(row.strip().split(",")[-1]) / 1_000_000.0)
        except ValueError:
            continue
    return durations


def forbidden_matches(trace_processor: str, trace: str) -> tuple:
    coverage_rows = query(
        trace_processor,
        trace,
        "select count(*) from stack_profile_frame",
    )
    has_callstacks = False
    for row in coverage_rows:
        try:
            has_callstacks = int(row.strip().split(",")[-1]) > 0
        except ValueError:
            pass
    matches = []
    if has_callstacks:
        for symbol in FORBIDDEN_SYMBOLS:
            rows = query(
                trace_processor,
                trace,
                'select count(*) from stack_profile_frame where name like "%' + symbol + '%"',
            )
            for row in rows:
                try:
                    if int(row.strip().split(",")[-1]) > 0:
                        matches.append(symbol)
                except ValueError:
                    continue
    return has_callstacks, matches


def summarize(durations: list) -> dict:
    if not durations:
        return {"samples": 0}
    ordered = sorted(durations)
    p95_index = max(0, int(round(0.95 * len(ordered))) - 1)
    return {
        "samples": len(ordered),
        "median_ms": round(statistics.median(ordered), 3),
        "p95_ms": round(ordered[p95_index], 3),
        "max_ms": round(ordered[-1], 3),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--trace-processor", required=True)
    parser.add_argument("--traces", required=True, help="directory of .perfetto-trace files")
    parser.add_argument("--output", required=True)
    parser.add_argument(
        "--expected-samples",
        type=int,
        default=None,
        help="require exactly this many trace-section samples per captured variant",
    )
    arguments = parser.parse_args()

    variants = {"baseline": [], "fixture": []}
    trace_counts = {"baseline": 0, "fixture": 0}
    callstack_coverage = False
    matches = set()
    traces = sorted(glob.glob(arguments.traces + "/*.perfetto-trace"))
    if not traces:
        print(f"No .perfetto-trace files found in {arguments.traces}; nothing was analyzed", file=sys.stderr)
        return 1
    for trace in traces:
        basename = trace.rsplit("/", 1)[-1]
        if basename.startswith("fixture-"):
            variant = "fixture"
        elif basename.startswith("baseline-"):
            variant = "baseline"
        else:
            print(f"Unrecognized trace name {basename}: expected baseline-*/fixture-*", file=sys.stderr)
            return 1
        trace_counts[variant] += 1
        variants[variant].extend(section_durations_ms(arguments.trace_processor, trace))
        covered, trace_matches = forbidden_matches(arguments.trace_processor, trace)
        callstack_coverage = callstack_coverage or covered
        matches.update(trace_matches)

    baseline = summarize(variants["baseline"])
    fixture = summarize(variants["fixture"])
    delta = None
    if baseline.get("samples") and fixture.get("samples"):
        delta = round(fixture["median_ms"] - baseline["median_ms"], 3)

    summary = {
        "trace_section": TRACE_SECTION,
        "baseline": baseline,
        "fixture": fixture,
        "median_delta_ms": delta,
        "forbidden_symbol_matches": sorted(matches),
        "callstack_coverage": callstack_coverage,
    }
    with open(arguments.output, "w") as output:
        json.dump(summary, output, indent=2)
    print(json.dumps(summary, indent=2))

    if matches:
        print("Forbidden symbols present in runtime callstacks", file=sys.stderr)
        return 1
    for variant, count in trace_counts.items():
        if count > 0 and not variants[variant]:
            print(
                f"{variant}: {count} trace(s) captured but zero {TRACE_SECTION} samples;"
                " app atrace was probably not enabled for the package",
                file=sys.stderr,
            )
            return 1
        if count > 0 and arguments.expected_samples is not None and len(variants[variant]) != arguments.expected_samples:
            print(
                f"{variant}: expected exactly {arguments.expected_samples} {TRACE_SECTION} samples"
                f" (one per cold-start iteration), found {len(variants[variant])}",
                file=sys.stderr,
            )
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
