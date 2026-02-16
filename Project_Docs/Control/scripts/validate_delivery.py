#!/usr/bin/env python3
"""
Hard delivery validator for Control + Task package workflow.

Usage:
  python3 Project_Docs/Control/scripts/validate_delivery.py \\
    --phase postcommit \\
    --task Project_Docs/Control/TASKS/TASK_STEP_10_D3.md \\
    --evidence-dir Project_Docs/Re_Step/Evidence/Step10/20260215T205400Z \\
    --current-state Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md \\
    --current-step Project_Docs/Control/CURRENT_STEP.md \\
    --step-label "Step-10"

Phase semantics:
  precommit  - Run before git commit. gate10 (pending) expected to fail.
               validate_postcommit.* allowed missing. gate09_/gate10_/gate11_
               produced by this run are skipped.
  postcommit - Run after git commit + hash backfill. All files must exist.
               gate09_/gate10_/gate11_ produced by this run are skipped.
               Default phase if --phase is omitted (strict by default).
"""

from __future__ import annotations

import argparse
import pathlib
import re
import subprocess
import sys
from typing import List, Optional, Set, Tuple


HEADER_RE = re.compile(r"^##\s+(.+?)\s*$")
BULLET_RE = re.compile(r"^\s*-\s+(.+?)\s*$")
NUMBER_RE = re.compile(r"^\s*\d+\.\s+(.+?)\s*$")
BACKTICK_RE = re.compile(r"`([^`]+)`")

# Files produced by this validator in the current run (always skipped).
SELF_PRODUCED_PREFIXES = ("gate09_", "gate10_", "gate11_")

# Files that only exist after postcommit (skipped during precommit only).
POSTCOMMIT_ONLY_PREFIXES = ("validate_postcommit",)

# Files conditionally required at precommit (policy_exception needed
# only when precommit.exit != 0, but we cannot know that ahead of time,
# so we skip it at precommit and require it at postcommit).
PRECOMMIT_CONDITIONAL_PREFIXES = ("validate_policy_exception",)

# Delivery summary is overwritten by this validator each run.
DELIVERY_SUMMARY = "delivery_integrity_summary.log"


def read_text(path: pathlib.Path) -> str:
    return path.read_text(encoding="utf-8")


def section_lines(md: str, section_title: str) -> List[str]:
    lines = md.splitlines()
    start = -1
    for i, line in enumerate(lines):
        m = HEADER_RE.match(line)
        if m and m.group(1).strip() == section_title:
            start = i + 1
            break
    if start < 0:
        return []
    out: List[str] = []
    for j in range(start, len(lines)):
        if HEADER_RE.match(lines[j]):
            break
        out.append(lines[j])
    return out


def extract_dir(section: List[str]) -> str:
    for line in section:
        m = BACKTICK_RE.search(line)
        if m:
            val = m.group(1).strip()
            if "/" in val:
                return val
    return ""


def extract_list_items(section: List[str]) -> Tuple[List[str], List[str]]:
    """
    Returns (items, errors).
    Enforces explicit list; shorthand like gate01~gate11 is rejected.
    """
    items: List[str] = []
    errors: List[str] = []
    for line in section:
        m = BULLET_RE.match(line) or NUMBER_RE.match(line)
        if not m:
            continue
        body = m.group(1)
        if "~" in body or ".." in body or "+" in body:
            errors.append(f"Shorthand not allowed: {body}")
            continue
        ticks = BACKTICK_RE.findall(body)
        if not ticks:
            errors.append(f"Backtick file token required: {body}")
            continue
        for t in ticks:
            token = t.strip()
            if token and not token.endswith("/{RUN_ID}/"):
                items.append(token)
    return items, errors


def parse_task(task_path: pathlib.Path) -> Tuple[List[str], List[str], List[str]]:
    md = read_text(task_path)
    evidence_sec = section_lines(md, "强制证据输出")
    artifact_sec = section_lines(md, "强制输出文档")
    evidence_items, evidence_errors = extract_list_items(evidence_sec)
    artifact_items, artifact_errors = extract_list_items(artifact_sec)
    return evidence_items, artifact_items, evidence_errors + artifact_errors


def _should_skip(basename: str, phase: str) -> bool:
    """Determine whether a file should be skipped based on phase."""
    # Always skip files produced by this validator in the current run.
    for prefix in SELF_PRODUCED_PREFIXES:
        if basename.startswith(prefix):
            return True
    # delivery_integrity_summary.log is overwritten by this validator.
    if basename == DELIVERY_SUMMARY:
        return True
    # Phase-dependent skips.
    if phase == "precommit":
        for prefix in POSTCOMMIT_ONLY_PREFIXES:
            if basename.startswith(prefix):
                return True
        for prefix in PRECOMMIT_CONDITIONAL_PREFIXES:
            if basename.startswith(prefix):
                return True
    # postcommit: nothing else is skipped. All validate_* must exist.
    return False


def check_files_exist(base: pathlib.Path, items: List[str],
                      phase: str = "postcommit") -> Tuple[List[str], List[str]]:
    """
    Returns (missing, skipped) file lists.
    """
    missing: List[str] = []
    skipped: List[str] = []
    for item in items:
        basename = pathlib.Path(item).name
        if _should_skip(basename, phase):
            skipped.append(item)
            continue
        # item might be a filename or a relative path.
        p = pathlib.Path(item)
        full = p if p.is_absolute() else (base / p.name if "/" not in item else p)
        if not full.exists():
            if "/" in item:
                if not pathlib.Path(item).exists():
                    missing.append(item)
            else:
                missing.append(str(base / item))
    return missing, skipped


def parse_taskfile_from_current_step(current_step: pathlib.Path) -> str:
    text = read_text(current_step)
    m = re.search(r"^\s*-\s*TaskFile:\s*(\S+)\s*$", text, re.MULTILINE)
    return m.group(1).strip() if m else ""


def latest_commit_for_step(step_label: str) -> Optional[str]:
    try:
        out = subprocess.check_output(
            ["git", "log", "--oneline", "--grep", step_label, "-n", "1"],
            text=True,
        ).strip()
    except Exception:
        return None
    if not out:
        return None
    return out.split()[0]


def check_commit_not_pending(current_state: pathlib.Path, step_label: str) -> Tuple[bool, str]:
    text = read_text(current_state)
    pat = re.compile(rf"^\|\s*{re.escape(step_label)}\s*\|\s*([^|]+)\|", re.MULTILINE)
    m = pat.search(text)
    if not m:
        return False, f"Missing row for {step_label}"
    commit = m.group(1).strip()
    if commit == "(pending)" or commit == "":
        return False, f"{step_label} commit is pending"
    latest = latest_commit_for_step(step_label)
    if latest and latest != commit:
        return False, f"{step_label} commit mismatch: state={commit}, latest={latest}"
    return True, f"{step_label} commit = {commit}"


def write_gate(evidence_dir: pathlib.Path, gate_name: str, lines: List[str], ok: bool) -> None:
    log = evidence_dir / f"{gate_name}.log"
    ex = evidence_dir / f"{gate_name}.exit"
    log.write_text("\n".join(lines) + "\n", encoding="utf-8")
    ex.write_text("0\n" if ok else "1\n", encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--phase", choices=["precommit", "postcommit"],
                    default="postcommit",
                    help="Validation phase: precommit or postcommit (default: postcommit)")
    ap.add_argument("--task", required=True)
    ap.add_argument("--evidence-dir", required=True)
    ap.add_argument("--current-state", required=True)
    ap.add_argument("--current-step", required=True)
    ap.add_argument("--step-label", required=True)
    args = ap.parse_args()

    phase = args.phase
    task = pathlib.Path(args.task)
    evidence_dir = pathlib.Path(args.evidence_dir)
    current_state = pathlib.Path(args.current_state)
    current_step = pathlib.Path(args.current_step)

    evidence_items, artifact_items, parse_errors = parse_task(task)

    gate9_lines: List[str] = []
    gate9_ok = True
    gate9_lines.append(f"task={task}")
    gate9_lines.append(f"evidence_dir={evidence_dir}")
    gate9_lines.append(f"phase={phase}")
    if parse_errors:
        gate9_ok = False
        gate9_lines.append("parse_errors:")
        gate9_lines.extend(f"- {e}" for e in parse_errors)

    ev_missing, ev_skipped = check_files_exist(evidence_dir, evidence_items, phase)
    if ev_missing:
        gate9_ok = False
        gate9_lines.append("missing_evidence:")
        gate9_lines.extend(f"- {m}" for m in ev_missing)
    else:
        gate9_lines.append(f"evidence_items_ok={len(evidence_items)}")
    if ev_skipped:
        gate9_lines.append(f"skipped_by_phase({phase}):")
        gate9_lines.extend(f"- {s}" for s in ev_skipped)

    # Artifact dir inference from task file.
    task_md = read_text(task)
    artifact_sec = section_lines(task_md, "强制输出文档")
    artifact_dir_s = extract_dir(artifact_sec)
    artifact_dir = pathlib.Path(artifact_dir_s) if artifact_dir_s else pathlib.Path(".")
    art_missing, art_skipped = check_files_exist(artifact_dir, artifact_items, phase)
    if art_missing:
        gate9_ok = False
        gate9_lines.append("missing_artifacts:")
        gate9_lines.extend(f"- {m}" for m in art_missing)
    else:
        gate9_lines.append(f"artifact_items_ok={len(artifact_items)}")

    write_gate(evidence_dir, "gate09_evidence_completeness", gate9_lines, gate9_ok)

    ok10, msg10 = check_commit_not_pending(current_state, args.step_label)
    write_gate(evidence_dir, "gate10_commit_not_pending", [msg10], ok10)

    tf = parse_taskfile_from_current_step(current_step)
    next_task_ok = bool(tf) and pathlib.Path("Project_Docs/Control/TASKS", tf).exists()
    msg11 = f"taskfile={tf} exists={next_task_ok}"
    write_gate(evidence_dir, "gate11_next_taskfile_exists", [msg11], next_task_ok)

    all_ok = gate9_ok and ok10 and next_task_ok
    summary = evidence_dir / "delivery_integrity_summary.log"
    summary.write_text(
        "\n".join(
            [
                f"phase={phase}",
                f"gate09_evidence_completeness={'PASS' if gate9_ok else 'FAIL'}",
                f"gate10_commit_not_pending={'PASS' if ok10 else 'FAIL'}",
                f"gate11_next_taskfile_exists={'PASS' if next_task_ok else 'FAIL'}",
                f"delivery_integrity={'PASS' if all_ok else 'FAIL'}",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main())
