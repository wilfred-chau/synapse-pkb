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
