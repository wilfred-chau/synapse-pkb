#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""check-docs.py - doc-pilot knowledge base consistency checks.

Usage:
    python check-docs.py [--root <repo-root>]

If --root is omitted, the script walks upward from the current directory
looking for docs/.doc-pilot.json (falls back to `git rev-parse
--show-toplevel`, then the current directory). All thresholds and the
optional ticket prefix are read from docs/.doc-pilot.json, so this script
never needs project-specific edits.

Cross-platform (pure stdlib). Exit code 0 = all checks passed, 1 = errors.
"""

import argparse
import json
import os
import re
import subprocess
import sys


def find_root(explicit_root):
    if explicit_root:
        return os.path.abspath(explicit_root)
    cur = os.path.abspath(os.getcwd())
    while True:
        if os.path.exists(os.path.join(cur, "docs", ".doc-pilot.json")):
            return cur
        parent = os.path.dirname(cur)
        if parent == cur:
            break
        cur = parent
    try:
        out = subprocess.check_output(
            ["git", "rev-parse", "--show-toplevel"], stderr=subprocess.DEVNULL
        )
        return out.decode().strip()
    except Exception:
        return os.path.abspath(os.getcwd())


def load_config(docs):
    cfg_path = os.path.join(docs, ".doc-pilot.json")
    if not os.path.exists(cfg_path):
        return None
    with open(cfg_path, "r", encoding="utf-8") as f:
        return json.load(f)


def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def line_count(path):
    return read(path).count("\n") + 1


class Report:
    def __init__(self):
        self.errors = []
        self.warnings = []

    def error(self, msg):
        self.errors.append(msg)

    def warn(self, msg):
        self.warnings.append(msg)


def check_required_files(docs, rpt):
    required = [
        "README.md",
        "overview/architecture-snapshot.md",
        "progress/STATUS.md",
        "progress/dev-log.md",
        "bugs/BUGFIX_LOG.md",
        "deploy/DEPLOYMENT.md",
    ]
    for rel in required:
        if not os.path.exists(os.path.join(docs, rel)):
            rpt.error("MISSING required file: docs/%s" % rel)


def check_index(docs, rpt):
    idx_path = os.path.join(docs, "README.md")
    if not os.path.exists(idx_path):
        rpt.error("Cannot check index: docs/README.md missing")
        return
    index = read(idx_path)
    for dirpath, _, files in os.walk(docs):
        for name in files:
            if not name.endswith(".md"):
                continue
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, docs).replace(os.sep, "/")
            if rel == "README.md":
                continue
            if rel not in index:
                rpt.error("INDEX MISS: docs/%s not registered in docs/README.md" % rel)


def check_bug_numbering(docs, rpt):
    bug_active = os.path.join(docs, "bugs", "BUGFIX_LOG.md")
    bug_hist_dir = os.path.join(docs, "bugs", "history")
    files = []
    if os.path.exists(bug_active):
        files.append(bug_active)
    if os.path.isdir(bug_hist_dir):
        files += [
            os.path.join(bug_hist_dir, f)
            for f in sorted(os.listdir(bug_hist_dir))
            if f.endswith(".md")
        ]
    if not files:
        rpt.error("Cannot check bugs: docs/bugs/BUGFIX_LOG.md missing")
        return

    pattern = re.compile(r"(?m)^\s*##\s.*?BUG-(\d{3,})")
    per_file = {}
    for fp in files:
        nums = sorted(set(int(n) for n in pattern.findall(read(fp))))
        per_file[os.path.basename(fp)] = nums

    all_nums = sorted(set(n for nums in per_file.values() for n in nums))
    if not all_nums:
        rpt.warn("No BUG-NNN entries found under docs/bugs/")
        return

    names = list(per_file.keys())
    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            dup = set(per_file[names[i]]) & set(per_file[names[j]])
            for d in sorted(dup):
                rpt.error("BUG-%03d appears in both %s and %s" % (d, names[i], names[j]))

    top = max(all_nums)
    missing = sorted(set(range(1, top + 1)) - set(all_nums))
    for m in missing:
        rpt.error("BUG numbering gap: BUG-%03d missing" % m)


def check_status_freshness(docs, rpt):
    status_path = os.path.join(docs, "progress", "STATUS.md")
    if not os.path.exists(status_path):
        rpt.error("Cannot check status: STATUS.md missing")
        return
    text = read(status_path)
    if not re.search(r"last-updated:\s*\d{4}-\d{2}-\d{2}", text):
        rpt.error("STATUS.md: <!-- last-updated: YYYY-MM-DD --> marker missing")
    if len(text) > 12000:
        rpt.warn("STATUS.md is large (%d chars); keep under ~150 lines" % len(text))


def check_root_stray(root, docs, cfg, rpt):
    whitelist = set((cfg or {}).get("root_whitelist", ["README.md"]))
    for name in os.listdir(root):
        full = os.path.join(root, name)
        if os.path.isfile(full) and name.endswith(".md") and name not in whitelist:
            rpt.error(
                "STRAY root doc: %s (archive it via doc-pilot Mode A, or add to "
                "root_whitelist in docs/.doc-pilot.json if intentional)" % name
            )


def check_links(docs, rpt):
    idx_path = os.path.join(docs, "README.md")
    if not os.path.exists(idx_path):
        return
    for lineno, line in enumerate(read(idx_path).splitlines(), start=1):
        for m in re.finditer(r"\]\(([^)#\s]+\.md)\)", line):
            target = m.group(1)
            if re.match(r"^(https?:|/)", target):
                continue
            if not os.path.exists(os.path.join(docs, target)):
                rpt.error("BROKEN LINK docs/README.md:%d -> %s" % (lineno, target))


def check_capacity(docs, cfg, rpt):
    limits = (cfg or {}).get("capacity_limits", {})
    dev_log = os.path.join(docs, "progress", "dev-log.md")
    if os.path.exists(dev_log):
        n = line_count(dev_log)
        warn_at = limits.get("dev_log_warn_lines", 1500)
        err_at = limits.get("dev_log_error_lines", 2000)
        if n > err_at:
            rpt.error(
                "dev-log.md has %d lines (> %d): rotate oldest day entries into "
                "progress/history/ now (see SKILL.md rotation procedure)" % (n, err_at)
            )
        elif n > warn_at:
            rpt.warn(
                "dev-log.md has %d lines (> %d): rotation threshold approaching" % (n, warn_at)
            )

    arch = os.path.join(docs, "overview", "architecture-snapshot.md")
    if os.path.exists(arch):
        n = line_count(arch)
        warn_at = limits.get("architecture_warn_lines", 400)
        if n > warn_at:
            rpt.warn(
                "architecture-snapshot.md has %d lines (> %d): consider splitting "
                "into per-domain files" % (n, warn_at)
            )

    bug_active = os.path.join(docs, "bugs", "BUGFIX_LOG.md")
    if os.path.exists(bug_active):
        text = read(bug_active)
        n = line_count(bug_active)
        cnt = len(re.findall(r"(?m)^\s*##\s.*?BUG-\d{3,}", text))
        max_lines = limits.get("bugfix_log_max_lines", 1000)
        max_entries = limits.get("bugfix_log_max_entries", 40)
        if n > max_lines or cnt > max_entries:
            rpt.error(
                "BUGFIX_LOG.md active volume too large (%d lines / %d entries): "
                "rotate into bugs/history/" % (n, cnt)
            )


def check_volume_index(docs, rpt):
    dev_log = os.path.join(docs, "progress", "dev-log.md")
    hist_dir = os.path.join(docs, "progress", "history")
    if os.path.exists(dev_log):
        head = "\n".join(read(dev_log).splitlines()[:30])
        idx_vols = set(re.findall(r"dev-log-vol\d{2}-\d{4}-\d{2}-\d{2}_\d{4}-\d{2}-\d{2}\.md", head))
        disk_vols = set(
            f for f in os.listdir(hist_dir) if f.startswith("dev-log-vol")
        ) if os.path.isdir(hist_dir) else set()
        for v in idx_vols - disk_vols:
            rpt.error("dev-log.md volume index lists %s but the file is missing in progress/history/" % v)
        for v in disk_vols - idx_vols:
            rpt.error("history volume %s is not listed in dev-log.md header volume index" % v)

    bug_active = os.path.join(docs, "bugs", "BUGFIX_LOG.md")
    bug_hist = os.path.join(docs, "bugs", "history")
    if os.path.exists(bug_active):
        head = "\n".join(read(bug_active).splitlines()[:30])
        idx_vols = set(re.findall(r"BUGFIX_LOG-vol\d{2}-BUG-\d{3,}-\d{3,}\.md", head))
        disk_vols = set(
            f for f in os.listdir(bug_hist) if f.startswith("BUGFIX_LOG-vol")
        ) if os.path.isdir(bug_hist) else set()
        for v in idx_vols - disk_vols:
            rpt.error("BUGFIX_LOG.md volume index lists %s but the file is missing in bugs/history/" % v)
        for v in disk_vols - idx_vols:
            rpt.error("bugs/history volume %s is not listed in BUGFIX_LOG.md header volume index" % v)


def check_requirements_snapshot(docs, cfg, rpt):
    if not (cfg or {}).get("requirements_source", {}).get("enabled"):
        return
    req_dir = os.path.join(docs, "requirements")
    if not os.path.isdir(req_dir):
        return
    docxs = [f for f in os.listdir(req_dir) if f.endswith(".docx")]
    if not docxs:
        return
    latest = max(docxs, key=lambda f: os.path.getmtime(os.path.join(req_dir, f)))
    latest_mtime = os.path.getmtime(os.path.join(req_dir, latest))
    md_path = os.path.join(req_dir, "requirements-full.md")
    if not os.path.exists(md_path):
        rpt.error(
            "STALE requirements snapshot: requirements-full.md missing while %s exists; "
            "re-run scripts/docx-to-md.py" % latest
        )
    elif os.path.getmtime(md_path) < latest_mtime:
        rpt.error(
            "STALE requirements snapshot: requirements-full.md older than %s; "
            "re-run scripts/docx-to-md.py" % latest
        )


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--root", default=None, help="repo root (default: auto-detect)")
    args = ap.parse_args()

    root = find_root(args.root)
    docs = os.path.join(root, "docs")
    cfg = load_config(docs)
    rpt = Report()

    if cfg is None:
        print("=== doc-pilot check-docs: repo = %s ===" % root)
        print()
        print("NO CONFIG - docs/.doc-pilot.json not found.")
        print("This project has not been initialized yet. Run:")
        print("  python scripts/init-kb.py --root %s --project-name \"<name>\"" % root)
        sys.exit(1)

    check_required_files(docs, rpt)
    check_index(docs, rpt)
    check_bug_numbering(docs, rpt)
    check_status_freshness(docs, rpt)
    check_root_stray(root, docs, cfg, rpt)
    check_links(docs, rpt)
    check_capacity(docs, cfg, rpt)
    check_volume_index(docs, rpt)
    check_requirements_snapshot(docs, cfg, rpt)

    print()
    print("=== doc-pilot check-docs: repo = %s (project: %s) ===" % (root, cfg.get("project_name", "?")))
    print()
    if rpt.errors:
        print("FAILED - %d error(s), %d warning(s):" % (len(rpt.errors), len(rpt.warnings)))
        for e in rpt.errors:
            print("  [ERROR] %s" % e)
        for w in rpt.warnings:
            print("  [WARN ] %s" % w)
        sys.exit(1)
    else:
        print("PASSED - 0 error(s), %d warning(s)" % len(rpt.warnings))
        for w in rpt.warnings:
            print("  [WARN ] %s" % w)
        sys.exit(0)


if __name__ == "__main__":
    main()
