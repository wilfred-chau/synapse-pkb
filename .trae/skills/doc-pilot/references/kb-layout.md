# docs/ 知识库结构与归档规范

## 目录结构与分类规则

```
docs/
├── .doc-pilot.json      # 项目配置（由 scripts/init-kb.py 生成，见「配置文件」一节）
├── README.md            # 文档索引（导航 + 登记表，人与 AI 共用）
├── overview/            # 系统全貌：描述系统「现在是什么样」——架构演进时同步修订
├── progress/
│   ├── STATUS.md        # 进度速览（L1 必读）：模块状态表 + 最近动态 + 待办 + 已知问题
│   ├── dev-log.md       # 活跃开发日志：按时间正序追加；超过 capacity_limits.dev_log_warn_lines 预警、
│   │                     超过 dev_log_error_lines 必须按天条目轮转最旧部分入
│   │                     history/dev-log-volNN-起始日_截止日.md（头部历史卷索引登记）
│   └── history/         # 已完结阶段史料 + dev-log 历史卷：只读，内容原样冻结
├── bugs/
│   ├── BUGFIX_LOG.md    # 缺陷修复记录（活跃卷）：纯追加，编号 BUG-NNN 全局自增，永不复用
│   └── history/         # 已轮转历史卷：BUGFIX_LOG-volNN-BUG-XXX-YYY.md，只读冻结，编号跨卷连续
├── design/              # 技术方案与决策记录：「打算怎么做」——定稿后冻结，新方案新增文件
├── plans-local/         # 本地方案草稿与执行清单（可选，本地协作沉淀；默认不进 Git、不进 README 索引）
├── review/              # 代码评审报告：快照，文件名带日期前缀，定稿冻结
├── qa/                  # 测试规划与验证清单
├── deploy/              # 部署与运维手册
├── requirements/        # 需求原文（可选）：若项目维护 docx 原件，保持只读 source of truth，
│                          旁边放机器转换的 requirements-full.md 副本（AI 可检索）；
│                          仅当 docs/.doc-pilot.json 的 requirements_source.enabled=true 时启用此路由
└── archive/             # 已废止文档：顶部加废止声明 + 指向替代文档
```

| 目录 | 判定标准 | 更新方式 |
| --- | --- | --- |
| `overview/` | 描述系统「现在是什么样」 | 架构/横切机制变更时修订对应小节 |
| `progress/` | 描述「做到哪了」 | dev-log 追加；STATUS 每次收工刷新 |
| `bugs/` | 缺陷修复记录（活跃卷 + 历史卷） | 纯追加（BUG-NNN 全局自增）；活跃卷超容量红线时轮转入 `bugs/history/`，编号延续 |
| `design/` | 「打算怎么做」的方案与决策 | 定稿冻结；需求变更走新文件或顶部追加「变更记录」 |
| `plans-local/` | 本地方案草稿、拆解清单、临时任务文档 | 本地使用；默认不纳入 README 索引，也不作为正式文档归档 |
| `review/`、`qa/` | 质量快照 | 定稿冻结，文件名带日期 |
| `deploy/` | 怎么装、怎么运维 | 随部署方式演进更新 |
| `archive/` | 已过时但有史料价值 | 只读 + 顶部废止声明 |
| `requirements/` | 需求原始输入（可选目录） | 只读 |

## 配置文件（docs/.doc-pilot.json）

这是本 skill 唯一的项目特定输入，不存在时视为「未初始化」（见 SKILL.md「模式 C：初始化知识库」）。
字段说明与默认值见 skill 内 `templates/config.example.json`。所有脚本、路由规则中出现的
项目名、工单前缀、容量阈值、需求文档开关，均从这个文件读取，而不是写死在 SKILL.md 或脚本里——
这是本 skill 能够被原样放进任意项目而无需改代码的核心机制。

## 命名规则

- 目录名与文件名一律英文 kebab-case；评审/快照类加日期前缀：`YYYY-MM-DD-<主题>.md`。
- 活文档不加日期（会被反复追加）；快照类必须带日期。唯一例外：dev-log 轮转产出的历史卷必须
  带日期（`dev-log-volNN-起始日_截止日.md`，如 `dev-log-vol01-2026-09-01_2026-10-15.md`）——
  它是被冻结的史料切片，不再追加，日期区间写进文件名供按时间检索；卷号 NN 两位严格递增。
- 保留两个专名不改：`BUGFIX_LOG.md`（编号体系入口）、`DEPLOYMENT.md`（若被外部广泛引用）。

## STATUS.md 收敛规则

- 每次 dev-log 追加后同步刷新；「最近动态」只保留最近 5~8 条，更旧的下沉到 dev-log（本就存在那里）。
- 「更新日期」同时维护正文表格与 `<!-- last-updated: YYYY-MM-DD -->` 隐藏标记（供 check-docs.py 校验新鲜度）。
- 「已知问题与坑位速查」≤10 行，只保留高发与全局性坑位（按复发率/严重度精选），细节一律指向
  `bugs/` 对应条目——这是防止「日志越大知识越稀释」的摘要层。
- 模块状态表是唯一权威进度视图，与根 README「业务模块与进度」（若存在）保持一致（两处改一处必须同步另一处）。

## 归档与迁移

若在已有项目（存在散落的历史 md 文档但从未用过 doc-pilot）引入本 skill，首次「整理归档」
（SKILL.md 模式 A）应生成一份迁移映射表（原位置 → 新位置），追加到 `dev-log.md` 首条记录中，
作为该项目知识库诞生的存档说明。不要把这类一次性迁移记录写进本文件（本文件只描述规范，不描述
某个项目的历史）。

## 例外白名单（不属于散落文档，不迁移）

根目录 `README.md` 默认保留；项目若有其他约定俗成留在根目录的文档（如 `CHANGELOG.md`、
`CONTRIBUTING.md`、测试目录就地说明文件等），在 `docs/.doc-pilot.json` 的 `root_whitelist`
数组中登记，check-docs.py 与模式 A 归档扫描都会读取这份名单，不会误判为「散落文档」。
