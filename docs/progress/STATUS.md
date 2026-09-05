# 个人知识库系统（PKB） · 项目进度速览（STATUS）

> AI 开发值守必读入口。上接 [../README.md](../README.md)，细节见 [dev-log.md](dev-log.md)。
> last-updated 隐藏标记供 check-docs.py 校验新鲜度，更新正文日期时必须同步改。

<!-- last-updated: 2026-09-05 -->

## 模块进度总览

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| 文档与部署基线 | doc-pilot 知识库初始化、需求快照入库、CentOS 服务器现状盘点与基础依赖安装完成 | ✅ |
| 模块 A-E（一期） | 已完成前后端基础骨架与 Maven 构建基线，尚未进入业务实现 | 🟡 |
| 模块 F（二期） | 语音备忘录，按需求保持冻结 | ⬜ |

## 最近动态（新→旧，最多 8 条）

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
- [ ] 为 `synapse_pkb` 数据库初始化业务表结构与 `vector` 扩展
- [ ] 在后续开发任务中启用 Jira 建票前置门，统一使用 `SPKB-*` ticket 编号
- [ ] 为 backend 补充分层包结构、统一异常与配置基线
- [ ] 为 frontend 补充路由、布局壳与 API 调用规范
- [ ] 开始 Phase 1 的基础平台最小闭环实现

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
