#!/usr/bin/env python3
"""Minimal cross-session task record helper for doc-pilot.

Pure stdlib, works on any OS. Stores one JSON file per task under
docs/.doc-pilot/tasks/<task_id>.json. This is *runtime memory* for
resuming long-running or paused tasks across sessions -- it does not
replace the long-term knowledge base (dev-log / STATUS / BUGFIX_LOG).

Usage:
    python task-context.py create TASK-20260906-001 "标题" --level L4
    python task-context.py update TASK-20260906-001 --stage 开发中 --next-step "..."
    python task-context.py show TASK-20260906-001
    python task-context.py list
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from datetime import datetime, timezone

VALID_STAGES = {
    "待处理", "方案确认中", "已确认待建票", "开发中", "验证中",
    "已完成", "已暂停", "已阻塞", "验证未通过",
}


def now() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def task_dir(root: Path) -> Path:
    p = root / "docs" / ".doc-pilot" / "tasks"
    p.mkdir(parents=True, exist_ok=True)
    return p


def load(path: Path) -> dict:
    if not path.exists():
        raise SystemExit(f"任务记录不存在: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def save(path: Path, data: dict) -> None:
    data["updated_at"] = now()
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )


def cmd_create(args, root: Path) -> None:
    p = task_dir(root) / f"{args.task_id}.json"
    if p.exists():
        raise SystemExit(f"任务已存在: {p}")
    data = {
        "task_id": args.task_id,
        "title": args.title,
        "level": args.level,
        "stage": "待处理",
        "ticket_key": None,
        "plan_path": None,
        "affected_modules": [],
        "completed_steps": [],
        "current_step": "",
        "blockers": [],
        "next_step": "",
        "key_files": [],
        "created_at": now(),
        "updated_at": now(),
    }
    save(p, data)
    print(f"已创建: {p}")


def cmd_update(args, root: Path) -> None:
    p = task_dir(root) / f"{args.task_id}.json"
    data = load(p)
    if args.stage:
        if args.stage not in VALID_STAGES:
            raise SystemExit(
                f"未知阶段 '{args.stage}'，合法取值: {', '.join(sorted(VALID_STAGES))}"
            )
        data["stage"] = args.stage
    if args.ticket_key is not None:
        data["ticket_key"] = args.ticket_key
    if args.plan_path is not None:
        data["plan_path"] = args.plan_path
    if args.current_step is not None:
        data["current_step"] = args.current_step
    if args.next_step is not None:
        data["next_step"] = args.next_step
    if args.add_completed_step:
        data.setdefault("completed_steps", []).append(args.add_completed_step)
    if args.add_blocker:
        data.setdefault("blockers", []).append(args.add_blocker)
    if args.clear_blockers:
        data["blockers"] = []
    save(p, data)
    print(f"已更新: {p} (stage={data['stage']})")


def cmd_show(args, root: Path) -> None:
    p = task_dir(root) / f"{args.task_id}.json"
    print(json.dumps(load(p), ensure_ascii=False, indent=2))


def cmd_list(args, root: Path) -> None:
    d = task_dir(root)
    files = sorted(d.glob("TASK-*.json"))
    if not files:
        print("(当前没有任务记录)")
        return
    for f in files:
        data = json.loads(f.read_text(encoding="utf-8"))
        print(f"{data['task_id']}  [{data['level']}]  {data['stage']:<8}  {data['title']}")


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--root", default=".", help="仓库根目录，默认当前目录")
    sub = ap.add_subparsers(dest="cmd", required=True)

    c = sub.add_parser("create", help="创建一条新任务记录")
    c.add_argument("task_id")
    c.add_argument("title")
    c.add_argument("--level", default="L4", choices=["L1", "L2", "L3", "L4"])

    u = sub.add_parser("update", help="更新一条任务记录")
    u.add_argument("task_id")
    u.add_argument("--stage", choices=sorted(VALID_STAGES))
    u.add_argument("--ticket-key")
    u.add_argument("--plan-path")
    u.add_argument("--current-step")
    u.add_argument("--next-step")
    u.add_argument("--add-completed-step")
    u.add_argument("--add-blocker")
    u.add_argument("--clear-blockers", action="store_true")

    s = sub.add_parser("show", help="查看一条任务记录")
    s.add_argument("task_id")

    sub.add_parser("list", help="列出所有任务记录")

    args = ap.parse_args()
    root = Path(args.root).resolve()

    handlers = {
        "create": cmd_create,
        "update": cmd_update,
        "show": cmd_show,
        "list": cmd_list,
    }
    handlers[args.cmd](args, root)


if __name__ == "__main__":
    main()
