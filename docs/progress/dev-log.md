# 个人知识库系统（PKB） · 开发日志（活跃卷）

> 按时间正序追加，一事一条。条目模板见 skill 内 `templates/devlog-entry.md`。
> 超过阈值（见 docs/.doc-pilot.json 的 capacity_limits）须按「天条目」轮转入 history/，
> 轮转后在下方登记一行。

## 历史卷索引

_(暂无轮转卷)_

---

## 2026-09-05｜知识库初始化

**背景**：doc-pilot skill 首次接入本项目，生成 docs/ 知识库骨架与配置文件。

**产出**：
- docs/ 目录树与种子文件
- docs/.doc-pilot.json 配置文件

**验证**：
- 运行 `python scripts/check-docs.py` 应全绿（除「尚无模块」等占位提示）。

**遗留/Next**：
- 补充 overview/architecture-snapshot.md 与模块进度总览。

## 2026-09-05｜部署基线修正与服务器现状盘点

**背景**：需求文档中的部署环境仍写为 Ubuntu，但当前实际部署目标为内网 CentOS Stream 9 服务器；需要先摸清机器现状，避免后续按错误环境准备依赖。

**产出**：
- 修正需求文档中关于部署环境的描述，统一为 `192.168.106.130 / CentOS Stream 9`
- 在 `docs/deploy/server-inventory.md` 中记录登录方式、资源情况、已安装服务、端口占用与待安装项
- 补充 `docs/overview/architecture-snapshot.md`、`docs/deploy/DEPLOYMENT.md`、`docs/progress/STATUS.md`
- 将需求原件与 Markdown 快照纳入 `docs/requirements/`

**过程要点（踩坑与决策）**：
- 当前仓库不是 git 仓库，因此 doc-pilot 初始化时显式配置为 `vcs = none`
- 服务器已配置 SSH Key 免密登录，后续按台账方式统一管理连接信息
- 现有服务器可复用 Java 17、Maven、Node 18、Redis、Nginx、MySQL、Jenkins、Nacos；PKB 仍坚持 PostgreSQL + pgvector 方案，不因机器已有 MySQL 而改需求

**验证**：
- 通过 SSH 执行只读盘点命令，确认 OS、运行时、systemd 服务、监听端口与目录现状
- 确认服务器系统为 CentOS Stream 9，关键端口包含 `22/3306/6379/8848/9848/9849/7848/10688`

**遗留/Next**：
- 输出并确认需要 root 权限执行的安装命令（Docker / Docker Compose Plugin / PostgreSQL 16 / pgvector / python3-pip）
- 根据现有资源与端口占用，设计 PKB 的部署目录、Compose 编排与 Nginx 入口

## 2026-09-05｜服务器基础依赖安装完成

**背景**：PKB 的目标技术栈要求 Docker Compose 与 PostgreSQL 16 + pgvector，但当前 CentOS 服务器仅具备 Java/Node/Redis/MySQL/Nacos/Jenkins 等既有基础设施，尚不足以直接承载 PKB。

**产出**：
- 安装 `python3-pip`
- 安装 `docker-ce`、`docker-compose-plugin`，并启动 `docker.service`
- 将 `chau` 加入 `docker` 组
- 安装 `postgresql16`、`postgresql16-server`、`postgresql16-contrib`、`pgvector_16`
- 初始化 PostgreSQL 16 数据目录并启动 `postgresql-16.service`

**过程要点（踩坑与决策）**：
- 服务器原本安装了 `podman-docker` 兼容包，会与 `docker-ce` 冲突；安装 Docker 前必须先移除该兼容包。
- `pgvector` 在 PGDG 仓库中有现成 `pgvector_16` 包，因此不需要源码编译，也不需要额外解决 `postgresql16-devel` 的依赖链。
- PostgreSQL 当前仅监听本机回环地址（`127.0.0.1` / `::1`），这对容器内或本机访问更安全，后续是否开放外部访问由部署方案决定。

**验证**：
- `docker --version` -> `29.8.0`
- `docker compose version` -> `v5.5.1`
- `python3 -m pip --version` -> `21.3.1`
- `psql --version` -> `16.15`
- `systemctl is-active docker` / `postgresql-16` / `redis` 均为 `active`
- `SELECT name FROM pg_available_extensions WHERE name='vector';` 返回 `vector`

**遗留/Next**：
- 规划 PKB 的部署目录、Compose 文件、环境变量和 Nginx 入口
- 明确是否复用系统 Redis，还是为 PKB 保持独立容器/实例边界

## 2026-09-05｜统一 PostgreSQL 超级用户密码

**背景**：后续 PKB 部署和数据库初始化需要稳定、可追溯的 PostgreSQL 管理员口令，因此将服务器上的 `postgres` 超级用户密码统一为项目约定值。

**产出**：
- 完成 PostgreSQL `postgres` 用户密码统一设置
- 在 `docs/deploy/server-inventory.md` 中记录数据库凭据与验证结果
- 在 `docs/deploy/DEPLOYMENT.md` 与 `docs/progress/STATUS.md` 中补充密码约定

**过程要点（踩坑与决策）**：
- 使用本机 Unix 账户 `postgres` 执行 `ALTER ROLE postgres WITH PASSWORD '***'`
- 为确保后续容器或应用链路可用，验证时使用 `127.0.0.1` 的 TCP 方式而非本地 peer 方式
- 当前实例的 `password_encryption` 为 `scram-sha-256`

**验证**：
- `ALTER ROLE` 执行成功
- 使用 TCP 方式成功登录 `postgres` 数据库
- `SHOW password_encryption;` 返回 `scram-sha-256`

**遗留/Next**：
- 在 PKB 的环境变量设计中统一引用该 PostgreSQL 凭据
- 后续如需对外开放数据库访问，再单独评估 `pg_hba.conf` 与防火墙策略

## 2026-09-05｜开放 Navicat 直连 PostgreSQL

**背景**：当前 PostgreSQL 仅监听本机回环地址，虽然服务器本地和 SSH 隧道可用，但本机 Navicat 不能直接访问 `192.168.106.130:5432`，需要补齐监听地址和 `pg_hba` 白名单。

**产出**：
- 将 PostgreSQL `listen_addresses` 调整为 `127.0.0.1,192.168.106.130`
- 在 `pg_hba.conf` 中增加白名单访问规则
- 重启 `postgresql-16.service`
- 更新部署与服务器台账文档，记录当前对外开放策略

**过程要点（踩坑与决策）**：
- 没有直接全网开放，而是仅对白名单客户端放行。
- 操作前对 `postgresql.conf` 与 `pg_hba.conf` 做了带时间戳的备份。
- 当前 `firewalld` 处于 `inactive` 状态，因此本次无需额外开放防火墙端口。

**验证**：
- PostgreSQL 已监听服务器网卡地址的 `5432`
- 本机 TCP 连通性验证通过
- `SHOW listen_addresses;` 返回包含本机回环与服务器网卡地址

**遗留/Next**：
- 若你后续从别的客户端机器访问，需要继续往 `pg_hba.conf` 追加对应来源 IP
- 如需提升安全性，后续可把管理账号和业务库账号拆开，避免长期直接使用 `postgres`

## 2026-09-05｜前后端基础骨架与 Maven 基线初始化

**背景**：项目已完成需求、部署和环境准备，但仓库内还没有实际代码结构；同时需确保后续 Java 构建统一使用指定的 Maven 安装目录与本地仓库路径，避免回落到 C 盘。

**产出**：
- 新增根聚合 `pom.xml`，支持在仓库根目录执行 Maven 构建
- 新增项目级 `settings.xml` 与 `.mvn/maven.config`，将本地仓库固定到 `F:\software\maven_repo`
- 调整 `F:\maven\apache-maven-3.9.16\conf\settings.xml` 与用户级 `C:\Users\ChauTsuen\.m2\settings.xml`
- 修正用户级 `MAVEN_HOME` / `M2_HOME` 到 `F:\maven\apache-maven-3.9.16`
- 搭建 `backend/` Spring Boot 3 骨架（启动类、基础配置、测试类）
- 搭建 `frontend/` React 18 + TypeScript + Vite + Ant Design 骨架（入口、页面壳、Vite 代理、构建配置）

**过程要点（踩坑与决策）**：
- 当前 PowerShell 环境里同时存在多个 Maven 路径与历史环境变量，单看 PATH 不足以判断最终实际版本，因此额外校验了 `Get-Command mvn` 与 `where mvn`
- 为了让项目根直接支持 `mvn clean verify`，采用了“根聚合 POM + backend 子模块”的结构，而不是只保留单独的 `backend/pom.xml`
- 前端阶段只保留基础壳和构建链路，不提前引入路由业务页、状态管理或接口实现，避免超出当前任务范围

**验证**：
- `mvn -version` 返回 Maven `3.9.16`，`Maven home` 为 `F:\maven\apache-maven-3.9.16`
- `mvn "help:evaluate" "-Dexpression=settings.localRepository" -q -DforceStdout` 返回 `F:\software\maven_repo`
- `mvn clean verify -DskipTests` 构建成功
- `npm install` 成功
- `npm run build` 成功

**遗留/Next**：
- 在 backend 中补充分层包结构、统一异常处理和配置占位
- 在 frontend 中补充路由与应用布局壳
- 再进入模块 A（基础平台）的最小闭环实现

## 2026-09-05｜创建 PKB 项目数据库

**背景**：前后端骨架已搭好，但 CentOS 上的 PostgreSQL 还没有项目专用数据库；在正式开始建表和接入应用前，需要先把基础数据库建立起来。

**产出**：
- 在 `192.168.106.130:5432` 的 PostgreSQL 16 实例上创建数据库 `synapse_pkb`

**过程要点（踩坑与决策）**：
- 数据库命名采用项目名对应的 snake_case：`synapse_pkb`
- 本次只创建数据库，不提前创建任何业务表
- `pgvector` 扩展虽然已在实例层安装可用，但仍需在后续建表阶段进入 `synapse_pkb` 单独执行 `CREATE EXTENSION vector`

**验证**：
- 执行 `CREATE DATABASE synapse_pkb WITH OWNER = postgres ENCODING = 'UTF8' TEMPLATE template0;` 成功
- 连接 `synapse_pkb` 执行 `SELECT current_database(), current_user;` 返回数据库 `synapse_pkb`、用户 `postgres`

**遗留/Next**：
- 在 `synapse_pkb` 中初始化业务表、索引和 `vector` 扩展
- 为 backend 配置开发环境数据库连接参数

## 2026-09-05｜补充提交收尾规则

**背景**：当前项目已经要求每次改码后提供 commit message 草案，但在实际协作中，仍需要同时明确本次提交建议纳入的文件范围，避免用户手动 `git add` 时混入无关文件。

**产出**：
- 更新 `doc-pilot` skill：后续每次改码收工时，除 commit message 草案外，还要同步列出本次建议执行 `git add` 的文件清单
- 同步更新 `CLAUDE.md`，保持项目核心规则与 skill 口径一致

**过程要点（踩坑与决策）**：
- 文件清单要求仅覆盖本次任务相关文件，不默认把工作区全部改动一股脑纳入提交
- 若工作区存在不应跟随本次提交的文件，后续汇报中需明确指出哪些文件暂不建议 `add`

**验证**：
- `doc-pilot` 的「收工写回闭环」规则已补充 `git add` 文件清单要求
- `CLAUDE.md` 的收工动作与协作约束已同步补充相同要求

**遗留/Next**：
- 后续所有开发任务收工时，统一按“commit message 草案 + 建议 add 文件清单”输出

## 2026-09-05｜接入 Jira Cloud 与开发前置建票 skill

**背景**：项目后续需要把开发任务和 bug 修复与 Jira ticket 关联起来，形成“先建票、后开发、commit 带编号”的闭环，方便个人项目长期追踪。

**产出**：
- 在 Jira Cloud 站点创建软件看板项目 `synapse-pkb`
- 项目 Key 确认为 `SPKB`
- 新增本地凭据文件 `.env.local`（仅本地使用，已被 `.gitignore` 排除）
- 新增 skill `.trae/skills/dev-with-ticket/`
- 新增脚本 `create-jira-issue.ps1`，可通过 Jira REST API 创建 Task / Bug
- 更新 `docs/.doc-pilot.json`，启用 `ticket_system.enabled = true` 并接入 `dev-with-ticket`
- 更新 `CLAUDE.md`，同步 Jira 与 ticket 前置门约定

**过程要点（踩坑与决策）**：
- token 只写入本地 `.env.local`，不进入任何会被 Git 提交的文件
- Jira 项目采用软件看板（Kanban）模板，避免引入 Scrum / Sprint 流程负担
- 站点 issue type 名称是中文本地化值，因此建票脚本增加了对 `任务` / `缺陷` 的兼容解析与兜底逻辑
- `dev-with-ticket` 不替代 `doc-pilot`；两者的顺序是“先按 doc-pilot 读文档，再建票，再经用户确认后开发”

**验证**：
- Jira API 认证成功
- 成功创建 Jira 项目 `synapse-pkb`，Key = `SPKB`
- 成功创建验证票 `SPKB-1`
- `.env.local` 已被 `.gitignore` 排除

**遗留/Next**：
- 后续所有开发 / 修复任务，先走 `dev-with-ticket` 建票，再进入实际开发
- commit message 草案统一带 `[SPKB-N]` 前缀

## 2026-09-05｜接入 plan-before-code 并修复 Jira 中文建票编码

**背景**：新引入了 `plan-before-code` 作为中大型任务的方案确认环节，需要把它正确接入现有 `doc-pilot` / `dev-with-ticket` 体系；同时，Jira 验证票在看板中出现中文标题显示为 `??` 的问题，需要尽快修复，否则后续建票可读性会持续受影响。

**产出**：
- 新 skill `plan-before-code` 已纳入项目规则
- `CLAUDE.md` 已明确三者是“`doc-pilot` 全程值守 + 其他 skill 在其值守范围内工作”的嵌套关系
- `dev-with-ticket` skill 已补充与 `plan-before-code` 的协作边界
- `create-jira-issue.ps1` 已增加 UTF-8 文件输入能力：`-SummaryFile` / `-DescriptionFile`
- 现有 Jira 验证票 `SPKB-1` 的中文标题已修正
- 在 `BUGFIX_LOG.md` 中登记 Jira 中文乱码问题及修复方法

**过程要点（踩坑与决策）**：
- `plan-before-code` 没有被定义成脱离 `doc-pilot` 的独立流水线，而是被定义成 `doc-pilot` 值守下的中间环节
- Jira 的 `??` 不是页面渲染问题，而是建票时文本在进入脚本前就被命令行参数编码链路损坏
- 单纯在 PowerShell 里构造正确的 Unicode 字符串还不够，发请求时也需要显式用 UTF-8 字节体，才能真正写回正确标题

**验证**：
- 通过 Jira API 再次读取 `SPKB-1`，summary 已返回正确中文
- `dev-with-ticket` 脚本已具备不依赖命令行中文参数的安全输入方式

**遗留/Next**：
- 后续凡是 Jira 标题或描述包含中文，优先使用 UTF-8 文件输入模式
- 中大型任务先由 `plan-before-code` 输出方案，再交由 `dev-with-ticket` 建票

## 2026-09-05｜编制新会话提示词指南

**背景**：项目已经接入 `doc-pilot`、`plan-before-code`、`dev-with-ticket` 等核心 skill，但如果开发人员在新会话中提示词写法不规范，skill 触发效果会不稳定，因此需要在正式开发前补一份统一的提示词指南。

**产出**：
- 新增 `docs/design/new-session-prompting-guide.md`
- 文档覆盖开发、修 bug、整理文档三类核心场景
- 为每类场景明确：
  - 推荐 skill 组合
  - 标准提示词撰写规范
  - 必备要素
  - 示例模板
  - 注意事项
- 在文档中补充审核组织建议、审核清单和审核输出要求
- 更新 `docs/README.md` 索引与 `STATUS.md`

**过程要点（踩坑与决策）**：
- 文档重点不是解释 skill 内部实现，而是帮助开发人员在“全新会话窗口”里稳定触发 skill
- 特别强调了 `doc-pilot` 是全程值守者，而不是“只在开工前读一次文档”
- 对开发和 bug 场景分别区分了简单任务与中大型/复杂任务，避免把 `plan-before-code` 过度流程化
- “组织审核”这项需求通过文档内置审核建议与清单落地，便于项目团队按统一标准评审

**验证**：
- 新文档已登记进 `docs/README.md`
- 文档内容已覆盖三类核心场景与审核清单

**遗留/Next**：
- 组织相关开发人员完成首轮审阅
- 根据审阅结果补充或压缩示例模板，保证日常使用成本可接受

## 2026-09-05｜调整提示词指南定位为“自然表达优先”

**背景**：用户反馈当前《新会话提示词指南》虽然完整，但仍然过于强调开发者显式选择 skill 和流程，容易把本应由 agent 承担的判断成本转移给开发者，这与项目希望“让 skill 帮助开发者更轻松开发”的初衷不一致。

**产出**：
- 重写 `docs/design/new-session-prompting-guide.md`
- 将文档主旨从“如何点名 skill”调整为“如何自然表达任务，让 agent 自主判断模式”
- 在 `CLAUDE.md` 中补充“提示词体验原则”
- 明确把“开发需求文档里的 A1 部分”这类自然表达设为项目推荐写法

**过程要点（踩坑与决策）**：
- 文档不再把显式点名 `doc-pilot` / `plan-before-code` / `dev-with-ticket` 作为默认推荐路径
- skill 仍然保留，但定位为 agent 内部的流程能力，而不是要求开发者手动背诵的操作指令
- 只在“先出方案”“直接改”“不要动某部分”等少数边界场景下，建议开发者补一句控制条件

**验证**：
- 新版文档已改为自然表达优先的结构
- `CLAUDE.md` 已同步该原则

**遗留/Next**：
- 后续继续观察实际使用中，agent 对自然表达任务的模式判断是否稳定

## 2026-09-05｜实现 A1 单用户认证与个人空间最小闭环

**背景**：用户要求按需求文档开始开发 A1 模块。经 `doc-pilot` 开工读取与 `plan-before-code` 方案确认后，决定本次只交付 A1 的最小闭环：单用户 JWT 本地登录、个人空间基础展示，以及可承接后续 `user_id` 过滤的数据库基线，不把 A2-A5 范围提前混入。

**产出**：
- 创建 Jira ticket `SPKB-2`
- 后端新增 Spring Security + JWT 鉴权链路
- 新增 `/api/auth/login` 与 `/api/auth/me` 接口
- 新增 `pkb_users` 表的 Flyway 迁移脚本、JPA 实体、仓库与启动期单用户初始化逻辑
- 前端新增登录页、token 持久化、受保护应用壳与个人空间展示
- 补充后端鉴权集成测试与前端构建验证
- 为 `plan-before-code` 新增本地方案目录约束，并把本次方案落到 `docs/plans-local/2026-09-05-a1-auth-foundation/`

**过程要点（踩坑与决策）**：
- A1 的“数据库预留多用户迁移路径”本次收敛为先建立 `pkb_users` 基线，不提前创建 `entries` 等业务表，避免需求越界
- 登录账号改为本地配置驱动，启动时同步到数据库，保证当前单用户模式可用，同时保留后续扩展空间
- Maven 测试命令需显式指向仓库根 `settings.xml`，否则会误走 `backend/settings.xml` 的相对路径而失败
- Spring Security 默认会把未认证访问返回为 `403` 且启用默认内存用户，需要显式关闭默认用户配置并改为 `401` 入口响应

**验证**：
- `mvn -s "f:\synapse-pkb\settings.xml" test`
- `npm run build`
- 在 CentOS 服务器本地启动 A1 后端，连接 `synapse_pkb` 执行 Flyway 迁移
- 在 CentOS 服务器本地导入种子 SQL：`backend/src/main/resources/db/seed/A1__seed_pkb_users.sql`
- 远程校验 `flyway_schema_history` 与 `pkb_users` 数据已落库
- 调用 `http://192.168.106.130:18081/api/auth/login` 验证种子用户 `pkb-admin / Password123!` 可成功登录

**遗留/Next**：
- 后续在 A2/A5 的业务表中补齐 `user_id` 字段、索引和按当前用户过滤查询
- 为运行时环境补齐 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`APP_JWT_SECRET`、`APP_BOOTSTRAP_*` 配置

## 2026-09-06｜补齐 IDEA 直启后端的本地配置与强制自检约束

**背景**：用户明确要求后续所有改码任务都必须完成本地编译和前后端服务启动自检；同时，希望在 Windows 的 IDEA 中直接启动后端时，就能连接 CentOS 上的 PostgreSQL，而不是每次手动补环境变量。

**产出**：
- 在 `CLAUDE.md` 中新增“改码后的本地强制自检”约束
- 后端配置改为可自动导入仓库根或 `backend/` 下的 `.env.local`
- 新增 `.env.example`，列出 Jira、数据库、JWT 与 bootstrap 用户所需键名模板
- 本地 `.env.local` 已补齐后端启动所需配置（仅本地生效，不入库）

**过程要点（踩坑与决策）**：
- 当前后端原本只依赖环境变量占位符，IDEA 若未单独配置 Run Configuration 环境变量则无法直接启动
- 采用 `spring.config.import` 读取 `.env.local`，既保留敏感信息本地化，也避免把凭据写进可提交配置文件
- `.gitignore` 已排除 `.env.local`，因此真实数据库凭据不会进入远程仓库；而 `.env.example` 只保留占位模板，方便本地配置对齐

**验证**：
- `mvn -pl backend test`
- `npm run build`
- `mvn -pl backend spring-boot:run` 可在本地直接启动，并成功连接 `192.168.106.130:5432/synapse_pkb`
- `GET http://localhost:8080/actuator/health` 返回 `UP`
- `POST http://localhost:8080/api/auth/login` 使用 `pkb-admin` 成功返回 token
- `npm run dev -- --host 127.0.0.1`
- `GET http://127.0.0.1:5173` 返回 `200`
- `POST http://127.0.0.1:5173/api/auth/login` 通过 Vite 代理成功联调后端

**遗留/Next**：
- 若后续新增更多本地运行配置项，优先同步补充到 `.env.example`
- 后续可再补一份 IDEA Run Configuration 使用说明，方便本地新环境快速恢复

## 2026-09-06｜修复 dev-with-ticket 的 Jira 中文乱码

**背景**：用户反馈通过 `dev-with-ticket` skill 创建的 Jira 工单中，中文标题和中文描述都变成了问号；此前虽然已经改成通过 UTF-8 文件传参，但 `SPKB-2` 仍然损坏，说明编码问题还存在于 HTTP 请求发送阶段。

**产出**：
- 修复 `.trae/skills/dev-with-ticket/scripts/create-jira-issue.ps1` 的 UTF-8 请求体发送逻辑
- 增加 PowerShell 输出编码设置 `$OutputEncoding = [System.Text.Encoding]::UTF8`
- 请求头显式声明 `application/json; charset=utf-8`
- 文件输入的 `Summary` / `Description` 在发送前执行 `Trim()`
- 用修复后的脚本创建中文验证票 `SPKB-3`
- 重建 A1 工单为 `SPKB-4`
- 将损坏的 `SPKB-2` 标记作废并流转到完成态

**过程要点（踩坑与决策）**：
- 问题不在 Jira 展示层，也不在 UTF-8 文件读取本身，而在 `Invoke-RestMethod` 的请求体仍直接发送字符串
- 脚本十六进制检查显示文件头为 ASCII 文本内容，无 BOM；由于脚本内无中文字面量，因此文件保存编码不是本次主因
- 删除 `SPKB-2` 时 Jira API 返回 `403`，说明当前权限下不能删除 issue，因此改为“作废说明 + 关闭流转”的处理方式

**验证**：
- 使用中文 UTF-8 文件输入创建 `SPKB-3`
- Jira API 回读 `SPKB-3`，中文标题和中文描述均完整
- 使用 A1 本地方案文件重建 `SPKB-4`
- Jira API 回读 `SPKB-4`，中文标题和描述均完整
- 浏览器自动化尝试访问 Jira 页面时被 Atlassian 登录页拦截，无法在缺少网页登录凭据的前提下进入 issue 详情页

**遗留/Next**：
- 若后续需要做网页端自动核验，需要补充 Atlassian 网页登录态或改为人工登录后复核

## 2026-09-06｜公共能力加固：统一日志、异常处理与 API 契约

**背景**：A1 已完成单用户认证闭环，但如果继续直接开发 A2/A3 等业务功能，日志、异常处理、接口格式和前端请求层不统一的问题会快速放大返工成本。因此本次先插入一轮最小但高收益的公共能力加固，为后续业务模块打底。

**产出**：
- 创建 Jira ticket `SPKB-5`
- 后端新增统一 `ApiResponse<T>` 成功/失败结构
- 后端新增全局异常处理，统一处理参数校验、认证失败、常见状态异常与兜底系统异常
- 后端新增请求链路日志与 `requestId` 贯穿，响应头补充 `X-Request-Id`
- 前端重构 `api.ts`，统一封装响应解包、错误标准化、401 失效处理、开发态请求/响应日志
- 前端登录链路适配新的统一响应格式
- 更新架构快照与项目进度文档

**过程要点（踩坑与决策）**：
- 本次刻意只做“最小统一结构”，没有把错误码字典、审计表、外部日志平台一起拉进来，避免横切任务无限膨胀
- 后端请求日志放在过滤器层实现，这样无论是正常业务响应还是 Spring Security 的未认证响应，都能带上 `requestId` 并统一输出耗时
- 认证失败响应不再直接走默认 `sendError`，改为统一 JSON 错误体，避免前端后续在不同接口上遇到不同形态的错误
- 前端用 `ApiClientError` 统一标准化错误对象，后续页面不再各自猜测后端错误结构
- 本地自检时默认 `8080` 一度被占用，因此先用备用端口验证启动能力，随后在用户释放端口后重新切回默认 `8080` 完成正式联调

**验证**：
- `mvn -pl backend test`
- `npm run build`
- `mvn -pl backend spring-boot:run`
- `GET http://localhost:8080/actuator/health` 返回 `UP`
- `POST http://localhost:8080/api/auth/login` 返回统一包裹格式，并包含 `requestId`
- `npm run dev -- --host 127.0.0.1`
- `GET http://127.0.0.1:5173` 返回 `200`
- `POST http://127.0.0.1:5173/api/auth/login` 通过 Vite 代理联调成功

**遗留/Next**：
- 后续新增业务接口时继续统一使用 `ApiResponse<T>`，不要再引入裸对象返回
- 下一步可在该公共层基础上继续推进 A2 分类标签或 A3 统一录入

## 2026-09-06｜状态码体系第一轮完善：统一错误码定义与业务异常骨架

**背景**：上一轮公共能力加固已经统一了 `ApiResponse<T>` 和全局异常处理，但 `error.code` 仍停留在少量通用字符串常量阶段。继续推进后续业务模块前，需要先把错误码从“格式统一”推进到“语义统一”，避免后续接口继续散落返回粗粒度错误码。

**产出**：
- 创建 Jira ticket `SPKB-6`
- 后端新增统一错误码定义接口 `ApiErrorCode` 与公共错误码枚举 `CommonErrorCode`
- 后端新增 `BusinessException` 业务异常骨架，支持绑定 HTTP 状态码、错误码和默认消息
- 改造 `GlobalExceptionHandler`，统一从错误码定义生成响应错误体
- 改造 `SecurityErrorResponseWriter` 与 `JwtAuthenticationFilter`，细化认证错误码
- 认证链路从通用 `UNAUTHORIZED` 收敛为 `AUTHENTICATION_REQUIRED`、`AUTH_INVALID_TOKEN`、`AUTH_INVALID_CREDENTIALS`
- 补充后端测试，覆盖无 token、无效 token、错误密码等场景
- 更新架构快照与项目进度文档

**过程要点（踩坑与决策）**：
- 本次仍然坚持“骨架先行”，没有一次性穷举未来所有业务错误码，只先沉淀公共错误码和认证样板
- 前端本轮保持兼容，不引入新的前端错误码映射层，继续直接消费后端返回的稳定字符串码值
- 无效 JWT 以前会退化为通用未认证响应，本次改为在过滤器层直接输出 `AUTH_INVALID_TOKEN`，方便后续前端和日志更准确定位问题
- `ResponseStatusException` 仍保留兜底接入能力，但后续业务代码应优先抛 `BusinessException`，避免重新回到散落字符串时代

**验证**：
- `mvn -pl backend test`
- `npm run build`
- `mvn -pl backend spring-boot:run`
- `GET http://localhost:8080/actuator/health` 返回 `UP`
- `POST http://localhost:8080/api/auth/login` 使用错误密码返回 `AUTH_INVALID_CREDENTIALS`
- `GET http://localhost:8080/api/auth/me` 携带无效 token 返回 `AUTH_INVALID_TOKEN`
- `npm run dev -- --host 127.0.0.1`
- `POST http://127.0.0.1:5173/api/auth/login` 通过 Vite 代理联调成功

**遗留/Next**：
- 后续在 A2-A5 业务模块中继续补齐分模块业务错误码，避免滥用 `BAD_REQUEST` / `REQUEST_FAILED`
- 视后续业务复杂度，再决定是否补充错误码对外文档或前端映射表

## 2026-09-06｜架构纠偏：切换 MyBatis-Plus 并建立后端分层基线

**背景**：用户在项目早期及时指出两个架构问题：后端 ORM 应切换为更适合复杂 SQL 演进的 MyBatis-Plus；同时，现有代码虽然按功能有初步分包，但还没有清晰固定 `entity / dto / vo / mapper / service / controller` 的分层约束。若继续在当前基础上推进 A2-A5，后续返工成本会快速放大，因此本次先完成架构纠偏。

**产出**：
- 创建 Jira ticket `SPKB-7`
- 后端依赖从 `spring-boot-starter-data-jpa` 切换为 `mybatis-plus-spring-boot3-starter`
- 移除 A1 现有链路中的 JPA Entity / Repository，实现 `PkbUserEntity + PkbUserMapper + PkbUserService`
- 建立 `auth/controller`、`auth/service/impl`、`auth/model/dto`、`auth/model/vo`、`user/entity`、`user/mapper`、`user/service/impl` 等目录结构
- 新增 MyBatis-Plus 自动填充处理器，接管 `created_at` / `updated_at`
- 预留 `resources/mapper/**` XML 目录，并让 `PkbUserMapper.xml` 承载查询 SQL
- 认证链路在保持现有 API 契约、状态码体系和 JWT 行为不变的前提下完成持久层迁移
- 更新架构快照与项目进度文档

**过程要点（踩坑与决策）**：
- 本次按“按业务域分包，域内显式分层”落地，而不是改成全局 `controller/service/mapper` 大目录，避免后续模块继续混在一起
- 虽然当前 SQL 还很简单，但已经预留 XML Mapper 目录约定，让后续复杂 SQL 不需要二次迁移目录结构
- A1 当前仅有 `pkb_users` 一处 JPA 落地，因此选择现在完成 ORM 切换，收益远大于成本
- 为减少行为回归，本次没有顺手改认证协议、接口返回格式或测试口径，只替换持久层与包结构

**验证**：
- `mvn -pl backend test`
- `npm run build`
- `mvn -pl backend spring-boot:run`
- `GET http://localhost:8080/actuator/health` 返回 `UP`
- `POST http://127.0.0.1:5173/api/auth/login` 通过 Vite 代理联调成功
- `GET http://localhost:8080/api/auth/me` 携带有效 token 返回当前用户
- `GET http://localhost:8080/api/auth/me` 携带无效 token 返回 `AUTH_INVALID_TOKEN`

**遗留/Next**：
- 后续 A2-A5 继续沿用这套分层目录模板，避免重新出现混放结构
- 若后续业务 SQL 明显变复杂，可继续在现有 XML Mapper 目录下扩展，而无需再做 ORM 路线切换

## 2026-09-06｜审计日志薄骨架：注解、AOP 与 operation_logs 基线

**背景**：在统一 API 契约、错误码体系、请求链路日志和 MyBatis-Plus 分层基线都已落地后，继续开发 A2-A5 时如果还没有审计能力，后续再回头补“谁做了什么”的横切能力会越来越贵。本次先按“薄骨架”思路补齐审计日志基础设施：不做完整平台，但把未来即插即用需要的接口形态和扩展位一次定对。

**产出**：
- 创建 Jira ticket `SPKB-8`
- 新增 `operation_logs` 表的 Flyway 脚本 `V2__create_operation_logs.sql`
- 新增审计种子脚本 `G2__seed_operation_logs.sql`
- 新增 `@AuditOperation` 注解、审计切面、审计持久化服务与 `operationlog` 业务域
- 通过 `OperationLogService + OperationLogMapper` 统一落库审计事件
- 第一版审计模型包含 `actor_user_id`、`actor_username`、`action`、`target_type`、`target_id`、`request_id`、`success`、`message`、`details_json`、`occurred_at`
- 以认证登录成功作为第一条样板审计事件，动作码为 `AUTH_LOGIN`
- 增加后端集成测试，验证登录成功后确实新增审计记录
- 更新架构快照与项目进度文档

**过程要点（踩坑与决策）**：
- 这次没有把审计做成“切面里写死一堆 if/else”，而是额外引入了 `AuditOperationCustomizer` 扩展位：切面负责统一抓公共上下文，具体业务对象解析交给 customizer，方便未来为条目编辑、标签合并、关联确认等场景定制目标对象和 details
- 第一版明确只记录成功业务事件，不把登录失败、安全失败、异常堆栈一股脑塞进同一张表，避免“操作审计”和“安全事件”在模型层混杂
- 切面内部对审计落库异常做了吞吐保护：即使审计失败，也不会反向打断主业务链路，这是后续从同步落库演进到异步事件化的前提
- `details_json` 第一版即预留为可扩展字段，但当前仍保持轻量，只写必要的业务上下文，不提前设计复杂差异模型
- `target_id` 目前使用字符串而非纯数值，优先兼容未来可能出现的复合标识或非数值目标对象

**验证**：
- `mvn -pl backend test`
- `npm run build`
- `mvn -pl backend spring-boot:run`
- `GET http://localhost:8080/actuator/health` 返回 `UP`
- `POST http://localhost:8080/api/auth/login` 返回成功，并写入 `AUTH_LOGIN` 审计事件
- `POST http://127.0.0.1:5173/api/auth/login` 通过 Vite 代理联调成功
- 使用本机 `psql` 只读查询 PostgreSQL，确认 `operation_logs` 中存在刚写入的 `AUTH_LOGIN|pkb-admin|USER|1|true|<requestId>` 记录

**补充验证**：
- 用户随后已代为完成 CentOS 服务器本地 `G2__seed_operation_logs.sql` 导入验证，因此本次审计日志任务现已达到完整收尾状态

**遗留/Next**：
- 为 A2-A5 的新增/编辑/删除/关联确认方法逐步挂接 `@AuditOperation`
- 视后续需求决定是否把失败安全事件单独建模，避免和业务审计混表
