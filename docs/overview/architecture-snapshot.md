# 个人知识库系统（PKB） · 系统全貌（Architecture Snapshot）

> 描述系统「现在是什么样」。架构或横切机制变更时同步修订对应小节，不追加流水账。

## 技术栈

- 后端：Java 17 + Spring Boot 3
- 前端：React 18 + TypeScript + Ant Design 5
- 数据层：PostgreSQL 16（规划安装，需启用 pgvector）+ Redis
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

- 鉴权 / 权限：单用户模式，采用 JWT 本地登录；数据库层预留 `user_id` 但一期所有查询按固定单用户处理。
- 日志 / 审计：后续通过 `operation_logs` 记录条目编辑、删除、关联确认等关键行为；部署与服务变更单独记录在 `../deploy/server-inventory.md` 与 `../deploy/DEPLOYMENT.md`。
- 任务调度：关联计算与批量任务走 `Spring @Scheduled + Redis`，避免阻塞录入链路。
- 部署约定：目标环境为单机 CentOS Stream 9；当前已确认可复用 Java 17、Maven、Node 18、Redis、Nginx、MySQL、Jenkins、Nacos，且已补齐 Docker Engine / Docker Compose / PostgreSQL 16 / pgvector / python3-pip。
- 范围约定：一期严格聚焦模块 A-E；语音备忘录属于二期，不提前落地。
