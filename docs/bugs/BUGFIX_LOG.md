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
