# doc-pilot 使用说明（用户手册）

> 面向使用者的操作手册：怎么唤起、几类任务各会发生什么、可直接复制的提示词。
> **AI 助手注意**：本仓库任何会话中，用户问「doc-pilot 怎么用 / 如何使用 / 使用说明 / 调用方法」
> 或「这个项目里有什么 skill」时，第一动作是读取并展示本文（清点类问题先动态列出当前项目
> skills 目录下的清单），不要自由发挥。

## 一句话理解

doc-pilot = 任意项目的**文档中枢 + 开发值守机制**：AI 开工前先读知识库再动手（防跨会话
失忆），收工后必写回记录（防重复踩坑）。首次进入一个新项目时会自动引导初始化知识库骨架
（见「首次使用」）。

## 首次使用（新项目自动初始化）

如果当前项目 `docs/.doc-pilot.json` 不存在，说明这是本 skill 第一次接入本项目。正常描述
任务即可（比如「继续开发登录模块」），AI 会先走一遍简短的初始化对话（项目名、是否有独立
的工单系统、commit 语言习惯等），然后生成 `docs/` 骨架与配置文件，再继续你原本的任务。
之后每次在本项目里使用都会自动读取这份配置，不需要重复初始化。

## 怎么唤起（三层保障，正常说话即可）

| 层级 | 机制 | 可靠性 |
| --- | --- | --- |
| 1. 自动触发 | skill description 语义匹配你的任务描述 | 首选，覆盖常见说法 |
| 2. 项目级规则兜底 | 若项目配置了每次会话生效的规则文件，要求执行同等动作 | 提示词偏门时接管 |
| 3. 手动点名 | 提示词里写「按 doc-pilot skill 开工」或「doc-pilot」 | 100% 保障 |

## 直接可复制的提示词

| 你想做的事 | 你可以说 | AI 会走 |
| --- | --- | --- |
| 继续开发 | 「继续开发 <模块名>」 | 模式 B-开发 |
| 新功能/规划 | 「规划 <某功能>」 | 模式 B-开发 |
| 修 bug/排查 | 「这个报错 <关键词> 帮我排查」 | 模式 B-修复 |
| 写/跑 E2E | 「给 <模块> 补测试用例」 | 测试路由 |
| 整理/归档文档 | 「整理一下文档」或「doc-pilot」 | 模式 A（新项目自动先走模式 C 初始化） |
| 了解系统全貌 | 「梳理一下系统现状」 | overview 精读 |
| 部署运维 | 「打更新包并说明回滚步骤」 | deploy 路由 |
| 问 skill 用法 | 「doc-pilot 怎么用」 | 元路由：展示本手册 |
| 问有哪些 skill | 「这个项目里有什么 skill 工具？」 | 元路由：列 skill 清单 + 展示本手册 |
| 问模块/编号定义（需启用需求源） | 「<某编号> 是什么？」 | 需求速查：检索 requirements-full.md 作答 |
| 问模块进展 | 「<模块> 做完了吗？」 | STATUS 模块表 |

## 几类任务时 skill 分别会做什么

### 0. 首次接入新项目（模式 C）

- 检测到 `docs/.doc-pilot.json` 不存在 → 简短询问项目名、是否有工单系统（有则记前缀与对应
  skill 路径）、commit message 语言、是否有 docx 需求原文 → 调用
  `scripts/init-kb.py` 生成 `docs/` 骨架与配置 → 汇报初始化结果 → 继续处理你原本的请求。
- 已有 `docs/.doc-pilot.json` 的项目不会重复触发本流程。

### 1. 开发任务（模式 B-开发）

- **开工读档**：⓪ 分支同步检查（仅 git 项目）——`git fetch` 后比对当前分支与远程，落后先
  `git pull --ff-only` 并按 duty-routes「远程拉取的记录规范」留痕 → ① 读
  `docs/progress/STATUS.md`（进度速览）→ ② 读 `docs/overview/architecture-snapshot.md`
  （系统全貌）→ ③ 按需选读相关 `design/` 方案、dev-log 近期条目（一次 ≤2 个大文件）→
  之后才动手写代码。
- **开发前置建票门**（仅当 `docs/.doc-pilot.json` 的 `ticket_system.enabled = true` 时生效）：
  动码前先按配置里登记的 `gate_skill_path` 建票——具体流程由该 skill 定义（查重/预览/确认/
  创建）；收工时 AI 给出带票号前缀的 commit 文案。若未启用工单系统，本项完全跳过，不会
  提及任何工单相关流程。
- **收工写回**：① 按模板追加 `dev-log.md`（若有关联票号则记录）→ ② 刷新 STATUS（模块表/
  最近动态/日期）→ ③ 架构有变则修订 overview → ④ 新增文档登记 `docs/README.md` 索引 →
  ⑤ 收工汇报末尾给出可直接粘贴的 git commit 文案（语言与风格取自配置的 `commit_message`
  字段）。

### 2. 测试任务（两条子路由）

- **写用例 / 跑测试**：先读 `docs/qa/` 下相关测试规划文档，选读 dev-log 中相关里程碑条目
  （踩坑密集区）。
- **测试暴露 bug / 修 bug**：先检索 `docs/bugs/` 全目录（现象关键词+模块+报错码，含 history/
  历史卷）防复发 → 命中历史条目则核对根因/经验教训 → 未命中则查 overview 横切机制再定位
  代码 → 修复后追加 BUG-NNN 记录（编号跨卷全局连续）并同步 STATUS。

### 3. 文档整理（模式 A，七步）

扫描散落 md（跳过 `docs/.doc-pilot.json` 的 `root_whitelist` 名单）→ 按 kb-layout 判定归属
并规范命名（kebab-case）→ 用版本控制工具迁移（保留历史）→ 超大日志按「天条目」轮转入
history/（阈值见配置，卷名带日期区间）→ 过时文档移 archive/ 加废止声明 → 刷新 README 索引
与 STATUS、修复全仓库旧路径引用 → 跑 `python scripts/check-docs.py` 全绿才算完成。

### 4. 元问题（有哪些 skill / 怎么用）

- 问「项目里有什么 skill / skill 列表」→ 先动态列出当前项目 skills 目录下的清单，再展示
  本手册；若用户问的是其他某个 skill 的具体用法，改读那个 skill 自己的 SKILL.md。
- 问「doc-pilot 怎么用」→ 直接展示本手册。

### 5. 需求速查（轻量查询，不开工，仅当启用需求源）

- **触发**：「某编号是什么」「某模块要做什么」「某范围」——纯定义/范围类提问，且
  `requirements_source.enabled = true`。
- **动作**：检索 `docs/requirements/requirements-full.md` 编号定义表 → 引用原文行作答
  （编号｜功能项｜本期范围）→ 追问进展时补 `STATUS.md` 模块表（需求定义 ≠ 实现现状，
  分开答）。
- **不做**：开工读档全流程、收工写回（纯查询无产出）。
- **区分**：问「是什么/范围」走本路由；问「怎么开发/继续做」走模式 B-开发。

## 如果 AI 没按规矩来（纠偏话术）

| 你观察到 | 你可以说 |
| --- | --- |
| 没读文档就直接写代码 | 「先读 docs/progress/STATUS.md 再动手」 |
| 修 bug 没查历史 | 「先检索 docs/bugs/BUGFIX_LOG.md 再修」 |
| 做完没写记录 | 「按 doc-pilot 收工检查单过一遍」 |
| 想强制激活 skill | 「按 doc-pilot skill 开工」 |

## FAQ

**Q：新项目第一次用需要我手动建 docs/ 目录吗？**
A：不需要。只要正常描述任务，AI 检测到 `docs/.doc-pilot.json` 不存在就会先走一遍简短问答
（项目名等几项），然后自动跑 `scripts/init-kb.py` 生成骨架，你也可以直接说「初始化文档库」
主动触发。

**Q：新会话第一句话推荐说什么？**
A：「读一下 STATUS.md，我们继续 <模块名>」——最短路径进入开工状态。AI 收到后（若为 git
项目）第一动作是确认分支最新，再进开工读档。

**Q：记不住触发词怎么办？**
A：不需要记。正常描述任务即可，语义匹配兜底；实在不确定就说「doc-pilot」。

**Q：项目里还有其他 skill 吗？**
A：这取决于具体项目——问「这个项目里有什么 skill 工具？」AI 会动态列出当前 skills 目录
下的实际清单，而不是本手册写死的名单（本 skill 设计为可被放进任意项目，不假设其他 skill
一定存在）。

**Q：需求文档在哪？AI 能直接读吗？**
A：仅当项目启用了需求源（`requirements_source.enabled = true`）时适用：`docs/requirements/`——
原始 docx 只读保存（source of truth），旁边有机器转换的 `requirements-full.md` 副本
（AI 可全文检索）。**策略是查 md 副本而非用 python-docx 直读 docx**：副本可全文检索、
表格已成结构化行，查询成本远低于每次解析二进制；`check-docs.py` 会校验「md 不比 docx 旧」，
docx 更新后过期即报错强制重转——首选 `scripts/docx-to-md.py`（python-docx，表格完整还原）；
无 Python 环境时降级 `scripts/docx-to-md.ps1`（表格平铺为逐行文本，仅 Windows）。

**Q：怎么验证知识库健康？**
A：`python scripts/check-docs.py`（跨平台；也可加 `--root <路径>` 指定仓库根）。检查项包括：
必备文件 / 索引完整 / BUG 编号跨卷连续 / STATUS 新鲜度 / 根目录整洁（尊重 root_whitelist）/
链接有效 / 容量红线预警与超限（阈值读配置） / dev-log 卷索引 / 需求快照新鲜度（若启用）。

**Q：历史卷是什么？内容会丢吗？**
A：dev-log 与 BUGFIX_LOG 超过配置里的容量阈值时须按规则轮转入各自的 `history/` 目录
（卷号递增、日期或编号区间入文件名，索引登记在活跃卷头部）。内容只读冻结永不丢，BUG 编号
跨卷连续、永不重置。

**Q：放到另一个项目里要改什么？**
A：原则上什么都不用改。把整个 skill 目录复制过去即可；第一次在那个项目里触发时，AI 会
引导你走一遍初始化问答并生成该项目专属的 `docs/.doc-pilot.json`。如果你想跳过问答直接
指定参数，也可以让 AI 直接运行 `python scripts/init-kb.py --project-name "..." [其他参数]`。

## 相关文件（skill 内部）

- [../SKILL.md](../SKILL.md)：skill 主定义（模式 A/B/C、容量红线、硬性约束）
- [duty-routes.md](duty-routes.md)：提示词触发词对照与检索技巧
- [verify-checklist.md](verify-checklist.md)：新会话验证清单
- [kb-layout.md](kb-layout.md)：docs/ 知识库结构、归档规范与配置文件说明
- [../templates/](../templates/)：devlog / bugfix / status 三模板 + 配置文件示例
- [../scripts/init-kb.py](../scripts/init-kb.py)：新项目知识库初始化脚本
- [../scripts/check-docs.py](../scripts/check-docs.py)：知识库一致性校验脚本（跨平台）
- [../scripts/docx-to-md.py](../scripts/docx-to-md.py)：docx → md 转换工具·首选
- [../scripts/docx-to-md.ps1](../scripts/docx-to-md.ps1)：docx → md 转换工具·降级（无 Python 时用）
