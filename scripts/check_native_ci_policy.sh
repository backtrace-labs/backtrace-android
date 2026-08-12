#!/usr/bin/env bash
set -euo pipefail

# CI trust/supply-chain policy checker for the test workflow. Fails on:
#   - an unpinned external `uses:` (anything not pinned to a full 40-hex commit SHA)
#   - `saucectl-version: latest`
#   - `pull_request_target`
#   - ACCESS_TOKEN used in checkout
#   - a repository-secret reference in a job not gated by the trusted policy output
#     (workflow_dispatch-only jobs count as trusted: forks cannot dispatch them)
#   - `|| true` on a workflow gate command
#   - missing `permissions:` with `contents: read`
#   - missing API 21/22 legacy lane
#   - missing 16 KB runtime image (google_apis_ps16k)
#   - missing fatal-process gate (RUN_FATAL=1)
#
# Usage: scripts/check_native_ci_policy.sh <workflow-file>

workflow="${1:?usage: $0 <workflow-file>}"
if [ ! -f "$workflow" ]; then
    echo "Workflow file not found: $workflow" >&2
    exit 1
fi

python3 - "$workflow" <<'PY'
import re
import sys

path = sys.argv[1]
text = open(path, encoding="utf-8").read()
lines = text.splitlines()
violations = []

# Rule: pull_request_target is never allowed.
if re.search(r"^\s*pull_request_target\s*:", text, re.M):
    violations.append("pull_request_target must not be used")

# Rule: permissions: contents: read at the workflow level.
if not re.search(r"^permissions:\s*$", text, re.M) or not re.search(r"^\s+contents:\s*read\s*$", text, re.M):
    violations.append("workflow must declare permissions: contents: read")

# Rule: every external action pinned to a full commit SHA.
for number, line in enumerate(lines, 1):
    match = re.search(r"uses:\s*([^\s#]+)", line)
    if not match:
        continue
    ref = match.group(1)
    if "@" not in ref:
        violations.append(f"line {number}: action without a ref: {ref}")
        continue
    sha = ref.rsplit("@", 1)[1]
    if not re.fullmatch(r"[0-9a-f]{40}", sha):
        violations.append(f"line {number}: action not pinned to a full commit SHA: {ref}")

# Rule: no `saucectl-version: latest`.
if re.search(r"saucectl-version:\s*latest", text):
    violations.append("saucectl-version must be pinned, not latest")

# Rule: ACCESS_TOKEN must not be used for checkout.
if "ACCESS_TOKEN" in text:
    violations.append("ACCESS_TOKEN must not be referenced; submodules are public over HTTPS")

# Rule: no `|| true` in workflow run blocks (gates live in scripts, where only
# diagnostics may be non-gating).
for number, line in enumerate(lines, 1):
    if "|| true" in line:
        violations.append(f"line {number}: '|| true' is not allowed on a workflow command")

# Rule: repository secrets only in trusted-gated jobs. Split on top-level job keys.
job_starts = [
    (index, re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line).group(1))
    for index, line in enumerate(lines)
    if re.match(r"^  ([A-Za-z0-9_-]+):\s*$", line)
]
jobs_index = next((i for i, line in enumerate(lines) if line.strip() == "jobs:"), None)
if jobs_index is not None:
    job_starts = [(i, name) for i, name in job_starts if i > jobs_index]
    for position, (start, name) in enumerate(job_starts):
        end = job_starts[position + 1][0] if position + 1 < len(job_starts) else len(lines)
        body = "\n".join(lines[start:end])
        secret_refs = [
            secret
            for secret in re.findall(r"secrets\.([A-Za-z0-9_]+)", body)
            if secret not in ("GITHUB_TOKEN",)
        ]
        if not secret_refs:
            continue
        trusted_gated = "needs.policy.outputs.trusted == 'true'" in body
        dispatch_gated = "github.event_name == 'workflow_dispatch'" in body
        if not trusted_gated and not dispatch_gated:
            violations.append(
                f"job '{name}' references secrets ({', '.join(sorted(set(secret_refs)))})"
                " without a trusted-policy gate"
            )

# Rule: required lanes present.
if not re.search(r"api-level:\s*\[\s*21\s*,\s*22\s*\]", text):
    violations.append("missing API 21/22 legacy lane")
if "google_apis_ps16k" not in text:
    violations.append("missing 16 KB runtime lane (google_apis_ps16k)")
if "RUN_FATAL=1" not in text and "run_16kb_native_qualification.sh" not in text:
    violations.append("missing fatal-process gate (RUN_FATAL=1)")

if violations:
    for violation in violations:
        print("CI policy violation: " + violation, file=sys.stderr)
    sys.exit(1)

print(f"CI policy check passed for {path}.")
PY
