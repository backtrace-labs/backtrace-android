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
    parser.add_argument(
        "--expected-baseline-traces",
        type=int,
        default=None,
        help="require exactly this many baseline trace files",
    )
    parser.add_argument(
        "--expected-fixture-traces",
        type=int,
        default=None,
        help="require exactly this many fixture trace files (use zero when not captured)",
    )
    arguments = parser.parse_args()

    for flag, count in (
        ("--expected-samples", arguments.expected_samples),
        ("--expected-baseline-traces", arguments.expected_baseline_traces),
        ("--expected-fixture-traces", arguments.expected_fixture_traces),
    ):
        if count is not None and count < 0:
            parser.error(f"{flag} must be zero or greater")

    variants = {"baseline": [], "fixture": []}
    trace_counts = {"baseline": 0, "fixture": 0}
    per_trace_section_counts = {}
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
        trace_durations = section_durations_ms(arguments.trace_processor, trace)
        per_trace_section_counts[basename] = len(trace_durations)
        variants[variant].extend(trace_durations)
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
        "trace_file_counts": trace_counts,
        "per_trace_section_counts": per_trace_section_counts,
        "forbidden_symbol_matches": sorted(matches),
        "callstack_coverage": callstack_coverage,
    }
    with open(arguments.output, "w") as output:
        json.dump(summary, output, indent=2)
    print(json.dumps(summary, indent=2))

    failed = False
    if matches:
        print("Forbidden symbols present in runtime callstacks", file=sys.stderr)
        failed = True

    expected_trace_counts = {
        "baseline": arguments.expected_baseline_traces,
        "fixture": arguments.expected_fixture_traces,
    }
    for variant, expected_count in expected_trace_counts.items():
        actual_count = trace_counts[variant]
        if expected_count is not None and actual_count != expected_count:
            print(
                f"{variant}: expected exactly {expected_count} trace file(s), found {actual_count}",
                file=sys.stderr,
            )
            failed = True

    if arguments.expected_samples is not None:
        for variant, trace_count in trace_counts.items():
            if trace_count > 0 and len(variants[variant]) != arguments.expected_samples:
                print(
                    f"{variant}: expected exactly {arguments.expected_samples} {TRACE_SECTION} samples"
                    f", found {len(variants[variant])}",
                    file=sys.stderr,
                )
                failed = True

    for trace, section_count in per_trace_section_counts.items():
        if section_count != 1:
            print(
                f"{trace}: expected exactly one {TRACE_SECTION} section, found {section_count};"
                " app atrace may not be enabled, or the activity may have initialized more than once",
                file=sys.stderr,
            )
            failed = True
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
