---
name: doc-pilot
description: 通用项目文档中枢与开发值守 skill：管理 docs/ 知识库（人与 AI 共读共写），在开发任务中强制「开工先读、收工必写」，并按任务复杂度分级选择工作流（是否需要方案确认、是否需要跨会话任务记录）。可原样安装到任意代码仓库使用，无需为每个项目手工改路径。触发场景：整理/归档/重构项目文档、初始化文档库（触发词：整理文档、归档文档、文档重构、初始化文档库、doc-pilot）；执行开发、规划、新功能、继续开发、重构、模块开发等任务时，开工前必须按本 skill 查阅 docs/ 知识库了解开发进度与系统全貌，判断任务分级后选择对应工作流，若项目配置启用了工单系统则动码前还需先过工单前置门，收工后追加开发日志并更新 STATUS.md；修复 bug、排查报错、异常、失败、回归问题时，必须先检索 docs/bugs/BUGFIX_LOG.md 避免重复踩坑，修复后按模板追加记录；用户询问本 skill 用法或清点项目里有哪些 skill 工具时，读取并展示 references/usage-guide.md；用户仅询问需求中某模块/编号的定义或范围时，检索 docs/requirements/requirements-full.md 作答，不进入开发流程。**首次在没有 docs/.doc-pilot.json 配置文件的仓库里触发本 skill 时，先走「模式 C：初始化知识库」完成一次性引导，而不是报错或假设项目结构。**
---

# doc-pilot · 通用文档中枢与开发值守

本 skill 管理 `docs/` 知识库（人与 AI 共读共写），并承担开发值守：**开工前定向读取文档，
收工后写回记录，中途按任务复杂度选择合适的工作方式**。它被设计为**项目无关**——所有项目
特定的信息（项目名、是否有工单系统、容量阈值、根目录白名单、本项目当前使用的其他 skill
路径等）都存放在 `docs/.doc-pilot.json` 配置文件里，而不是写死在本文件、CLAUDE.md 或脚本里。
这份 SKILL.md 与 `scripts/`、`references/`、`templates/` 可以原样复制到任何仓库。

doc-pilot 是全程值守角色，负责项目上下文持续读取、任务路由、规范监督与知识库写回，不是
一次性动作。`plan-before-code`、`dev-with-ticket` 等协作 skill 都在 doc-pilot 的值守范围内
运作，职责边界见 [references/skill-contract.md](references/skill-contract.md)。

## 模式 C：初始化知识库（新项目首次使用）

**触发条件**：本 skill 被激活，但目标仓库根目录下不存在 `docs/.doc-pilot.json`。

**标准动作**：

1. 用最多 4 个简短问题了解项目（不必用完整表单，聊天式问即可）：
   - 项目名称是什么？
   - 版本控制用 git 吗？（默认是，若否则后续跳过所有分支同步相关步骤）
   - 是否已经有独立的工单/需求票 skill（如 Jira/Linear 建票流程）？有的话工单前缀是什么、
     那个 skill 的路径是什么？没有就跳过，本 skill 不会引入任何工单相关流程。
   - 是否有 docx 格式的需求原文需要维护为可检索快照？commit message 习惯用中文还是英文？
     （这两项不确定可用默认值：不启用需求源、commit 用英文。）
2. 调用 `scripts/init-kb.py`，把上面的答案转成参数，例如：
   ```bash
   python scripts/init-kb.py --root . --project-name "示例项目" \
     --vcs git --commit-lang en \
     --ticket-prefix "" --ticket-gate-skill "" \
     [--requirements-docx] [--extra-root-whitelist "CHANGELOG.md"]
   ```
   该脚本会创建 `docs/` 目录树（`overview/ progress/ bugs/ design/ review/ qa/ deploy/
   requirements/ archive/` 及各自 history 子目录）、写入种子文件（README/STATUS/dev-log/
   BUGFIX_LOG/architecture-snapshot/DEPLOYMENT 的初始版本），并生成 `docs/.doc-pilot.json`。
3. 运行 `python scripts/check-docs.py` 确认全绿，向用户简要汇报初始化结果。
4. 之后才继续处理用户最初提出的那个任务（进入模式 A/B）。

已存在 `docs/.doc-pilot.json` 的项目永远不会重复触发本模式；后续所有路由都直接读取该配置。
配置字段含义见 [references/kb-layout.md](references/kb-layout.md)「配置文件」一节与
[templates/config.example.json](templates/config.example.json)。**本项目当前使用的其他
协作 skill（如 plan-before-code、dev-with-ticket）的实际路径，同样登记在此配置文件的
`skills_registry` 字段，不在 SKILL.md 或 CLAUDE.md 正文里硬编码路径**——这样宿主工具变化
（比如从一个 Agent 工具换到另一个）时，只需改一处配置，不需要逐份文档找路径改。

## 三条铁律

1. **先同步再动手**（仅当配置 `vcs = "git"`；`vcs = "none"` 时跳过本条）：每次任务开工第一步
   先确认当前分支代码是最新——`git fetch` 后用 `git status -sb` 比对当前分支与上游；远程有
   更新必须先 `git pull --ff-only` 拉取，并按 [references/duty-routes.md](references/duty-routes.md)
   「远程拉取的记录规范」做记录，再进入开工读取路由。拉取与本地未提交改动冲突时报告用户裁决，
   不擅自 stash/rebase/merge。`add`/`commit`/`push` 除非用户明确授权，默认由用户手动执行。
2. **先读后做**：任何开发 / bug 修复 / 重构 / 测试任务，动手写代码前必须完成下方「模式 B」
   的开工读取路由，并判断任务分级（见下方「任务分级」一节）；任何 bug 修复前必须先检索
   `docs/bugs/BUGFIX_LOG.md`。
3. **做完必写**：任务完成后必须按模板写回（开发日志 / BUG 记录 / STATUS 更新 / 任务记录收尾）。
   没有记录的工作等于没做——下个会话的你会因此重复踩坑。

## 元路由：被问到「怎么用本 skill / 有哪些 skill」时

用户询问 doc-pilot 的用法 / 使用说明 / 怎么调用时，不要自由发挥，第一动作是读取并完整展示
[references/usage-guide.md](references/usage-guide.md)（用户手册：唤起方式、任务流程、可复制
提示词、纠偏话术、FAQ）。

用户问「这个项目里有什么 skill / skill 列表 / skill 工具」时：先动态列出当前项目 skills 目录
（如 `.cline/skills/`、`.claude/skills/`、`.trae/skills/` 等，按实际使用的 agent 工具而定）
下的全部 skill，再展示 usage-guide.md 作为 doc-pilot 的说明；若用户问的是其他某个 skill 的
用法，直接读取那个 skill 自己的 SKILL.md 作答。**不要假设固定的 skill 清单**——本 skill 不
硬编码同项目里还装了哪些其他 skill，实际登记信息以 `docs/.doc-pilot.json` 的 `skills_registry`
为准。

## 需求速查路由：被问到「某模块/编号是什么」时（仅当 `requirements_source.enabled = true`）

用户问「某编号是什么」「某模块要做什么」「某范围」这类**纯定义/范围查询**时，不进入模式 B
开工流程，标准动作：

1. 检索 `docs/requirements/requirements-full.md`（编号定义表），命中行直接引用原文作答，
   注明出处为需求文档。
2. 用户追问进展/是否完成时，补读 `docs/progress/STATUS.md` 模块表对应行（需求≠现状，两者
   要分开答）。
3. 纯查询无产出，不写回 dev-log。**不直读 docx**：查询一律走 md 副本检索；`check-docs.py`
   会校验「md 不比 docx 旧」，过期即报错，此时先执行
   `python scripts/docx-to-md.py docs/requirements/<name>.docx docs/requirements/requirements-full.md`
   重转再作答（无 Python 时降级 `.ps1`，仅 Windows）。

若 `requirements_source.enabled = false`（默认值），本路由不生效，此类问题按普通问答处理即可。

区分口径：问「是什么/范围」→ 本路由；问「怎么开发/继续做」→ 模式 B-开发。

## 知识库地图（渐进式加载）

| 层级 | 文件 | 何时读 | 体量约束 |
| --- | --- | --- | --- |
| L1 必读 | `docs/progress/STATUS.md` | 每次开发/修复任务开工 | 见配置 `status_max_lines`（默认 <150 行） |
| L1 必读 | `docs/bugs/BUGFIX_LOG.md` | 修 bug / 排查问题前（检索相似案例与经验教训） | 纯追加，按 BUG-NNN 检索 |
| L2 选读 | `docs/overview/architecture-snapshot.md` | 需要了解系统全貌/架构约定时 | 见配置 `architecture_warn_lines`，超限按域拆分 |
| L2 选读 | `docs/design/*.md`、`docs/review/*.md`、`docs/qa/*.md` | 任务涉及对应模块/方案/测试时 | 按相关性选 1~2 个 |
| L2 选读 | `docs/deploy/DEPLOYMENT.md` | 部署/运维/升级任务 | 单文件 |
| L2 选读 | `docs/requirements/requirements-full.md`（若启用） | 问模块/编号定义、需求范围对照时 | 只读快照，docx 更新后重转 |
| L3 检索 | `docs/progress/history/*.md`、`docs/bugs/history/*.md`、`docs/archive/*` | 按相关性主动检索历史经验，不因为不是必读层就完全忽略 | 只读，先定位再读相关段落，禁止整读 |

历史检索优先维度：模块名 → 技术组件 → 异常关键词 → 需求编号 → 类似实现方式。
完整路由细则见 [references/duty-routes.md](references/duty-routes.md)。

## 任务分级：判断走多重的工作流

完成开工读取后，在动手实现前先判断本次任务的复杂度分级，避免两种极端——简单任务被拖入
冗长的方案确认流程，或复杂任务在没有对齐实现路径的情况下直接下手写代码。

| 分级 | 特征 | 参考信号 | 工作流 |
| --- | --- | --- | --- |
| L1 微小明确 | 单点修改，实现路径显而易见，不需要设计 | 预计涉及 1 个文件；改动类型属于文案/配置/明显条件错误/排序调整 | 读取 → 直接实现 → 自检 → 写回 |
| L2 小范围开发/修复 | 逻辑清晰，无架构分叉 | 预计涉及 2～3 个文件；不新增模块、不改数据结构 | 读取 → 简要说明思路 → （按配置决定是否建票）→ 实现 → 自检 → 写回 |
| L3 中型功能 | 涉及多模块或存在实现分叉 | 预计涉及 4 个及以上文件；新增模块；改动会影响既有接口或数据结构 | 读取 → 历史经验检索 → 触发 `plan-before-code` → 用户确认 → 建票（按配置）→ 实现 → 自检 → 写回 |
| L4 大型/高风险 | 跨模块、影响架构、数据库结构变更、多阶段实施 | 涉及数据库 DDL；多阶段上线；高回归风险 | 读取 → 深入分析 → 触发 `plan-before-code` → 用户确认 → 建票 → 分阶段实现 → 多轮验证 → 写回 |

「预计涉及文件数」只是校准主观判断的参考线，不是唯一标准——如果凭经验判断某个改动虽然
只碰 1 个文件但风险很高（比如改的是鉴权核心逻辑），应按更高分级处理；反过来，凭直觉觉得
分级偏高时，可以先用工具快速搜索一下相关类名/接口名实际出现在多少个文件里，用检索结果
校正判断，而不是单凭印象拍板。

**分级判断结果需要在回复里用一句话说明**，例如「本次判断：L2，预计涉及 2 个文件，直接
进入实现」，不需要展开解释，但要让用户能看到这一步判断本身，而不是完全隐藏在内部推理里。

若用户明确要求「直接改」「不用出方案」：L1/L2 可以直接执行；L3/L4 若属于明显高风险/架构
任务，应先简短提示跳过方案确认的风险，再遵从用户的明确决定，不得无限阻塞开发。

## 跨会话任务记录：什么时候需要，什么时候不需要

L1/L2 任务通常在一次对话内就能完成，**不需要**创建独立的跨会话任务记录，走完知识库写回
闭环（dev-log/STATUS）就够了。

L3 任务视情况决定：如果预计能在当前会话内做完，同样只需要写回闭环；如果预计跨会话、或者
用户中途要求暂停稍后继续，再创建任务记录。

L4 任务，以及任何用户明确要求暂停/稍后继续的任务，**必须**创建任务记录，避免下一次会话
重新摸索"做到哪里了"。

创建方式：调用 `scripts/task-context.py create <task_id> "<标题>" --level L4`，任务记录存放于
`docs/.doc-pilot/tasks/`，这是**运行态记忆**，不是长期知识库的一部分，不参与容量轮转规则，
也不能替代 dev-log/STATUS/BUGFIX_LOG——重要成果仍然必须写进长期知识库,任务记录只负责回答
"当前做到哪一步"。

最小记录内容：任务 ID、标题、分级、当前阶段（见下）、关联工单号（如有）、已完成步骤、
当前步骤、下一步、遇到的阻塞。

**任务当前阶段**（比起穷举每个细粒度动作，只标记几个真正需要跨会话恢复时能立刻定位的
关键节点即可）：

```
待处理 → 方案确认中 → 已确认待建票 → 开发中 → 验证中 → 已完成
```

异常情况：`已暂停`（用户主动暂停，可恢复）、`已阻塞`（需要外部依赖或决策）、
`验证未通过`（实现存在但验证没过）。

阶段发生变化时才更新记录（比如"方案确认中 → 已确认待建票"这种节点切换才写一次），不需要
每改一行代码都更新；**阶段变化的同时，也要在当次回复里用一句话让用户看到**（例如"当前
阶段：已确认待建票 → 进入建票环节"），不要只悄悄改 JSON 文件而不在对话里体现，这样用户
不用专门去翻文件才能知道流程走到哪。

**任务记录不是事实来源**。恢复会话、读取任务记录后，如果记录内容和实际代码/Git 状态对不上，
以代码和 Git 的真实状态为准，并顺手修正任务记录。

用户说「继续上次的任务」时的恢复流程：读 `STATUS.md` 找到进行中的任务 → 读对应任务记录 →
读最近的 dev-log 条目 → 必要时读相关代码 → 确认状态一致后再继续。

任务收尾时，把任务记录的阶段标记为「已完成」「已暂停」或「已阻塞」之一，并确保长期知识库
（dev-log/STATUS/BUGFIX_LOG）已经写入了对应成果——任务记录本身可以在任务完成一段时间后
按项目习惯清理，清理前必须确认长期成果已经沉淀，不会因为清理任务记录而丢失。

## 模式 A：文档整理归档

触发：用户要求整理/归档文档、发现根目录或非约定位置散落新文档、索引缺失。若 `docs/.doc-pilot.json`
不存在，先走「模式 C」完成初始化，再执行本模式。

1. 扫描仓库根目录与一级子目录的散落 `*.md`（白名单：`docs/.doc-pilot.json` 的 `root_whitelist`
   数组，默认只含根 `README.md`，加上 `docs/**`、常见 agent 配置目录如 `.cline/**`/`.claude/**`/
   `.trae/**`、`.github/**`）。
2. 按 [references/kb-layout.md](references/kb-layout.md) 的分类规则判定归属目录，重命名为
   英文短名（kebab-case，评审/快照类带日期前缀）。
3. `vcs = "git"` 时已跟踪文件用 `git mv`（保留历史），未跟踪文件用普通移动命令；`vcs = "none"`
   时统一用普通移动命令。
4. 超大追加型日志超过配置阈值时按「天条目」边界整体轮转入
   `docs/progress/history/dev-log-volNN-起始日_截止日.md`，活跃段另立 `dev-log.md`。
5. 已过时但仍有史料价值的文档不移除，移入 `docs/archive/` 并在文件顶部加「废止声明 + 指向
   替代文档」。
6. 刷新 `docs/README.md` 索引与 `docs/progress/STATUS.md`；同步修复全仓库对旧路径的引用
   （根 README、其他文档内链接）。
7. 运行校验脚本，全部通过后才算完成：

```bash
python scripts/check-docs.py
```

## 模式 B：开发值守（日常）

### 开工读取路由

| 任务类型 | 必读 | 按需选读 |
| --- | --- | --- |
| 新功能 / 规划 / 继续开发 / 重构 | `STATUS.md` → `overview/architecture-snapshot.md` | 相关 `design/` 方案、`dev-log.md` 尾部近期条目、`history/` 对应阶段、需求快照（若启用）对应章节 |
| 修 bug / 报错 / 异常 / 排查 / 回归 | `bugs/BUGFIX_LOG.md`（先检索相似 BUG 与「经验教训」） | `overview/`、涉及模块的 design、review 中已知问题 |
| 架构调研 / 了解系统全貌 | `overview/architecture-snapshot.md` | 各 design、history、STATUS |
| 部署 / 运维 / 升级 | `deploy/DEPLOYMENT.md` | `archive/`、根 README 运维章节 |
| 测试 / 用例编写 | `qa/` 下相关测试规划文档 | `dev-log.md` 中相关里程碑条目（踩坑密集） |

细则与检索技巧见 [references/duty-routes.md](references/duty-routes.md)。

### 与协作 skill 的分工

完成开工读取、判断任务分级之后，按分级决定是否引入协作 skill，职责边界见
[references/skill-contract.md](references/skill-contract.md)：

- L3/L4：先触发 `plan-before-code` 生成方案，用户确认后，方案摘要（而不是原始需求文本）
  作为后续建票的 Description。
- 若配置启用了工单系统，方案确认之后（L1/L2 则在开工读取之后）进入下方「开发前置建票门」。

### 开发前置建票门（仅当配置 `ticket_system.enabled = true`）

若配置里启用了工单系统，开发/修复/迭代任务在完成开工读取路由、按分级需要的方案确认（如有）
之后、写第一行代码之前，必须先按 `ticket_system.gate_skill_path` 指向的那个 skill 完成建票
（查重 → 草稿确认 → 建票回报票号，具体细则以那个 skill 自己的定义为准，本 skill 不重复
定义）。权限边界：票的状态流转由用户手动执行；纯文档/查询/复盘任务不开票。建票门工具不可用
时（skill 未加载/鉴权失败），暂停并向用户说明具体原因，征得同意后再继续，不得未经确认自行
跳过。

**若 `ticket_system.enabled = false`（默认值），完全跳过本节，不提及任何工单相关流程。**

### 验证：代码完成不等于任务完成

任何改码任务，收工前必须完成：编译/构建通过、实际启动或可运行验证、核心改动链路的人工
验证；涉及接口联调的要实际联调而不是只看单侧日志；涉及数据库改动的要通过导入符合业务
逻辑的种子数据验证实际数据链路，不能只做结构修改不验证。

因环境变量、外部服务、端口占用等原因导致本地验证失败时，必须先继续排查；确实无法消除的，
要在汇报中明确说明阻塞原因、已尝试的动作和当前未完成的验证项，不能把任务表述为「已完成」，
任务记录（如果创建了）应标记为「验证未通过」而不是「已完成」。

### 收工写回闭环

| 情形 | 动作 |
| --- | --- |
| 完成一个开发阶段/功能 | 按 [templates/devlog-entry.md](templates/devlog-entry.md) 追加到 `docs/progress/dev-log.md` 末尾（若启用工单系统且本次动码前建了票，记录票号）；更新 `STATUS.md`（模块表/最近动态/更新日期） |
| 修复一个 bug | 按 [templates/bugfix-entry.md](templates/bugfix-entry.md) 追加 `BUG-NNN` 到 `docs/bugs/BUGFIX_LOG.md`（编号=全库最大值+1，含 `history/` 历史卷），并同步更新 `STATUS.md` 已知问题区 |
| 架构/横切机制发生变化 | 修订 `overview/architecture-snapshot.md` 对应小节 |
| 新增了一份正式文档 | 登记 `docs/README.md` 索引 |
| 产生了新的踩坑经验 | 写进对应 BUG 条目「经验教训」或 dev-log「过程要点」 |
| 存在任务记录（L4 或跨会话的 L3） | 标记为「已完成」「已暂停」或「已阻塞」之一 |
| 完成任何改码任务 | 若 `vcs = "git"`，收工汇报末尾主动提供**可直接粘贴的 git commit 文案**与**本次建议执行 `git add` 的文件清单**（格式见下）；`add`/`commit`/`push` 仍由用户手动执行，除非用户明确授权代为执行 |

一个任务只有同时满足以下条件才算真正收尾：实现完成；必要验证已完成或失败项已明确声明；
STATUS 已更新；dev-log/BUGFIX_LOG 已按情形写回；任务记录（如果存在）已关闭；需要建票的
任务已完成建票流程。

**commit 文案格式**：代码块包裹、可整段粘贴，语言取自配置 `commit_message.language`
（默认英文），若 `commit_message.conventional_commits = true`（默认）则首行 =
`[<TICKET-NNN>] type(scope): summary`（conventional commits：feat/fix/refactor/docs/test/chore，
≤72 字符；未启用工单系统或无关联票时省略 `[<TICKET-NNN>]` 前缀）；空一行；正文 3~5 条要点
（what/why/verification）。

**`git add` 文件清单格式**：紧跟在 commit 文案之后给出，仅列出**本次任务相关且建议纳入提交**的文件；优先按文件路径逐行列出，必要时可直接给出可粘贴执行的 `git add <file1> <file2> ...` 命令。若工作区存在不应纳入本次提交的文件，需明确说明哪些文件应暂不 `add`。

### 容量红线（防单文件无限膨胀，`check-docs.py` 超限即报错；阈值以 `docs/.doc-pilot.json`
的 `capacity_limits` 为准，以下为默认值）

| 文件 | 默认红线 | 触发动作 |
| --- | --- | --- |
| `docs/progress/dev-log.md` | 预警 > 1500 行；超限 > 2000 行 | 按「天条目」（`## YYYY-MM-DD`，单条永不跨卷）整体搬移最旧若干天入 `progress/history/dev-log-volNN-起始日_截止日.md`（卷号 NN 两位严格递增，日期区间入文件名，整体复制原文），并在 dev-log.md 头部「历史卷索引」与 `docs/README.md` 各登记一行。轮转步骤：① 以天条目为边界定切割线（活跃卷保留最近约 600 行）→ ② 新建历史卷文件（头部注明来源与行号范围）→ ③ 从活跃卷删除已搬移段 → ④ dev-log 头部索引加一行 → ⑤ docs/README.md 登记 → ⑥ 跑 `check-docs.py` 全绿才算完成 |
| `docs/bugs/BUGFIX_LOG.md` | > 1000 行 或 > 40 条 | 轮转：当前卷改名为 `bugs/history/BUGFIX_LOG-volNN-BUG-XXX-YYY.md`（编号区间写进文件名），新建只含文件头的空活跃卷，**编号全局延续、永不重置** |
| `docs/progress/STATUS.md` | > 150 行；坑位速查 > 10 行 | 「最近动态」删最旧条目；坑位速查只留高发/全局性条目，细节指向 BUGFIX_LOG |
| `docs/overview/architecture-snapshot.md` | 预警 > 400 行（此文件天然比 STATUS 大，只预警不设 ERROR） | 拆分成按域/子系统的多个文件，并在本 SKILL.md 或 README 保留一份「何时读哪个子文件」的索引表 |

轮转后跨卷规则：修 bug 检索面向 `docs/bugs/` 全目录（含历史卷）；新 BUG 编号 = 全库（含历史
卷）最大值 + 1；编号连续性与跨卷重复由 `check-docs.py` 校验。**任务记录不参与本节的容量
轮转规则**，它是运行态记忆，按项目习惯清理即可。

## 长会话上下文保护

当前会话上下文明显膨胀、或任务本身预计跨会话时：

1. 若任务分级达到需要任务记录的程度，先更新任务记录。
2. 更新必要的 dev-log/STATUS。
3. 输出一份下一次会话可直接复用的恢复摘要，至少包含：项目核心技术栈、当前任务（及任务
   ID/工单号，如有）、当前所处阶段、已完成内容、当前阻塞、下一步计划、涉及的关键文件。
4. 建议用户开启新会话。

## 硬性约束

- 修改含中文的内容文件一律用编辑工具直接改，避免 Windows 下 PowerShell 文本管道
  （`Get-Content | Set-Content`）导致的双重编码问题；同理，任何脚本在发起包含中文内容的
  网络请求（如调用第三方 API）时，必须显式按 UTF-8 编码处理请求体，不能依赖运行环境的
  默认编码。
- 若项目在 Windows 上使用 `.ps1` 脚本，新建的 `.ps1` 建议 UTF-8 带 BOM、内容尽量纯 ASCII，
  以避开编码事故；本 skill 的主校验脚本已改为跨平台的 `check-docs.py`，仅 docx 转换在无
  Python 环境时才需要 `.ps1` 降级版本。
- `history/`、`archive/`、`review/` 为只读史料，永不改写内容，只允许加顶部声明。
- 大文件读取策略：已切分的历史日志禁止尝试整读；先读 `STATUS.md` 定位阶段，再读对应史料的
  相关段落。
- 本 skill 不假设任何特定技术栈、工单系统或 CI 工具存在；涉及项目特定信息（包括其他协作
  skill 的实际路径）一律先查 `docs/.doc-pilot.json`，查不到就当作「未配置」处理，不臆测
  默认值以外的行为。
- 涉及数据库的写操作，先区分是否为结构性变更（建表、改字段、加索引、迁移脚本等）——结构性
  变更在真正执行前，先把即将运行的语句和影响范围说清楚；普通的数据增删改可以在完成必要
  验证准备后直接执行，不需要逐条确认。具体项目的数据库连接方式、只读/写入操作的执行路径，
  以该项目 CLAUDE.md 或等价规则文件中的约定为准。
