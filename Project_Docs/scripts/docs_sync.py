#!/usr/bin/env python3
"""Sync documentation status files for Project_Docs.

What this script does:
1) Rebuild Project_Docs/Reports/README.md from report files.
2) Refresh the auto-sync block in Project_Docs/CURRENT_STATUS.md.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Optional


@dataclass
class ReportEntry:
    task_id: Optional[int]
    file_name: str
    title: str
    status: str
    date: str


def _parse_title(text: str, fallback: str) -> str:
    for line in text.splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return fallback


def _parse_date(text: str) -> str:
    patterns = [
        r"\*{0,2}完成时间\*{0,2}\s*[:：]\s*(\d{4}-\d{2}-\d{2})",
        r"\*{0,2}日期\*{0,2}\s*[:：]\s*(\d{4}-\d{2}-\d{2})",
        r"\*{0,2}Document generated\*{0,2}\s*[:：]\s*(\d{4}-\d{2}-\d{2})",
    ]
    for pattern in patterns:
        match = re.search(pattern, text, flags=re.IGNORECASE)
        if match:
            return match.group(1)
    return "-"


def _parse_status(text: str, date: str) -> str:
    status_match = re.search(r"\*{0,2}状态\*{0,2}\s*[:：]\s*([^\n]+)", text)
    if status_match:
        return status_match.group(1).strip()
    if "BUILD SUCCESSFUL" in text or date != "-":
        return "已完成"
    return "已记录"


def _parse_task_id(file_name: str) -> Optional[int]:
    match = re.match(r"(\d{3})", file_name)
    if not match:
        return None
    return int(match.group(1))


def load_reports(reports_dir: Path) -> List[ReportEntry]:
    entries: List[ReportEntry] = []
    for path in sorted(reports_dir.glob("*.md")):
        if path.name == "README.md":
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        task_id = _parse_task_id(path.name)
        title = _parse_title(text, path.stem)
        date = _parse_date(text)
        status = _parse_status(text, date)
        entries.append(
            ReportEntry(
                task_id=task_id,
                file_name=path.name,
                title=title,
                status=status,
                date=date,
            )
        )
    entries.sort(
        key=lambda e: (
            e.task_id if e.task_id is not None else -1,
            e.date,
            e.file_name,
        ),
        reverse=True,
    )
    return entries


def _latest_task(entries: List[ReportEntry]) -> Optional[int]:
    task_ids = [e.task_id for e in entries if e.task_id is not None]
    if not task_ids:
        return None
    return max(task_ids)


def build_reports_readme(entries: List[ReportEntry], now_utc: str) -> str:
    latest = _latest_task(entries)
    latest_lines: List[str]
    if latest is None:
        latest_lines = ["- 最新任务: 无"]
    else:
        latest_rows = [e for e in entries if e.task_id == latest]
        latest_states = sorted({e.status for e in latest_rows})
        latest_files = ", ".join(e.file_name for e in latest_rows)
        latest_lines = [
            f"- 最新任务: #{latest:03d}",
            f"- 最新任务状态: {' / '.join(latest_states)}",
            f"- 最新任务文件: {latest_files}",
        ]

    lines = [
        "# Reports Index",
        "",
        "本索引由 `Project_Docs/scripts/docs_sync.py` 自动生成。",
        f"- 同步时间 (UTC): {now_utc}",
        f"- 报告总数: {len(entries)}",
        *latest_lines,
        "",
        "## 全量报告",
        "",
        "| Task | File | Date | Status | Title |",
        "|---|---|---|---|---|",
    ]

    for e in entries:
        task = f"#{e.task_id:03d}" if e.task_id is not None else "-"
        lines.append(
            f"| {task} | `{e.file_name}` | {e.date} | {e.status} | {e.title} |"
        )

    lines.append("")
    return "\n".join(lines)


def update_current_status(status_path: Path, entries: List[ReportEntry], now_utc: str) -> None:
    text = status_path.read_text(encoding="utf-8") if status_path.exists() else ""
    latest = _latest_task(entries)

    if latest is None:
        latest_summary = "- 最新任务: 无\n- 最新任务状态: 无\n- 最新任务文件: 无"
    else:
        latest_rows = [e for e in entries if e.task_id == latest]
        latest_states = " / ".join(sorted({e.status for e in latest_rows}))
        latest_files = ", ".join(e.file_name for e in latest_rows)
        latest_summary = (
            f"- 最新任务: #{latest:03d}\n"
            f"- 最新任务状态: {latest_states}\n"
            f"- 最新任务文件: {latest_files}"
        )

    block = (
        "<!-- AUTO_SYNC:START -->\n"
        "## 自动同步区块\n"
        f"- 最近同步 (UTC): {now_utc}\n"
        f"- 报告总数: {len(entries)}\n"
        f"{latest_summary}\n"
        "- 生成器: `python3 Project_Docs/scripts/docs_sync.py`\n"
        "<!-- AUTO_SYNC:END -->"
    )

    pattern = re.compile(
        r"<!-- AUTO_SYNC:START -->[\s\S]*?<!-- AUTO_SYNC:END -->",
        flags=re.MULTILINE,
    )

    if pattern.search(text):
        new_text = pattern.sub(block, text)
    else:
        suffix = "\n\n" if text and not text.endswith("\n\n") else ""
        new_text = text + suffix + block + "\n"

    status_path.write_text(new_text, encoding="utf-8")


def main() -> None:
    project_docs = Path(__file__).resolve().parents[1]
    reports_dir = project_docs / "Reports"
    status_path = project_docs / "CURRENT_STATUS.md"
    reports_index_path = reports_dir / "README.md"

    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    entries = load_reports(reports_dir)

    reports_readme = build_reports_readme(entries, now_utc)
    reports_index_path.write_text(reports_readme, encoding="utf-8")
    update_current_status(status_path, entries, now_utc)

    print(f"[docs_sync] reports indexed: {len(entries)}")
    print(f"[docs_sync] wrote: {reports_index_path}")
    print(f"[docs_sync] updated: {status_path}")


if __name__ == "__main__":
    main()
