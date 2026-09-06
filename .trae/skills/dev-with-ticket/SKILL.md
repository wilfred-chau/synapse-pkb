---
name: "dev-with-ticket"
description: "Creates a Jira ticket before development or bug-fix work starts. Invoke when user asks to implement a feature, continue development, refactor, or fix a bug."
---

# dev-with-ticket

这个 skill 用于把 **Jira 建票** 接到当前项目的开发流程里。它的职责不是替代 `doc-pilot`，而是作为
在 `doc-pilot` 值守下的开发前置门：**先建 ticket，拿到编号并让用户确认，再开始实际开发**。

## 触发时机

在以下场景触发：

- 用户要求开发某个新功能
- 用户要求继续开发某个模块
- 用户要求修复某个 bug、报错、异常或回归问题
- 用户要求重构某段代码，且该任务需要实际改码

以下场景不触发：

- 纯问答、纯查询、纯需求解释
- 只整理文档、只盘点结构、只做环境只读检查
- 用户明确说这次不需要建票

## 与 doc-pilot / plan-before-code 的配合关系

如果任务本身属于开发 / 修复：

1. 先按 `doc-pilot` 的开工读取路由读取 `docs/` 必要文档，并完成任务分级判断
2. 若任务分级为 L3/L4，且已启用 `plan-before-code`，则先在 `doc-pilot` 值守下完成方案确认
3. 再用本 skill 创建 Jira ticket
4. 若前面经过了 `plan-before-code`，优先使用**已确认方案摘要**作为 Jira Description
5. 把 ticket 编号回报给用户确认
6. **只有用户确认无误后**，才开始实际开发
7. 收工时继续按 `doc-pilot` 回写 `STATUS.md`、`dev-log.md` 等文档，并按需更新跨会话任务记录
8. commit message 草案需带 ticket 编号前缀，如 `[SPKB-1] fix xxx`

如果 `docs/.doc-pilot.json` 已启用 `ticket_system.enabled = true`，则本 skill 同时作为
`doc-pilot` 的前置建票门实现，三者的详细职责边界见 `doc-pilot` 的
`references/skill-contract.md`。

## 当前项目约定

- Jira 站点与凭据：来自本地 `.env.local`（具体路径见项目 CLAUDE.md 或 `docs/.doc-pilot.json`）
- 项目 Key：`SPKB`
- 默认 issue 类型映射：
  - 功能 / 开发 / 重构 -> `task`
  - bug / 报错 / 异常 / 回归 -> `bug`

## 本地配置要求

- Jira 凭据只允许放在本地专用配置文件里，不得写进会被 Git 提交的文件
- 每次使用前，若怀疑配置异常，先确认该配置文件存在且仍被 `.gitignore` 排除

## 建票执行方式

优先复用本 skill 自带脚本：

`scripts/create-jira-issue.ps1`

脚本的实际路径以 `docs/.doc-pilot.json` 的 `skills_registry.dev_with_ticket` 字段为准（该
字段登记的是"本项目当前使用的 dev-with-ticket skill 根目录"），下方示例中的路径占位符
`<skill_root>` 需要替换成该配置值——这样同一份 skill 无论安装在 `.trae/skills/`、
`.claude/skills/` 还是其他宿主工具的约定目录下，都不需要改这份 SKILL.md 正文。

示例：

```powershell
powershell -ExecutionPolicy Bypass -File "<skill_root>/scripts/create-jira-issue.ps1" `
  -Summary "实现条目基础 CRUD 骨架" `
  -Description "根据用户需求，开始实现基础平台的 Entry CRUD 最小闭环。" `
  -IssueType task `
  -ProjectKey SPKB
```

或创建 bug 票：

```powershell
powershell -ExecutionPolicy Bypass -File "<skill_root>/scripts/create-jira-issue.ps1" `
  -Summary "修复登录接口 500 错误" `
  -Description "用户反馈登录接口返回 500，需要定位并修复。" `
  -IssueType bug `
  -ProjectKey SPKB
```

若标题或描述包含中文，优先使用 UTF-8 文件输入，避免命令行参数编码把中文变成 `??`：

```powershell
powershell -ExecutionPolicy Bypass -File "<skill_root>/scripts/create-jira-issue.ps1" `
  -SummaryFile ".tmp/jira-summary.txt" `
  -DescriptionFile ".tmp/jira-description.txt" `
  -IssueType task `
  -ProjectKey SPKB
```

## 标题与描述生成规则

- 标题短而明确，直接概括本次任务目标
- 描述至少包含：
  - 背景 / 用户诉求
  - 预期修改范围
  - 若是 bug，补充现象与影响
  - 若已有 `plan-before-code` 方案，优先用该方案摘要作为描述主体
- 不编造验收结果，不提前写"已完成"

## 回报格式

创建成功后，先向用户回报：

- ticket 编号
- ticket 标题
- issue 类型

示例：

```text
已创建 Jira ticket：SPKB-12
类型：任务
标题：实现条目基础 CRUD 骨架
```

随后明确等待用户确认。未经确认，不进入实际开发。

## 收工约束

- commit message 草案需带 ticket 编号，例如：
  - `[SPKB-12] feat(entry): add entry CRUD skeleton`
  - `[SPKB-18] fix(auth): handle invalid jwt parsing`
- 同时给出本次建议执行 `git add` 的文件清单
- 若任务由本 skill 触发，且 `doc-pilot` 已启用建票门，则回写日志时建议写入 ticket 编号；
  若本次任务存在跨会话任务记录，同步登记 ticket 编号到该记录中
