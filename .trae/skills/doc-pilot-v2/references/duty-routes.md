# 开发值守路由细则

## 提示词触发词 → 行为对照

| 用户提示词包含 | 激活行为 |
| --- | --- |
| 整理文档 / 归档文档 / 文档重构 / 初始化文档库 / doc-pilot | 模式 A：整理归档流程（`docs/.doc-pilot.json` 不存在时先走模式 C 初始化） |
| 继续 / 开发 / 实现 / 新功能 / 规划 / 重构 / 具体模块名或编号 | 模式 B-开发：STATUS → overview → 相关 design → 开工 |
| bug / 编号-数字 / 报错 / 异常 / 失败 / 崩溃 / 4xx / 5xx / 排查 / 修复 / 回归 | 模式 B-修复：BUGFIX_LOG 检索 → 定位 → 修复 → 追加记录 |
| 全貌 / 架构 / 现状 / 梳理系统 / 调研 | overview 精读，按需 design/history |
| 部署 / 上线 / 更新包 / 回滚 / 运维 | deploy/DEPLOYMENT.md |
| e2e / 用例 / playwright / 测试规划 | qa/ + dev-log 中相关测试里程碑条目 |
| 怎么用 / 如何使用 / 使用说明 / 调用方法 / doc-pilot 怎么用 | 元路由：读取并展示 skill 内 `references/usage-guide.md`（用户手册） |
| 有什么 skill / 哪些 skill / skill 列表 / skill 工具 | 元路由：列出当前项目 skills 目录清单，并展示 `references/usage-guide.md` |
| 「某模块是什么」「某编号要做什么」「某范围」等模块/编号定义查询 | 需求速查（仅当 `requirements_source.enabled=true`）：检索 `requirements/requirements-full.md` 编号定义表引用原文作答（纯查询不开工不写回）；问进展则读 STATUS 模块表 |

以上为语义匹配兜底，非穷举；拿不准时先读 `docs/progress/STATUS.md`（成本极低）。

## 开工读取的标准动作

0. **分支同步检查**（铁律 1，仅当 `docs/.doc-pilot.json` 的 `vcs = "git"` 时执行；`vcs = "none"`
   跳过本步）：`git status` 看工作区 → `git fetch` → `git status -sb` 比对当前分支与上游。无更新
   直接进第 1 步；有更新先 `git pull --ff-only`（与本地未提交改动冲突时停下报告用户，不擅自
   stash/rebase/merge），并按下方「远程拉取的记录规范」做记录。
1. 读 `docs/progress/STATUS.md` 全文（受 `capacity_limits.status_max_lines` 约束，默认 <150 行）——
   对齐：模块进度、最近动态、当前焦点、已知问题、关键约定速查。
2. 按任务相关性选读 L2（见 SKILL.md 路由表），一次不超过 2 个大文件。
3. 检索而非通读 L3：先从 STATUS「最近动态」或文件名判断阶段，再读 `history/` 对应文件的相关
   段落（文件内可用标题跳读）。

## 远程拉取的记录规范

拉取到新提交后必须「根据最新变动做记录」，最小动作：

1. `git log --oneline <拉取前HEAD>..HEAD` 列出新提交，`git show --stat` 看涉及文件面。
2. 结构性变动（目录移动/模块新增/依赖或构建配置变更）：核对全仓库旧路径引用是否被新提交同步
   修复；影响开发流程的变动必须同步修订 `overview/architecture-snapshot.md` 等受影响文档。
3. 无论大小都在 `dev-log.md` 留痕：结构性变动单独成条（背景/产出/验证/遗留四段式）；纯小变动
   可并入当日已有条目一句话带过——防膨胀（容量红线意识），但不允许零记录。

## 修 bug 的标准前置动作

1. 先看 `BUGFIX_LOG.md` 头部「历史卷索引」（卷名 | BUG 编号区间 | 覆盖模块/关键词 | 日期区间）
   判断关键词命中哪个卷，再检索 `docs/bugs/` 全目录（活跃卷 `BUGFIX_LOG.md` + `history/` 历史卷，
   若已轮转）：按「现象关键词 + 模块关键词 + 报错码」检索是否同类复发；索引未命中的历史卷默认
   不打开全文，编号全局连续，历史卷条目同样有效。
2. 命中历史条目 → 优先核对「根因」与「经验教训」是否适用当前场景；复发类 bug 要在新增量里标注
   「复发于 BUG-NNN」。
3. 未命中 → 读 `overview/architecture-snapshot.md` 相关小节（横切机制通常是误伤高发区），
   再定位代码。
4. 高发坑位提示应按项目沉淀在 `STATUS.md` 的「已知问题与坑位速查」表，而不是写死在本文件——
   本文件描述的是通用方法，不是某个项目的具体坑位清单。

## 大文件检索技巧

- `BUGFIX_LOG.md` 条目结构固定：`## [日期] BUG-NNN: 标题` + 现象/根因/修复/验证/经验教训——
  按标题行跳读即可；活跃卷超红线会按卷轮转入 `history/`（容量红线见 SKILL.md），因此检索永远
  面向 `docs/bugs/` 目录而非单个文件。
- `history/` 各文件顶部有导航块，标注了所覆盖的原日志行号范围与阶段内容；dev-log 轮转产出的
  历史卷按 `dev-log-volNN-起始日_截止日.md` 命名，按日期检索，完整卷清单见 dev-log.md 头部
  「历史卷索引」。
- 大型 `design/` 方案文档若有需求映射总表或任务拆解小节，查具体子问题时先看这两类小节，
  再读全文。

## 收工检查单

- [ ] dev-log 或 BUGFIX_LOG 已按模板追加
- [ ] STATUS.md 的模块表/最近动态/更新日期（含 last-updated 隐藏标记）已刷新
- [ ] 架构有变 → architecture-snapshot.md 对应小节已修订
- [ ] 新增文档 → docs/README.md 索引已登记
- [ ] 可选：运行 `python scripts/check-docs.py` 全绿
