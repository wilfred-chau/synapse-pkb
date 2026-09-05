#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""init-kb.py - scaffold a doc-pilot knowledge base in a new project.

Usage:
    python init-kb.py --root <repo-root> --project-name "My Project" \
        [--docs-language zh|en] [--vcs git|none] \
        [--ticket-prefix CES] [--ticket-gate-skill <path>] \
        [--commit-lang en|zh] [--requirements-docx] \
        [--extra-root-whitelist "CHANGELOG.md,CONTRIBUTING.md"] \
        [--force]

Creates the docs/ directory tree, seed files, and docs/.doc-pilot.json
(the config file every other doc-pilot script and routing rule reads from).
Safe to re-run: existing files are never overwritten unless --force is given
for the config file specifically; directory/seed-file creation always skips
files that already exist.

Pure stdlib, no third-party dependencies, works on any OS with Python 3.
"""

import argparse
import json
import os
import sys
from datetime import date

DIRS = [
    "overview",
    "progress",
    "progress/history",
    "bugs",
    "bugs/history",
    "design",
    "review",
    "qa",
    "deploy",
    "requirements",
    "archive",
]

TODAY = date.today().isoformat()


def write_if_absent(path, content):
    if os.path.exists(path):
        print("SKIP  (exists): %s" % path)
        return False
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("WROTE: %s" % path)
    return True


def seed_readme(docs, project_name):
    return """# %s · docs/ 知识库索引

> 本文件是 docs/ 的导航与登记表，人与 AI 共用。新增正式文档后必须在此登记一行，
> 否则会被 check-docs.py 的索引校验报错。

## 快速入口

- [progress/STATUS.md](progress/STATUS.md) — 进度速览（每次开发任务开工必读）
- [progress/dev-log.md](progress/dev-log.md) — 开发日志（活跃卷）
- [bugs/BUGFIX_LOG.md](bugs/BUGFIX_LOG.md) — 缺陷修复记录（活跃卷）
- [overview/architecture-snapshot.md](overview/architecture-snapshot.md) — 系统全貌
- [deploy/DEPLOYMENT.md](deploy/DEPLOYMENT.md) — 部署与运维手册

## 目录说明

| 目录 | 内容 |
| --- | --- |
| `overview/` | 系统现状：架构、横切机制 |
| `progress/` | 进度速览 + 开发日志（含 history/ 历史卷） |
| `bugs/` | 缺陷修复记录（含 history/ 历史卷） |
| `design/` | 技术方案与决策记录 |
| `review/` | 代码评审快照 |
| `qa/` | 测试规划与验证清单 |
| `deploy/` | 部署与运维手册 |
| `requirements/` | 需求原文与可检索快照 |
| `archive/` | 已废止但保留史料价值的文档 |

详细的目录结构、命名规则、归档规则见 skill 内 `references/kb-layout.md`。
""" % project_name


def seed_status(docs, project_name):
    return """# %s · 项目进度速览（STATUS）

> AI 开发值守必读入口。上接 [../README.md](../README.md)，细节见 [dev-log.md](dev-log.md)。
> last-updated 隐藏标记供 check-docs.py 校验新鲜度，更新正文日期时必须同步改。

<!-- last-updated: %s -->

## 模块进度总览

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| _(初始化占位，尚无模块)_ | | ⬜ |

## 最近动态（新→旧，最多 8 条）

- **%s**：知识库初始化（doc-pilot 首次接入本项目）。

## 当前焦点 / 待办

- [ ] 补充模块进度总览
- [ ] 补充 overview/architecture-snapshot.md

## 已知问题与坑位速查

| 编号/关键词 | 一句话 | 详见 |
| --- | --- | --- |
| _(暂无)_ | | ../bugs/BUGFIX_LOG.md |

## 关键约定速查（高频引用，避免翻史料）

- _(待补充：项目特有的技术约定、命名规范等)_

## 上下文恢复路线

1. 本文件 → 2. ../overview/architecture-snapshot.md → 3. 按任务选 design/qa → 4. 需要历史细节再进 history/ 对应阶段文件。
""" % (project_name, TODAY, TODAY)


def seed_devlog(docs, project_name):
    return """# %s · 开发日志（活跃卷）

> 按时间正序追加，一事一条。条目模板见 skill 内 `templates/devlog-entry.md`。
> 超过阈值（见 docs/.doc-pilot.json 的 capacity_limits）须按「天条目」轮转入 history/，
> 轮转后在下方登记一行。

## 历史卷索引

_(暂无轮转卷)_

---

## %s｜知识库初始化

**背景**：doc-pilot skill 首次接入本项目，生成 docs/ 知识库骨架与配置文件。

**产出**：
- docs/ 目录树与种子文件
- docs/.doc-pilot.json 配置文件

**验证**：
- 运行 `python scripts/check-docs.py` 应全绿（除「尚无模块」等占位提示）。

**遗留/Next**：
- 补充 overview/architecture-snapshot.md 与模块进度总览。
""" % (project_name, TODAY)


def seed_bugfix_log(project_name):
    return """# %s · 缺陷修复记录（活跃卷）

> 纯追加，条目模板见 skill 内 `templates/bugfix-entry.md`。
> 编号 BUG-NNN 全局自增、永不复用；超过阈值须轮转入 history/。

## 历史卷索引

_(暂无轮转卷)_

---

_(暂无记录)_
""" % project_name


def seed_architecture(project_name):
    return """# %s · 系统全貌（Architecture Snapshot）

> 描述系统「现在是什么样」。架构或横切机制变更时同步修订对应小节，不追加流水账。

## 技术栈

- _(待补充)_

## 模块划分

- _(待补充)_

## 横切机制

- 鉴权 / 权限：_(待补充)_
- 日志 / 审计：_(待补充)_
- 其他约定：_(待补充)_
""" % project_name


def seed_deployment(project_name):
    return """# %s · 部署与运维手册

## 环境

- _(待补充)_

## 部署步骤

- _(待补充)_

## 回滚步骤

- _(待补充)_
""" % project_name


def build_config(args):
    cfg = {
        "_comment": "doc-pilot 配置文件，由 scripts/init-kb.py 生成；skill 每次运行时读取本文件而非硬编码路径/前缀。",
        "project_name": args.project_name,
        "docs_root": "docs",
        "docs_language": args.docs_language,
        "vcs": args.vcs,
        "commit_message": {
            "language": args.commit_lang,
            "conventional_commits": True,
        },
        "ticket_system": {
            "enabled": bool(args.ticket_prefix),
            "prefix": args.ticket_prefix or "",
            "gate_skill_path": args.ticket_gate_skill or "",
        },
        "requirements_source": {
            "enabled": bool(args.requirements_docx),
            "format": "docx" if args.requirements_docx else "",
        },
        "capacity_limits": {
            "dev_log_warn_lines": 1500,
            "dev_log_error_lines": 2000,
            "bugfix_log_max_lines": 1000,
            "bugfix_log_max_entries": 40,
            "status_max_lines": 150,
            "architecture_warn_lines": 400,
        },
        "root_whitelist": ["README.md"] + [
            w.strip() for w in (args.extra_root_whitelist or "").split(",") if w.strip()
        ],
        "created": TODAY,
    }
    return cfg


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--root", default=".", help="repo root (default: current directory)")
    ap.add_argument("--project-name", required=True)
    ap.add_argument("--docs-language", default="zh", choices=["zh", "en"])
    ap.add_argument("--vcs", default="git", choices=["git", "none"])
    ap.add_argument("--ticket-prefix", default="", help="e.g. CES, JIRA-, PROJ (leave empty to disable ticket gate)")
    ap.add_argument("--ticket-gate-skill", default="", help="path to another skill that owns ticket creation (optional)")
    ap.add_argument("--commit-lang", default="en", choices=["en", "zh"])
    ap.add_argument("--requirements-docx", action="store_true", help="project keeps a .docx requirements source of truth")
    ap.add_argument("--extra-root-whitelist", default="", help="comma-separated extra filenames allowed to stay in repo root")
    ap.add_argument("--force", action="store_true", help="overwrite docs/.doc-pilot.json if it already exists")
    args = ap.parse_args()

    root = os.path.abspath(args.root)
    docs = os.path.join(root, "docs")

    for d in DIRS:
        p = os.path.join(docs, d)
        if not os.path.isdir(p):
            os.makedirs(p, exist_ok=True)
            print("MKDIR: %s" % p)

    write_if_absent(os.path.join(docs, "README.md"), seed_readme(docs, args.project_name))
    write_if_absent(os.path.join(docs, "progress", "STATUS.md"), seed_status(docs, args.project_name))
    write_if_absent(os.path.join(docs, "progress", "dev-log.md"), seed_devlog(docs, args.project_name))
    write_if_absent(os.path.join(docs, "bugs", "BUGFIX_LOG.md"), seed_bugfix_log(args.project_name))
    write_if_absent(os.path.join(docs, "overview", "architecture-snapshot.md"), seed_architecture(args.project_name))
    write_if_absent(os.path.join(docs, "deploy", "DEPLOYMENT.md"), seed_deployment(args.project_name))

    for d in ("design", "review", "qa", "requirements", "archive", "progress/history", "bugs/history"):
        gk = os.path.join(docs, d, ".gitkeep")
        if not os.listdir(os.path.join(docs, d)):
            write_if_absent(gk, "")

    cfg_path = os.path.join(docs, ".doc-pilot.json")
    if os.path.exists(cfg_path) and not args.force:
        print("SKIP  (exists, use --force to overwrite): %s" % cfg_path)
    else:
        with open(cfg_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(build_config(args), f, ensure_ascii=False, indent=2)
            f.write("\n")
        print("WROTE: %s" % cfg_path)

    print()
    print("=== doc-pilot knowledge base initialized at %s ===" % docs)
    print("Next: run `python scripts/check-docs.py --root %s` to verify." % root)


if __name__ == "__main__":
    main()
