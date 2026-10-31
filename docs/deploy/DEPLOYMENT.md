# 个人知识库系统（PKB） · 部署与运维手册

## 环境

- 目标服务器：`192.168.106.130`
- 操作系统：CentOS Stream 9
- 当前远程用户：`chau`（SSH Key 免密登录）
- 当前权限状态：`chau` 属于 `wheel` 组，但未确认免密 sudo；所有需要 root 的安装或系统级变更先经用户确认
- 当前权限状态：`chau` 已具备可用 sudo 权限，且已加入 `docker` 组
- 当前 PostgreSQL 超级用户密码：已完成统一配置（详见本地敏感文档 `server-inventory.md`，不入库）
- 当前 PostgreSQL 远程访问：已开放 `192.168.106.130:5432`，并已配置白名单访问控制
- 当前 PKB 项目数据库：`synapse_pkb`，已迁移 V1 用户表、V2 审计表、V3 标签表（2026-11-01 已核验）；没有 entries/entry_tags
- 当前资源：
  - 根分区：约 `17G`，可用约 `8.2G`
  - 内存：约 `3.5Gi`
  - Swap：约 `2.0Gi`

## 当前已确认服务与依赖

- 已安装并可复用：
  - Java 17
  - Maven 3.6.3
  - Node.js 18.20.8
  - Python 3.9 + `pip`
  - Podman 5.6.0
  - Docker Engine 29.8.0
  - Docker Compose Plugin 5.5.1
  - Redis 6.2.19（`redis.service`，active）
  - Nginx 1.20.1（已安装，当前未确认启用）
  - MySQL 8（`mysqld.service`，active）
  - Jenkins（`jenkins.service`，active，端口 `10688`）
  - Nacos 2.3.2（Java 进程运行中，端口 `8848/9848/9849/7848`）
  - PostgreSQL 16（`postgresql-16.service`，active）
  - pgvector 0.8.6（扩展已可用）
- 当前缺失或待补齐：
  - `pnpm`（若前端后续需要）

## 端口现状（2026-09-05 盘点）

- `22`：SSH
- `3306/33060`：MySQL
- `6379`：Redis（仅本机回环）
- `5432`：PostgreSQL（已启用白名单访问控制）
- `8848/9848/9849/7848`：Nacos
- `10688`：Jenkins

详细清单与登录信息见 [server-inventory.md](server-inventory.md)。

## 部署步骤

1. 先核对 [server-inventory.md](server-inventory.md)，确认当前机器资源、已安装服务与端口占用。
2. 在得到用户确认后，安装缺失的系统依赖：
   - 已完成 Docker Engine / Docker Compose Plugin
   - 已完成 PostgreSQL 16 / pgvector
   - 已完成 `python3-pip`
3. 规划 PKB 的部署目录、数据目录、环境变量文件与反向代理入口，避免与现有 Jenkins / Nacos / MySQL 冲突。
4. 使用 Docker Compose 编排应用、PostgreSQL、Redis（若复用系统 Redis 需明确约束）与 Nginx 路由。
5. 部署完成后补充：
   - 容器与服务启动方式
   - 日志位置
   - 数据备份与恢复步骤
   - 回滚流程

## 回滚步骤

- 当前尚未进入正式部署阶段，待首版部署方案落地后补充。

## A2 临时验证与数据库升级（SPKB-9）

- 已在 CentOS 本地创建隔离库 `synapse_pkb_a2_test`，执行全迁移、G3 种子及 PG16 集成测试；测试数据不进入业务库。
- 业务库升级前备份为 `/home/chau/spkb9-before-v3-20261101.dump`，权限 600，`pg_restore -l` 检查通过；随后由服务器后端 Flyway 从 V2 升级 V3。完整 SQL、实际命令及验收证据见 [A2 验证记录](../qa/a2-verification.md)。
- 验证期间后端只监听服务器 loopback（测试库 18080、业务库 18081），通过 SSH 本机转发供 Vite 联调，不开放新防火墙端口。完成首轮验证后停止临时服务，随后按用户要求轮换实际凭据，并保留业务库后端 18081 + SSH 转发 + 本机 Vite 5173 供用户登录查看；测试后端已停止，这不是正式部署。
- Windows IDEA 自检必须关闭 Flyway 和 bootstrap（`APP_BOOTSTRAP_ENABLED=false`），数据库 URL 附加 URL 编码的只读会话选项 `?options=-c%20default_transaction_read_only%3Don`，只做健康与查询；写接口须指向 CentOS 后端。正式部署仍需专用最小权限账号，当前会话只读设置不是角色权限隔离。
- Vite `API_PROXY_TARGET` 可在 `frontend/.env.local` 或进程环境配置，默认 `http://localhost:8080`；不使用 `VITE_*` 前缀，避免将服务器配置打入客户端包。
- 不自动清理测试库或备份；若需恢复业务库，先停止写入，审阅 V3 后新增标签数据并单独确认恢复范围，不能直接覆盖。
- 实际登录口令/JWT 密钥已按用户授权轮换，旧密码及旧签名 token 均验证为 401。新值只在本机 `.env.local`；登录账号/密码分别读取 `APP_BOOTSTRAP_USERNAME`、`APP_BOOTSTRAP_PASSWORD`。旧备份包含轮换前哈希，恢复后必须重新执行凭据轮换，不能重新启用已暴露默认值。
