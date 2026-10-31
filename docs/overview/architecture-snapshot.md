# 个人知识库系统（PKB） · 系统全貌（Architecture Snapshot）

> 描述系统「现在是什么样」。架构或横切机制变更时同步修订对应小节，不追加流水账。

## 技术栈

- 后端：Java 17 + Spring Boot 3 + MyBatis-Plus
- 前端：React 18 + TypeScript + Ant Design 5
- 数据层：PostgreSQL 16 + Flyway（用户、审计、标签 V1–V3）；Redis 与 pgvector 后续接入，实际数据库升级证据见 A2 验证记录
- AI 能力：GLM Embedding / Chat 双接口（后续接入）
- 部署：Docker + Docker Compose + Nginx 反向代理
- 当前服务器基线：CentOS Stream 9（`192.168.106.130`）

## 模块划分

- 模块 A：基础平台（认证、分类标签、统一录入、附件原文、CRUD）
- 模块 B：内容处理与分类感知摘要（分类路由抽取、统一摘要、Embedding、去重）
- 模块 C：检索（语义检索、关键词检索、混合排序、高级筛选）
- 模块 D：主动关联引擎（候选召回、LLM 判定、待确认队列、重要性衰减、异步调度）
- 模块 E：主动呈现（每周回顾、知识图谱、首页信息流）
- 模块 F：语音备忘录（二期，不纳入一期开发）

## 横切机制

- 鉴权 / 权限：A1 已落地单用户模式的 JWT 本地登录，后端提供 `/api/auth/login` 与 `/api/auth/me`；前端已具备登录页、token 持久化和受保护应用壳。数据库当前已建立 `pkb_users` 作为单用户基线，后续业务表按该用户上下文补齐 `user_id` 字段与索引。
- API 契约：后端业务接口统一返回 `ApiResponse<T>` 包裹结构，包含 `success`、`data`、`error`、`requestId`、`timestamp`；其中 `error.code` 已升级为统一错误码体系骨架，按公共错误码枚举输出稳定字符串值，前端继续通过 `api.ts` 解包响应、处理 401 和标准错误消息，后续新增接口默认复用该契约。
- 异常处理：后端已补齐全局异常处理，并新增 `BusinessException` 作为业务异常骨架，统一覆盖参数校验失败、认证失败、常见 `ResponseStatusException` 与兜底系统异常；认证层返回也统一为 JSON 错误体，当前已区分未登录、无效 token、无权限、错误凭据等基础错误码，不再出现默认白页或散乱格式。
- 日志 / 审计：通过 `@AuditOperation + AOP` 在 Service 层记录 AUTH_LOGIN、TAG_CREATE、TAG_RENAME、TAG_MERGE。事务 advisor(order=100) 包裹审计 aspect(order=200)，成功结果生成最小快照，真实事务只在 afterCommit 经独立代理 REQUIRES_NEW 写入；无事务的登录仍直接尝试独立写入。writer 超时 3 秒，失败在回调内部隔离，不回滚业务；无变化的重命名跳过审计。该机制是提交后尽力写入，不保证宕机零丢失，不支持 NESTED 部分回滚语义。前端不再输出完整请求/响应/错误详情，后端异常日志不输出 SQL 值或完整异常堆栈。
- A2 非 AI 实现（SPKB-9）：分类由枚举提供固定五值查询，无分类表或管理平台；tags 按认证用户隔离、跨分类共享，使用 NFC/空白折叠/ASCII 判重与数据库 CHECK、生成键、联合唯一约束。重命名/合并使用有序行锁与事务；单源合并保留目标并硬删除源，重复请求 404。前端提供创建/重命名/合并弹窗、搜索、刷新及错误状态。当前没有 entries/entry_tags、使用统计或 AI 推荐；未来关联唯一来源的模型决策见 [分类与标签设计](../design/a2-category-tag-design.md)，验证结果见 [A2 验证记录](../qa/a2-verification.md)。
- 测试与启动：本机 Java 17/H2 冒烟只迁移至 V2；`postgres-it` profile 在 CentOS 的隔离 PG16 测试库执行全迁移、约束、隔离、并发与真实事务/审计故障测试。Windows 后端关闭 Flyway/bootstrap，使用数据库会话只读参数做启动/读查询；Vite 的 `API_PROXY_TARGET` 可指向 SSH 转发的服务器后端，写操作均由 CentOS 进程执行。
- 分层约定：后端当前采用“按业务域分包，域内显式分层”的结构；以 `auth`、`user` 等业务域为边界，在域内明确拆分 `controller`、`service`、`mapper`、`entity`、`model/dto`、`model/vo` 等目录，避免后续模块继续混放控制层、业务层和持久层代码。
- 任务调度：关联计算与批量任务走 `Spring @Scheduled + Redis`，避免阻塞录入链路。
- 部署约定：目标环境为单机 CentOS Stream 9；当前已确认可复用 Java 17、Maven、Node 18、Redis、Nginx、MySQL、Jenkins、Nacos，且已补齐 Docker Engine / Docker Compose / PostgreSQL 16 / pgvector / python3-pip。
- 范围约定：一期严格聚焦模块 A-E；语音备忘录属于二期，不提前落地。
