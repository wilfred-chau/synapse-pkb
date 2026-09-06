# 个人知识库系统（PKB） · 项目进度速览（STATUS）

> AI 开发值守必读入口。上接 [../README.md](../README.md)，细节见 [dev-log.md](dev-log.md)。
> last-updated 隐藏标记供 check-docs.py 校验新鲜度，更新正文日期时必须同步改。

<!-- last-updated: 2026-09-06 -->

## 模块进度总览

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| 文档与部署基线 | doc-pilot 知识库初始化、需求快照入库、CentOS 服务器现状盘点与基础依赖安装完成 | ✅ |
| 模块 A-E（一期） | 已完成前后端基础骨架；A1 单用户认证与个人空间最小闭环已落地，其余业务模块待继续实现 | 🟡 |
| 模块 F（二期） | 语音备忘录，按需求保持冻结 | ⬜ |

## 最近动态（新→旧，最多 8 条）

- **2026-09-06**：已创建 Jira `SPKB-8` 并完成审计日志薄骨架：后端新增 `operation_logs` 表、`@AuditOperation`、AOP 切面与审计持久化服务，并以登录成功作为第一条样板审计事件；本地后端测试、前端构建、前后端启动联调、PostgreSQL 只读核验，以及服务器本地 `G2__seed_operation_logs.sql` 导入验证均已完成。
- **2026-09-06**：已创建 Jira `SPKB-7` 并完成一次架构纠偏：后端 ORM 从 JPA 切换为 MyBatis-Plus，A1 认证链路改为 Mapper 持久层；同时建立“按业务域分包，域内显式分层”的后端目录基线，并预留 XML Mapper 目录；本地后端测试、前端构建和前后端启动联调均通过。
- **2026-09-06**：已创建 Jira `SPKB-6` 并完成统一状态码体系第一轮加固：后端新增统一错误码定义与 `BusinessException` 骨架，认证链路细化为 `AUTHENTICATION_REQUIRED`、`AUTH_INVALID_TOKEN`、`AUTH_INVALID_CREDENTIALS` 等错误码；本地后端测试、前端构建和前后端启动联调均通过。
- **2026-09-06**：已完成一轮公共能力加固并创建 Jira `SPKB-5`：后端补齐统一 `ApiResponse` 契约、全局异常处理与 requestId 请求链路日志；前端补齐统一 API 调用层、401 处理与开发态请求/响应日志；本地后端测试、前端构建和前后端启动联调均通过。
- **2026-09-06**：已修复 `dev-with-ticket` 的 Jira 中文乱码：`create-jira-issue.ps1` 改为以 UTF-8 字节流发送 JSON 请求体并显式声明 `charset=utf-8`；已创建中文验证票 `SPKB-3` 与重建后的 A1 工单 `SPKB-4`，损坏的 `SPKB-2` 已标记作废并关闭。
- **2026-09-06**：已补齐本地启动配置：后端支持自动导入 `.env.local`，在 Windows 本地可直接启动并连接 CentOS PostgreSQL；同时新增 `.env.example` 并将“改码后必须本地编译 + 前后端启动自检”固化为项目强约束。
- **2026-09-05**：已完成 A1 模块最小闭环，并在 CentOS 服务器本地完成 `synapse_pkb` 的 Flyway 落库、种子数据导入与登录链路验证：后端新增单用户 JWT 登录、`/api/auth/me`、`pkb_users` 数据库基线；前端新增登录页、登录态持久化与个人空间壳；关联 Jira ticket `SPKB-2`。
- **2026-09-05**：已将《新会话提示词指南》调整为“自然表达优先、agent 自主判断流程”的使用口径，避免要求开发者手动编排 skill。
- **2026-09-05**：已新增《新会话提示词指南》，明确开发、修 bug、整理文档三类场景下的标准提示词写法、模板与审核清单。
- **2026-09-05**：已将 `plan-before-code` 接入项目规则，并修复 Jira 建票中文标题显示为 `??` 的编码问题。
- **2026-09-05**：已完成 Jira Cloud 项目 `synapse-pkb`（Key：`SPKB`）创建，并接入 `dev-with-ticket` 开发前置建票 skill。
- **2026-09-05**：已在 CentOS PostgreSQL 16 上创建项目数据库 `synapse_pkb`，并完成连接验证。
- **2026-09-05**：完成根聚合 Maven 工程、Spring Boot 后端骨架、React + Vite 前端骨架搭建，并验证后端 `mvn clean verify -DskipTests` 与前端 `npm run build` 通过。
- **2026-09-05**：已开放 PostgreSQL `192.168.106.130:5432` 并启用白名单访问控制，本机 TCP 连通性验证通过。
- **2026-09-05**：已完成 PostgreSQL `postgres` 超级用户密码统一设置，并完成本机 TCP 登录验证。
- **2026-09-05**：完成 CentOS Stream 9 服务器首轮盘点，建立服务器台账并修正需求中的部署基线。
- **2026-09-05**：完成 Docker、Docker Compose、pip、PostgreSQL 16、pgvector 安装，并验证服务已启动。
- **2026-09-05**：知识库初始化（doc-pilot 首次接入本项目）。

## 当前焦点 / 待办

- [ ] 组织相关开发人员审核《新会话提示词指南》，收集反馈并完成首轮修订
- [ ] 输出首版 PKB 部署目录与 Compose 方案
- [ ] 基于统一错误码骨架，为后续 A2-A5 业务接口补齐分模块业务错误码
- [ ] 基于新的 MyBatis-Plus 与域内分层基线，为 A2-A5 模块沿用统一的 entity/mapper/service/controller 结构
- [ ] 基于 `@AuditOperation` 审计骨架，为后续 A2-A5 的 CRUD、标签维护、关联确认动作补齐审计接入
- [ ] 在 A2/A5 后续任务中，为业务表补齐 `user_id` 字段、索引与查询过滤
- [ ] 为 `synapse_pkb` 数据库初始化后续业务表结构与 `vector` 扩展
- [ ] 在后续开发任务中启用 Jira 建票前置门，统一使用 `SPKB-*` ticket 编号
- [ ] 基于 A1 继续实现 A2 分类与标签体系
- [ ] 基于 A1 继续实现 A3/A5 的统一录入与条目 CRUD

## 已知问题与坑位速查

| 编号/关键词 | 一句话 | 详见 |
| --- | --- | --- |
| Docker 冲突 | CentOS 上安装 Docker 前需先移除 `podman-docker` 兼容包，否则 `docker-ce` 冲突安装失败 | ../deploy/server-inventory.md |

## 关键约定速查（高频引用，避免翻史料）

- 需求文档原件与 Markdown 快照统一维护在 `docs/requirements/`
- 一期只做模块 A-E，语音备忘录不提前落地
- 服务器当前真实环境为 `192.168.106.130` / CentOS Stream 9，不再沿用旧的 Ubuntu 假设
- PostgreSQL 管理员口令已完成统一配置；敏感明文仅保留在不入库的 `docs/deploy/server-inventory.md`
- PostgreSQL 当前已启用白名单访问控制，如更换客户端机器需同步调整访问规则

## 上下文恢复路线

1. 本文件 → 2. ../overview/architecture-snapshot.md → 3. 按任务选 design/qa → 4. 需要历史细节再进 history/ 对应阶段文件。
