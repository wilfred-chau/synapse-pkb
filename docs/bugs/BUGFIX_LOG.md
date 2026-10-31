# 个人知识库系统（PKB） · 缺陷修复记录（活跃卷）

> 纯追加，条目模板见 skill 内 `templates/bugfix-entry.md`。
> 编号 BUG-NNN 全局自增、永不复用；超过阈值须轮转入 history/。

## 历史卷索引

_(暂无轮转卷)_

---

_(暂无记录)_

---

## BUG-001｜Jira 建票中文标题显示为 `??`

**背景**：在 Jira Cloud 中查看验证票 `SPKB-1` 时，原本应为中文的标题显示成 `?? Jira Cloud ??? dev-with-ticket skill`，说明标题文本在建票链路中发生了编码丢失。

**现象**：
- Jira 看板卡片标题中的中文被替换成 `??`
- 通过 API 读取 issue summary，返回值同样已经是 `??`

**根因**：
- 首次建票时，中文 `Summary` / `Description` 通过 PowerShell 命令行参数直接传给脚本
- 在当前 Windows PowerShell + 命令调用链中，这类非 ASCII 文本在进入脚本前已经被降级为 `?`
- 因此 Jira 本身并不是显示异常，而是接收到的内容就已经损坏

**修复**：
- 为 `create-jira-issue.ps1` 增加 `-SummaryFile` / `-DescriptionFile` 两种 UTF-8 文件输入方式
- 在脚本中显式设置 UTF-8 输入/输出编码
- 在 `dev-with-ticket` skill 中补充规则：若标题或描述包含中文，优先使用 UTF-8 文件输入
- 通过 UTF-8 字节请求体修正现有 Jira 票 `SPKB-1` 的标题

**验证**：
- 再次读取 `SPKB-1` 的 summary，已返回 `配置 Jira Cloud 集成与 dev-with-ticket skill`

**经验教训**：
- 通过 PowerShell 命令行参数传递中文给外部脚本并不稳，尤其当后续还要再发 HTTP 请求时
- 对 Jira 这类需要保真文本的接口，优先使用 UTF-8 文件输入或显式 UTF-8 请求体

---

## BUG-002｜dev-with-ticket 建票请求体未显式按 UTF-8 字节发送，导致中文正文再次损坏

**背景**：在创建 `SPKB-2` 时，虽然 `dev-with-ticket` 已经改成通过 UTF-8 文件输入标题与描述，但 Jira 中该票的中文标题和描述仍被写成了 `?`，说明问题不在终端显示，而是在 HTTP 请求发出前或发出时再次发生了编码破坏。

**现象**：
- Jira 看板中的 `SPKB-2` 标题显示为问号
- Jira issue 详情页中的中文描述同样丢失为问号
- 脚本本身不报错，但 Jira API 回读到的 summary/description 已经损坏

**根因**：
- `create-jira-issue.ps1` 在调用 `Invoke-RestMethod` 创建 issue 时，直接把 JSON 字符串作为 `-Body` 传入
- 在当前 PowerShell/Jira 调用链下，字符串请求体未显式转换为 UTF-8 字节流，导致中文在传输阶段被降级
- 请求头中的 `Content-Type` 也未显式带上 `charset=utf-8`
- 脚本文件本身经十六进制检查为 ASCII 内容，无 BOM；由于脚本内没有中文字面量，这不是本次乱码的直接根因

**修复**：
- 在脚本中补充 `$OutputEncoding = [System.Text.Encoding]::UTF8`
- 将 JSON body 通过 `[System.Text.Encoding]::UTF8.GetBytes(...)` 转为字节数组后再发送
- 将请求头 `Content-Type` 改为 `application/json; charset=utf-8`
- 对通过文件读取的 `Summary` / `Description` 做 `Trim()`，避免尾部换行被带入 Jira 字段
- 使用修复后的脚本重新创建中文测试票 `SPKB-3`
- 使用修复后的脚本重建 A1 正确工单 `SPKB-4`，并将损坏的 `SPKB-2` 标记作废并关闭

**验证**：
- Jira API 回读 `SPKB-3`，summary 为 `验证中文建票编码修复`，description 中文内容完整
- Jira API 回读 `SPKB-4`，A1 工单标题与描述中文完整
- 浏览器验证尝试已执行，但当前自动化会话未登录 Atlassian 网页端，只能到达登录页，因此网页内页核验受登录态限制；已用 Jira API 回读结果完成内容保真验证

**经验教训**：
- “输入是 UTF-8 文件”并不等于“HTTP 发送也是 UTF-8”；PowerShell 请求体编码必须显式控制
- 对外部 API 的中文文本链路，要同时检查：输入源编码、进程内字符串处理、HTTP 头、HTTP 请求体字节流四个环节

---

## 2026-11-01 BUG-003: 审计方法返回后直接写入，缺少事务提交与失败隔离（SPKB-9）

**现象**
- 既有 AUTH_LOGIN 样板通过不能证明事务业务安全；切面在方法返回后直接写审计，审计 SQL 可能加入业务事务，数据库报错后 catch 无法恢复 PostgreSQL 已失败的事务。
- 若仅提前改成独立事务写入，又可能在业务后续回滚时留下成功事件。

**根因**
- 未指定事务与审计 advisor 顺序，无提交同步回调，writer 无独立事务边界；原 try/catch 没有覆盖未来 afterCommit 回调执行及独立事务提交失败。

**修复**
- 事务 order=100，审计 order=200；复制最小快照，在真实事务 afterCommit 中调用独立代理 REQUIRES_NEW writer。
- 回调内部捕获写入及提交失败，writer 3 秒超时；无事务登录继续成功后写入，无变化的标签重命名跳过审计。

**验证**
- Java 17 本地 27 项测试通过，其中 6 项审计事务测试验证外层回滚、业务异常、快照、真实 INSERT 失败与 customizer 失败。
- CentOS PostgreSQL 16.15 最终 10 项集成测试通过，包含延迟外键导致的真实提交失败、审计 INSERT 失败、审计表被锁超时后业务仍提交且 HTTP 200。
- 真实登录及标签页面操作验证通过，详见 [A2 验证记录](../qa/a2-verification.md)。

**经验教训**
- 方法返回不等于事务提交；catch 不能挽救数据库事务失败状态；afterCommit 残留资源必须通过独立事务写入，回调抛异常也会影响调用者。
- 提交后尽力写入仍可能在宕机窗口漏记，本轮不保证 exactly-once，不支持 NESTED 部分回滚审计。

---

## 2026-11-01 BUG-004: 前端完整请求响应调试日志可能泄漏凭据（SPKB-9）

**现象**
- api.ts 在开发环境输出 request.data/response.data，登录请求密码及响应 token 可能进入控制台；直接输出归一化错误对象也会带上服务端错误详情。

**根因**
- 调试日志没有限定字段白名单；审计和全局异常日志输出完整异常，SQL 异常可能带出参数值。

**修复**
- 前端仅输出方法、路径、状态、耗时、错误码、requestId；不记录完整请求、响应或错误详情。
- 后端审计/全局/标签异常只输出受控元数据与异常类型；应用默认 INFO，并对 user/operationlog Mapper 固定 INFO，避免 SQL 调试参数输出。

**验证**
- 前端构建及真实登录/管理 Playwright 用例通过，控制台不包含实际登录密码或本次 token，无 pageerror。
- 后端异常故障用例通过，告警只包含事件、requestId、异常类型；未提交本地凭据、测试报告或截图。

**经验教训**
- 调试环境同样需要凭据保护；不能靠“只在 DEV 开启”替代日志字段白名单。

---

## 2026-11-01 BUG-005: 实际认证凭据复用已入库的测试默认值（SPKB-9）

**现象**
- 收工凭据扫描发现本机专用配置的实际登录口令与 JWT 密钥仍等于历史测试配置；旧开发日志也记载了同一口令。
- 命中内容在本轮之前已存在于 HEAD，删除当前文件内容不能消除 Git 历史中的暴露。

**根因**
- 测试示例凭据被复用于实际认证配置，缺少首次使用时的独立生成和轮换。

**修复**
- 用户明确批准立即轮换；用安全随机数生成新的实际口令和 JWT 密钥，仅写入受忽略的本机 `.env.local`。
- CentOS 本地后端通过 bootstrap 更新实际用户口令并加载新 JWT 密钥，测试后端也切换新专用配置；不在命令输出、对话、文档或 Git 中展示新值。
- 测试 YAML/A1 测试使用独立的仅测试凭据，旧开发日志将口令脱敏。此次旧日志修改只删除敏感值，不改写历史事实。

**验证**
- 轮换后的实际登录及旧凭据拒绝结果见 [A2 验证记录](../qa/a2-verification.md)。
- 本地测试、页面登录及凭据扫描重新执行，不用历史成功结果代替轮换验证。

**经验教训**
- 示例凭据不是部署凭据；日志脱敏与实际凭据轮换必须同时考虑。
- 未改写 Git 历史或强推；历史仍包含已撤销旧值，旧备份含历史密码哈希，应继续保持受限权限。
