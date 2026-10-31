# A2 验证执行单（SPKB-9）

## 数据库变更审批

本节准备于 2026-11-01，用户已明确批准“测试及业务库升级”：先测试库验证通过，再备份业务库并升级。所有真实数据库写入由 CentOS 本地进程执行。

### 目标与完整脚本

- 测试库：新建 `synapse_pkb_a2_test`，仅本任务使用，不覆盖已有同名库。
- 业务库：`synapse_pkb`，只读确认当前 Flyway 为 V1/V2。
- [V1 用户表](../../backend/src/main/resources/db/migration/V1__create_pkb_users.sql)及 [V2 审计表](../../backend/src/main/resources/db/migration/V2__create_operation_logs.sql)：只在全新测试库经 Flyway 执行，业务库不重跑。
- [V3 标签完整 SQL](../../backend/src/main/resources/db/migration/V3__create_tags.sql)：测试库验证通过后，业务库经 Flyway 升级；新增 `pkb_tag_normalize_v1(TEXT)`、`tags` 表及其序列、主键、外键、CHECK、联合唯一约束，不改 V1/V2 或已有数据，不建立 entries。
- [G3 种子](../../backend/src/main/resources/db/seed/G3__seed_tags.sql)：只在测试库导入两位禁用测试用户和各五个标签，包含中文与跨用户同名；不会导入业务库或允许种子用户登录。

新库命令：

```sql
CREATE DATABASE synapse_pkb_a2_test TEMPLATE template0 ENCODING 'UTF8';
```

测试库专用提交失败探针（不是业务模块，不进入 Flyway）：

```sql
CREATE TABLE a2_commit_probe (
    user_id BIGINT REFERENCES pkb_users(id) DEFERRABLE INITIALLY DEFERRED
);
```

测试可在探针写入不存在的 user_id，强制真实提交阶段失败，验证没有成功审计。审计失败使用超长审计字段触发真实 INSERT 失败，不删除、改名或破坏 audit 表。

### 保护与恢复

- 执行前确认目标库、UTF8/PG16 与 Flyway 历史；测试 profile 限定 Linux、localhost 及固定测试库名，拒绝连接业务库运行写测试。
- 业务库升级前用服务器本地 `pg_dump -Fc` 备份至受限目录；V3 为事务性迁移，失败应整体回滚，查询 Flyway 及对象核实，不修改历史 checksum。
- 不自动 DROP 数据库、表或既有数据；若业务库升级后需要撤回，先停止写入并评估 tags 新数据，再单独确认恢复方案。
- 启动后端的 Flyway/bootstrap 都属于写入路径，只在服务器执行。Windows 后端关闭两者并设置只读 JDBC 事务模式，仅用于启动与读接口检查。
- 临时服务器后端仅监听 loopback，通过 SSH 转发与 Vite 联调，不新增防火墙开放。

## 当前验证证据

- 本机 `mvn -pl backend verify`：Java 17.0.14，27 tests（A1 8、审计事务 6、名称/认证上下文 13）通过。H2 的 Flyway target 明确限定 V2，不代表 A2 PostgreSQL 验证通过。
- CentOS PostgreSQL 16.15 / Java 17.0.16：`mvn -f /tmp/spkb9-validation/pom.xml -pl backend -Ppostgres-it verify` 通过 27 个单测及 9 个 PG 集成测试；新增审计表锁超时用例后，用同一 profile 的 `test-compile failsafe:integration-test failsafe:verify` 最终 10 项 PG 测试全部通过（0 failed/error/skipped）。
- 测试库 V1/V2/V3 Flyway 全部 success=true；G3 导入两位禁用用户与 10 条标签，中文“机器学习、深度学习、科幻、阅读笔记”和 Java 均正确，两个用户可同名。
- Windows 后端 8081 用 Java 17 启动，关闭 Flyway/bootstrap，数据库会话 `default_transaction_read_only=on`；`/actuator/health` 返回 UP。首次用环境变量拆分 Hikari connection-init-sql 与 read-only 的组合发生属性绑定时连接池提前启动，改为 JDBC options 后通过，没有改 Hikari 代码。
- `npm run build` 通过；仍有 >500KB 的 Vite 主 bundle 提示（约 843KB / gzip 272KB），不是构建失败，本轮未引入无关打包优化。
- `PLAYWRIGHT_CHANNEL=msedge npm run test:e2e` 通过：真实登录及中文创建、重名冲突、重命名、合并、刷新持久化；搜索空态、模拟 401 回登录、凭据不出现在控制台，无 pageerror。管理写操作没有 Mock，均经 Vite → SSH → CentOS → PG 测试库。
- 已检查 1280px 桌面与 390px 移动截图，无横向溢出，按钮可见；图片位于受忽略的 `backend/target/a2-desktop.png`、`a2-mobile.png`，不是提交素材。
- 业务库备份 `/home/chau/spkb9-before-v3-20261101.dump`（600，18474 字节），`pg_restore -l` 可读性检查通过；未进行破坏性恢复演练。2026-11-01 02:23 的服务器 Flyway 从 V2 升级 V3 成功，历史 1/2/3 全部 success=true；核验 tags 的主键、用户外键、CHECK、联合唯一约束存在。
- 升级后通过服务器后端真实登录成功，新增 AUTH_LOGIN 且 requestId 非空；Windows 只读后端 `/me`、`/categories`、`/tags` 成功，五类代码正确，tags 为 0。业务库没有导入测试用户/标签。
- 最终 Java 17 `mvn -pl backend verify` 和前端 `npm run build` 均通过；浏览器用例最后一次复跑通过，对 console args 的完整序列化结果断言没有密码/token，不只是检查控制台摘要文本。

### 执行细节与安全边界

- 服务器测试源码/日志/临时 JAR 位于 `/tmp/spkb9-validation`（目录 700）；数据库测试凭据从本机 `.env.local` 读取，经 SSH stdin 传入进程环境，不输出、写入草稿或提交。
- PowerShell 向 SSH 发送脚本需无 BOM UTF-8，并处理 CRLF；首轮 Maven BUILD SUCCESS，但包装脚本因 BOM/CRLF 返回错误退出码，修正后复跑 verify 得到真实退出码 0。
- psql 种子通过 `cat G3... | sudo -n -u postgres psql -X -v ON_ERROR_STOP=1 -d synapse_pkb_a2_test` 执行，不放宽临时目录权限。
- 本机使用现有配置账号配合数据库会话只读参数，未创建/修改数据库账号授权；这不是独立只读角色的权限隔离。长期部署仍应配置专用最小权限账号，本轮只读检查不执行任何写 SQL。
- Playwright 不开启 trace/video，密码与 token 只保留进程内用于断言，报告/截图不记录它们。401 用例为显式错误状态模拟，真实认证和越权拒绝另由 PostgreSQL 集成测试验证。
- 默认 `mvn verify` 不自动执行 PG 写测试；`postgres-it` 必须同时满足 Linux 和固定 localhost 测试库 URL，否则初始化失败，避免误写业务库。
- Windows 打包时需先停止占用 target JAR 的后端进程。本次曾因正在运行的自检 JAR 导致 repackage 重命名失败；停止自己的进程后完整 verify 通过，没有跳过测试。

### 实际数据库执行命令

```bash
# 新测试库由上文 CREATE DATABASE 创建；测试 profile 用 Flyway 执行原样 V1/V2/V3
cat /tmp/spkb9-validation/backend/src/main/resources/db/seed/G3__seed_tags.sql \
  | sudo -n -u postgres psql -X -v ON_ERROR_STOP=1 -d synapse_pkb_a2_test
# 业务库备份；使用受限目录/umask 077
sudo -n -u postgres pg_dump -Fc synapse_pkb > /home/chau/spkb9-before-v3-20261101.dump
pg_restore -l /home/chau/spkb9-before-v3-20261101.dump > /dev/null
# DB_URL 指向服务器 localhost/synapse_pkb，凭据仅由安全配置注入
java -jar /tmp/spkb9-validation/app-production.jar \
  --server.address=127.0.0.1 --server.port=18081 --app.security.bootstrap-user.enabled=false
```

结构性 SQL 实际执行内容就是本文链接的原样 V1/V2/V3、CREATE DATABASE、测试探针表；测试探针只在隔离库，业务库仅执行 V3。真实测试 DML 覆盖创建/重命名/合并/并发/隔离，使用每测试独立用户；审计写失败以超长字段模拟，审计锁超时以测试库事务中的 `LOCK TABLE operation_logs IN ACCESS EXCLUSIVE MODE` 验证，不改业务库审计结构。

### 收工环境

本轮仅临时验证，不是正式部署。第一轮自检服务已停止；随后按用户批准轮换实际凭据，并应用户登录查看需求重新启动业务库后端（服务器 loopback 18081）、SSH 转发及本机 Vite 5173。最终前端指向真实 `synapse_pkb`，不是测试库。测试后端与本机只读自检后端停止；不影响 IDEA 或原有服务器服务。测试库、受限测试目录和业务库备份保留以便复核，不自动 DROP 或删除备份。

### 用户批准的凭据轮换

- 收工扫描发现实际登录口令/JWT 密钥与历史测试默认值相同，旧日志含同一口令；用户明确批准立即轮换。详见 BUG-005。
- 新口令使用 24 字节安全随机数、新 JWT 密钥使用 48 字节安全随机数，Base64 编码后只保存在本机受忽略的 `.env.local`；未在对话、文档、命令输出或 Git 展示。
- CentOS bootstrap 更新实际用户的 BCrypt 哈希，新进程加载新 JWT 密钥；用户名保持不变。测试配置改用独立仅测试值，旧日志脱敏，未改写 Git 历史或强推。
- 实际服务器验证：新口令登录成功、新 token 查询成功；旧口令返回 401，用旧签名密钥构造的 token 返回 401。
- 凭据扫描复核当前变更文件为 0 命中；本地 27 tests 在隔离测试凭据后再次通过。
- 用户登录入口为 `http://localhost:5173`，账号读取 `.env.local` 的 `APP_BOOTSTRAP_USERNAME`，密码读取同文件的 `APP_BOOTSTRAP_PASSWORD`。不在正式文档复制密码。现有其他手工启动配置若曾硬编码旧值，需要改为读取本地专用配置。

### 复跑入口

```powershell
# 本机 Java 17；打包前停止占用 target JAR 的自检进程
mvn -pl backend verify
# frontend 目录
npm run build
# 凭据 A2_E2E_USERNAME/A2_E2E_PASSWORD 从本地配置注入，不打印；代理必须指向 CentOS 后端
$env:PLAYWRIGHT_CHANNEL = 'msedge'
npm run test:e2e
```

服务器执行（DB_USERNAME/DB_PASSWORD 通过安全配置注入）：

```bash
export A2_TEST_DB_URL=jdbc:postgresql://127.0.0.1:5432/synapse_pkb_a2_test
mvn -f /tmp/spkb9-validation/pom.xml -pl backend -Ppostgres-it verify
```

## 待验证边界

只验证标签字典合并；条目关联迁移、使用统计、AI 候选与采纳不在本轮完成口径内。真实 PostgreSQL 全迁移、种子及故障测试不能被 Mock/H2 结果替代。
