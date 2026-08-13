#!/usr/bin/env python3
"""Generates a disposable large-central-directory asset fixture for startup qualification.

The generated files go into a build/work directory, never into Git: they exist only to inflate the
APK ZIP central-directory entry count so a startup trace can prove Backtrace initialization cost
does not scale with it.

Usage:
    scripts/generate_native_startup_fixture.py --entries 10000 --bytes-per-entry 1 --output <dir>
"""

import argparse
import os
import sys


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--entries", type=int, default=10000)
    parser.add_argument("--bytes-per-entry", type=int, default=1)
    parser.add_argument("--output", required=True)
    arguments = parser.parse_args()

    if arguments.entries < 1 or arguments.bytes_per_entry < 0:
        print("entries must be >= 1 and bytes-per-entry >= 0", file=sys.stderr)
        return 2

    output = os.path.abspath(arguments.output)
    if not any(marker in output for marker in ("build", "tmp", "work")):
        print(
            "Refusing to generate fixture assets outside a build/tmp/work directory: " + output,
            file=sys.stderr,
        )
        return 2

    os.makedirs(output, exist_ok=True)
    payload = b"x" * arguments.bytes_per_entry
    for index in range(arguments.entries):
        bucket = os.path.join(output, f"bucket{index // 1000:03d}")
        os.makedirs(bucket, exist_ok=True)
        with open(os.path.join(bucket, f"fixture{index:05d}.txt"), "wb") as entry:
            entry.write(payload)

    print(f"Generated {arguments.entries} fixture entries under {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
