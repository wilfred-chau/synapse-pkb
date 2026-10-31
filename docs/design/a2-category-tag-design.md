# A2 分类与标签管理设计

> 日期：2026-11-01。状态：D1–D6 已获用户确认，SPKB-9 已创建，待开发前工单确认；未开始实现或执行数据库变更。
> 任务：TASK-20261101-A2-TAGS（L4）。本轮仅覆盖 A2 非 AI 管理闭环。
> 需求依据：`../requirements/requirements-full.md` 第 2、3、4、5、6、11、12 节。

## 1. 开工核对事实

- Git：`dev`，HEAD `4224bd0`，fetch 后与 `origin/dev` 一致；开工时只有已有的 `docs/.doc-pilot/` 未跟踪记录，无业务代码改动。
- A1：JWT 登录、当前用户、个人空间壳已实现；认证过滤器从数据库重新读取用户，再构造 `AuthenticatedUser`。
- 后端：Spring Boot 3.3.4、Java 17、MyBatis-Plus 3.5.7；按业务域分层，XML 位于 `resources/mapper/<domain>/`。不是 JPA，未发现 Hibernate ORM 校验机制。
- 仓库迁移只有 V1 用户表、V2 审计表；未发现 category/tag/entry 业务实现或建表脚本。线上表结构本轮尚未重新查询，不以仓库检查代替数据库核验。
- 前端只有登录及个人空间展示，`api.ts` 只有 GET/POST 帮助函数；尚无分类、标签管理页面或前端测试脚本。
- 审计已完成 AUTH_LOGIN 样板。G2 种子验证在 STATUS、开发日志、任务记录中均已收尾，不是本轮阻塞。
- 已有测试为 H2 PostgreSQL 模式的 A1 集成测试，只验证登录成功写审计，没有事务回滚、审计写失败、标签并发或隔离测试。本轮未重新执行这些历史测试。
- 本地既有计划和任务未发现 A2 专属工单。方案确认后再按 dev-with-ticket 查询 Jira 去重；不能据此声称远端没有 A2 票，也不复用已完成的 SPKB-8 作为 A2 实现票。
- 文档差异：STATUS 尚无 A2 独立状态；部署手册“暂未建表”已落后于 V1/V2 及开发日志。配置未登记 skills_registry，本轮按用户提供的实际技能路径执行，不顺带重构技能。

后续进展（2026-11-01）：用户已提交方案（`0c98799`），并明确回复“同意 D1–D6，进入工单环节”。fetch 后 dev 与 origin/dev 一致，工作区开工时干净。Jira 已完整查询当前账号可见的 8 张 SPKB 工单，并复核涉及 A2 的 SPKB-4/5/7/8 描述，没有匹配 A2 非 AI 管理闭环的实现票。用户随后确认建票草稿，已创建 [SPKB-9](https://wilfredchau.atlassian.net/browse/SPKB-9)“实现 A2 非 AI 分类与标签管理闭环”（任务，Backlog），API 回读标题和描述与 UTF-8 草稿完全一致；等待用户确认票号后再开发。旧票部分仍处于 Backlog，不以 Jira 状态推翻代码与验证事实，也不代用户流转旧票。

## 2. 范围与完成口径

本轮计划实现固定分类查询、本人标签列表/创建/重命名/合并、前端操作闭环、V3 迁移及种子、测试、事务一致的成功审计。未要求单独删除标签，不新增删除接口。

不实现 entries、entry_tags、条目 CRUD、上传、抓取、检索、模型调用、推荐样板或分类管理平台。AI 候选标签只定义后续契约，不创建空实现或伪数据接口。

完成后只能标记“A2 非 AI 管理完成；A2 部分完成”。条目关联迁移及使用统计随 A5 接入验证，AI 3–5 个候选与采纳/修改随内容处理接入验收。

## 3. 固定分类

后端 `category` 域用枚举作为代码与展示名唯一来源，提供受认证保护的 `GET /api/categories`：

| code | name | 顺序 |
| --- | --- | --- |
| tech | 技术资料 | 1 |
| novel | 小说 | 2 |
| comic | 漫画 | 3 |
| movie | 电影 | 4 |
| note | 生活笔记 | 5 |

返回 `ApiResponse<List<CategoryResponse>>`，元素为 code/name。前端从接口加载，不维护第二套名称表。分类没有数据库表、Mapper 或管理操作，也不为它强造空 Service。

未来 entries.category 保存稳定 code，并加五值 CHECK 约束。内容处理用 code 路由配置化 Prompt；分类值域固定不妨碍模板配置化。

## 4. 标签作用域与名称

### 4.1 作用域决策 D1（已确认）

推荐同一用户的标签跨分类共享：tags 不加 category 或 parent_id。需求没有明确“一分类一套标签”，且“科幻”“成长”等适用于小说、漫画与电影。分类归属由条目独立表达，标签用于描述特征，不擅自把“二级”解释成分类树约束。

不同用户允许同名；不做公共标签、家庭共享或用户管理。500 个是容量假设，不作为强制数量上限。

### 4.2 名称规则 D2（已确认）

推荐明确且可在 PostgreSQL 与 Java 一致实现的规则：

1. Unicode NFC 规范化，不使用 NFKC，不折叠全角字母、繁简体、同义词。
2. 将固定 Unicode White_Space 集合转为普通空格，去首尾空白，并将内部连续空白折叠为一个空格。集合为 U+0009–000D、0020、0085、00A0、1680、2000–200A、2028、2029、202F、205F、3000，不能依赖 Java/数据库各自不同的默认 `trim` 或 `\s`。
3. 规范化后必须为 1–32 个 Unicode 码点，中文与英文均按码点计数，不按字节或 UTF-16 单元截断。拒绝空值、全空白、剩余 C0/C1 控制字符及 U+200B/U+FEFF 隐形分隔符；不误删 emoji 组合中的 ZWJ。
4. 展示名保留英文大小写；判重键仅将 ASCII A–Z 映射为 a–z。中文精确匹配；其他文字允许输入，非 ASCII 大小写保持有别，明确不声称是完整 Unicode case-fold。
5. `Java`、`java` 与首尾空白变体冲突；`Spring  Boot` 与 `Spring Boot` 冲突；`机器 学习` 与 `机器学习` 不同；`Java` 与全角字母变体不同。
6. 创建同名返回 409 `TAG_NAME_CONFLICT`，不静默复用。重命名排除自身：只改大小写允许；规范化后展示名不变返回 `changed=false`，不产生修改审计；与他人标签同名不冲突，与本人另一标签同键返回 409，不隐式合并。

依据：NFC 避免编码等价重复，保留用户表达；ASCII 折叠覆盖当前中英文技术标签，避免运行环境 locale 改变结果。若需要全面多语言大小写等价，须另行选择并固定 Unicode case-fold 版本，并迁移冲突数据，不能悄悄改判重规则。

数据库最终裁决：以版本化的标签专用不可变 SQL 规范化函数约束 name；以生成列 name_key（从 name 派生，不由调用者填写）和 `UNIQUE(user_id, name_key)` 兜住并发。name/key 使用确定的 `C` 排序规则作精确比较，CHECK 覆盖规范形式、长度和非法字符。服务端先验证以提供友好错误，数据库阻止绕过接口的非规范写入。

Java 与 SQL 使用同一测试向量验证 NFC、空白、中文、扩展汉字、32/33 码点等边界。唯一键异常只按指定约束映射为冲突，并回滚业务事务；其他数据库异常不伪装成重名。改规范化函数需新 Flyway 迁移并重检数据，不修改已发布函数语义而不重算约束/键。

## 5. 推荐数据模型 D3（已确认）

### 5.1 本轮仅新增 tags

| 字段 | 类型/约束 | 用途 |
| --- | --- | --- |
| id | BIGSERIAL PRIMARY KEY | 稳定标签 ID |
| user_id | BIGINT NOT NULL，FK pkb_users(id)，默认限制删除 | 用户归属 |
| name | VARCHAR(32) NOT NULL，规范形式 CHECK | 展示名 |
| name_key | TEXT 生成列，NOT NULL，C 排序 | 名称判重键 |
| created_at / updated_at | TIMESTAMPTZ NOT NULL | 沿用时间填充约定 |

联合唯一键 `(user_id, name_key)` 的前缀可支持用户查询，不盲目重复建 user_id 单列索引。列表按 name_key/id 稳定排序。生成列不是可独立修改的第二份业务数据。

本轮不保存 usage_count。页面提示“条目使用统计待条目模块接入”，不把固定 0 或虚构次数包装成已实现统计；API 本轮不返回使用次数。

### 5.2 数组与关联表比较

| 方案 | 优点 | 问题 |
| --- | --- | --- |
| entries.tags TEXT[] 为唯一关联来源 | 接近需求示意；数组交集/GIN 简单 | 名称非稳定 ID；重命名需批量改条目；无法对每个元素用普通 FK；去重、隔离和使用次数维护易漂移 |
| tags + entry_tags 为唯一关联来源 | 稳定 ID、FK、联合唯一键；重命名不改条目；合并/统计可事务化 | 多一张关联表；筛选需 JOIN/聚合；偏离需求第 11.1 节示意字段 |
| 数组与关联表双写 | 可直接读取两种形式 | 两份来源易不一致，增加修复机制，拒绝 |

推荐第二种：本轮建 tags；A5 才建 `entry_tags(user_id, entry_id, tag_id)`，唯一键 `(user_id, entry_id, tag_id)`，并通过 `(user_id, entry_id)`、`(user_id, tag_id)` 复合外键限制跨用户关联（相应父表补联合唯一约束）。不建 entries.tags 存储列；API 需要标签数组时按关联生成 VO。

这是对需求示意模型的明确调整，已由用户于 2026-11-01 批准；不直接改需求快照假装原文如此。批准记录见本文决策表，未来 A5 方案引用它。

使用次数以关联表与未软删除 entries 的实时聚合为准，不在 tags 中保存可写计数器；恢复条目自动恢复计数，合并不能简单“两个计数相加”。该建议已随 D3 一并批准，满足统计能力但调整物理存储位置。实测查询性能不达标时才引入可重建的派生缓存，不替代关联表来源。

## 6. 服务端隔离与 API

TagService 从现有 Spring Security 的认证上下文解析 `AuthenticatedUser.id`；仅抽出一个小型安全访问助手供实际调用，缺失/非法上下文拒绝，不默认回落用户 1。DTO 不接收 userId/nameKey；前端只提交名称及操作对象 ID。

查询、唯一性预检、行锁、UPDATE、DELETE 全部带 user_id。不能先无归属 selectById 再无条件 updateById/deleteById；合并源和目标必须都属于当前用户。不存在和不属于本人统一返回 404 `TAG_NOT_FOUND`，不泄露其他用户名称、ID 是否存在。

| 接口 | 请求 | 返回 data |
| --- | --- | --- |
| GET /api/categories | 无 | 五类 code/name |
| GET /api/tags | 无 | 本人 TagResponse 列表 |
| POST /api/tags | `{name}` | 创建的 TagResponse |
| PATCH /api/tags/{id} | `{name}` | TagResponse、changed |
| POST /api/tags/{sourceId}/merge | `{targetTagId}` | sourceTagId、targetTag（VO） |

统一 ApiResponse/BusinessException/ApiErrorCode，不返回 Tag 实体。标签错误枚举放 tag 域；前端 api.ts 补 PATCH helper。请求格式与名称错误 400，认证错误 401，跨用户/缺失 404，重名或并发状态冲突 409。并发锁等待超时映射为可重试的 `TAG_OPERATION_CONFLICT`，不无限等待或无限自动重试。

前端在个人空间增加“分类与标签”入口或页签：只读五类说明、共享标签列表、本地搜索、创建/重命名弹窗、源到目标合并确认。选择目标排除源；加载/空态/失败/提交禁用/401 均有明确反馈。成功后重读列表，以服务端名称和结果为准，不展示条目迁移数量。

## 7. 合并规则 D4（已确认）

推荐一次一个源到一个目标，不增加批量合并、别名树或撤销系统。

1. sourceId 与 targetTagId 必须是不同的正 ID；相同返回 400 `TAG_MERGE_SAME_TAG`。
2. Service 使用单个 REQUIRED 事务；按 ID 升序锁定当前用户的两条标签（XML 中 `ORDER BY id FOR UPDATE`），再验证两者均存在，避免相反顺序合并的死锁。
3. 当前没有 entries：只删除源标签，保留目标 ID/name/created_at，更新目标 updated_at。源名称释放，可重新创建并获得新 ID。审计记录源、目标 ID 与最小必要名称快照。
4. 源删除影响行数必须为 1，否则整笔回滚。重命名也使用同样的按用户行锁/更新规则，与合并串行协调。
5. 已成功合并后重复请求：源不存在，返回 404，不再修改或新增成功审计。最终数据效果可重复安全，但不保证重复请求返回同一成功响应。网络超时后刷新列表；不能把 404 当作“已确认上次成功合并到此目标”。
6. 并发 A→B 与 B→A：一笔完成，另一笔重新核验发现源/目标缺失而失败。A→B 后 B→C 是两次明确操作，不自动维护 A 的永久转向。

硬删除适合尚无关联的当前范围。若用户要求重试仍回放成功、旧 ID 永久可解析或可撤销，则需合并记录/幂等键等额外模型，应重新确认，不能从不可靠的审计日志重建业务幂等性。

### 未来条目关联迁移，未实现

A5 引入关联后，同一事务中锁标签 → 将源关联插入目标（`ON CONFLICT DO NOTHING` 去重）→ 删除源关联 → 删除源标签；软删除条目的关联也迁移，以保证恢复正确。使用次数按存活条目重新聚合。

条目增删标签写入也必须遵守同一标签锁协议，防止合并时新挂源；复合外键作为最终防线。条目新增、软删除、恢复与统计读的一致性均在 A5 实际验证。现在不创建空迁移接口、空关联表或测试假表来声称已经支持这些能力。

若未来真的存在旧 TEXT[] 数据，须先按用户展开并规范化名称、审阅冲突、回填 tags/entry_tags、比对集合及计数，再切换读写并移除旧字段；这是一项未来独立迁移，不是本轮已完成内容。

## 8. 成功审计与事务 D5（已确认）

### 当前骨架的实际限制

`AuditOperationAspect` 在 `joinPoint.proceed()` 返回后立刻调用 `OperationLogService.create`，无显式 advisor 顺序、无提交回调；写入 Service 也没有独立事务声明。

- 方法返回不等于外层事务已提交。同事务写审计通常能随业务回滚，但审计 SQL 出错可能让 PostgreSQL 事务进入失败状态，catch 无法恢复事务。
- 若直接改成提交前 REQUIRES_NEW 写审计，则外层随后回滚会留下成功事件。
- 因此既有 AUTH_LOGIN 非事务样板不能证明事务业务的两项保证。

### 推荐的最小加固

1. 明确事务 advisor 在外、审计 aspect 在内（事务 order 小于审计 order），通过真实代理测试锁定，不依赖同优先级偶然顺序。
2. 业务方法成功返回后生成不可变/拷贝的最小审计快照；立即捕获用户、requestId、源/目标及结果，不在延迟回调中读取已失效的请求/安全上下文，不保存完整 args/result。
3. 有真实事务时注册 TransactionSynchronization，仅 afterCommit 尝试落库；事务回滚、rollback-only 或提交失败不写成功审计。无事务的 AUTH_LOGIN 仍在成功返回后写入。
4. 通过独立 Spring Bean 代理调用 `OperationLogService.create` 的 REQUIRES_NEW 写事务。afterCommit 时原连接资源可能仍绑定，不能复用默认 REQUIRED 或同类 self-invocation。
5. draft/customizer 失败和提交后 writer 的执行/提交失败分别在对应边界捕获；特别是 afterCommit 异常会传播给调用者，必须在回调内部隔离，不能只包注册回调那一段。
6. 只记录实际成功变更 TAG_CREATE/TAG_RENAME/TAG_MERGE；给 draft 增加最小“跳过”能力用于 changed=false，不引入通用事件框架。
7. 失败告警只输出 action、requestId、方法、受控错误类别，不直接输出可能携带 SQL 参数或凭据的异常消息/完整堆栈。前端 api.ts 去掉请求/响应 data 调试输出，保留方法、路径、状态、耗时和 requestId。

保证边界：业务回滚不留成功事件；审计独立写入失败不回滚业务、不把已提交业务包装成失败响应。当前方案是提交后尽力写入，不保证宕机窗口零丢失或 exactly-once。同步尝试仍有耗时，须设置有限事务/语句/取连接超时并验证，不能声称“零延迟”。

若要求审计零丢失，需要事务 outbox 与重试，其写入仍与主事务耦合且增加运维复杂度；本轮不引入。若要求审计绝不增加响应等待，需另行确认有界异步投递及其丢失边界。

本轮支持 REQUIRED 标签事务及真实外层事务回滚，不声明支持 NESTED/savepoint 部分回滚审计。出现该传播方式时必须重新设计与测试。

依据：已对本地实际 Spring 6.1.13 jar 使用 javap 核实 TransactionSynchronization、TransactionSynchronizationManager、EnableTransactionManagement.order，以及 AbstractPlatformTransactionManager 的 doCommit → afterCommit 顺序；官方文档明确提交后写入须 REQUIRES_NEW 且回调异常可传播。

## 9. AI 接口边界

后续内容处理成功后调用标签候选提供者，输入为服务端确定的用户上下文、entryId/contentVersion、category 及已抽取正文/摘要；用户归属不来自模型或请求体。

契约建议为候选结果 `{status, entryId, contentVersion, candidates:[{name, existingTagId?}]}`；status 区分 pending/ready/failed，正常 ready 目标 3–5 个互异候选，不足时不伪造补齐。候选不等于已采纳标签，不自动创建 tags 或 entry_tags。

用户采纳/修改后，未来条目服务重新校验条目归属与版本，复用相同名称规则并确认/创建本人标签，再事务性写 entry_tags。候选 existingTagId 也必须重新校验，防止生成后标签已合并或改名。模型失败不阻断手工管理，重试不重复关联。

本轮只记录此契约，不注册未实现的公开 HTTP endpoint，不添加空 provider 类，不展示伪推荐按钮或假成功。

## 10. 影响面与实施顺序

1. 用户确认 D1–D6，记录批准日期/修改项；dev-with-ticket 远端查重，复用匹配票或确认摘要后建票，回报票号确认后才动码。
2. 审计加固与泄密日志修复先行，保留 AUTH_LOGIN 回归测试；新增标签服务端认证访问助手。
3. V3（序号执行前复查）创建 tags、规范化函数、生成键、约束；种子使用中文、英文大小写和两个测试用户的同名标签。无 entries 或 operation_logs 结构变更。
4. 完成 tag 分层及 XML 查询/锁/合并；接分类枚举查询、错误码和 VO。
5. 前端管理页及 API PATCH/代理配置，完成成功、冲突、失败、重试、401 交互。
6. 本地编译/单测、前端构建与启动；服务器 PG16 迁移/种子、集成/故障/并发测试和页面代理联调。
7. 按证据更新 STATUS、dev-log、架构、部署/验证文档；实现了什么与尚未验证什么分开；提供带票号的英文 commit 草案和精准 git add 清单，不代执行。

预计新增：`category/{model,controller}`（枚举/VO/只读控制器），`tag/{controller,service,service/impl,service/audit,mapper,entity,model/dto,model/vo}` 与域错误码、名称规则；`resources/mapper/tag/TagMapper.xml`；V3、A2 种子；分类标签前端模块及相应测试。

预计局部修改：operationlog 的 aspect/service/draft、事务顺序配置；security 小助手；frontend/src/App.tsx、api.ts；测试配置/构建 profile；文档。严禁为未使用的层建立空接口。

### 数据库执行与启动 D6（已确认）

严格采用“真实数据库写入由 CentOS 本地进程执行”：Flyway、种子、PG 集成测试及写接口后端运行在服务器；Windows Vite 可通过配置代理/SSH 转发连接服务器后端执行页面闭环。不经 MCP 写库。

本机 IDEA 后端启动检查采用关闭 Flyway、禁用 bootstrap 的只读模式连接已迁移数据库，仅验证健康、分类/标签查询；带写入的登录审计与管理动作走服务器后端。当前 bootstrap 无开关且每次启动更新用户，需要仅新增一个 enabled 开关，默认行为不变，并用只读数据库账号作安全兜底；Vite 代理目标改为可配置且保留 localhost:8080 默认值。以上均属于验证所需的有限基础改动，不改登录协议。

当前 H2 不足以验证 PG NFC/生成列/锁/事务失败行为。推荐保留明确标记的 A1 H2 冒烟（迁移目标 V2，仅限测试配置），本机运行纯 Java/Mock 测试；PG16 全迁移与数据库集成测试单独 profile，在 CentOS 的隔离测试库执行。不能把 H2 通过当作 A2 数据层验收；若全迁移无法运行则数据库验收未完成。隔离测试库创建也属于 DDL，先说明再确认。

执行任何 DDL 前展示完整 SQL、目标库/版本、影响对象及数据保护/恢复办法，用户确认后才在服务器本地执行；方案批准不是对未知 SQL 的执行授权。V1/V2 不改写。普通种子 DML 按项目既有授权执行并验证，测试故障仅注入隔离测试库，不破坏实际审计表。读取优先数据库 MCP；当前会话未提供 PostgreSQL MCP，执行时按项目既有 psql 只读路径降级，凭据只从本地受忽略配置读取。

## 11. 验证矩阵（均待执行）

| 验证面 | 必须验证的结果 |
| --- | --- |
| 基线 | 后端编译/单测、前端构建、实际启动、A1 登录/me/401 不回归 |
| 分类 | 只有五类及固定代码，顺序一致，页面无管理入口 |
| 名称 | 中文/扩展汉字、空白集合、NFC、32/33 码点、大小写、非法字符、同名并发只成功一次 |
| 数据库约束 | 服务器本地直接写入非规范名/重复键/不存在用户均被约束拒绝；函数和 Java 测试向量一致 |
| 隔离 | 测试库两用户可有同名；列表互不可见；越权重命名、源/目标越权合并全被拒绝且无成功审计 |
| 合并 | 保留目标、删除源、源名称可复用、自合并拒绝、缺失/重复请求 404、并发反向合并不损坏 |
| 事务 | 操作中异常、外层回滚、rollback-only、真实提交阶段失败均无业务残留与成功审计 |
| 审计故障 | 测试库中让真实 audit INSERT 失败，业务仍提交且 HTTP 成功；customizer/序列化失败同样隔离；验证实际 Spring 代理及提交后路径 |
| 前端 | 创建/重命名/合并弹窗、刷新持久化、加载/空态/错误/禁重提交、401、Vite 代理写入服务器后端 |
| 敏感信息 | 浏览器与后端日志不包含密码/token/完整请求体；requestId 可关联；git diff 无专用配置 |
| 剩余项 | 没有 entries/entry_tags 时明确未测关联迁移/统计；无模型时明确未完成候选及采纳闭环 |

种子只写隔离测试用户及其标签，例如“机器学习”“深度学习”“科幻”“阅读笔记”“Java”；冲突用例放可预期失败的独立测试事务，不让种子脚本因一条预期错误中断。测试凭据运行时提供/生成，不把真实口令提交。清理仅针对本次测试用户和标识，绝不清空真实用户数据或历史审计。

## 12. 决策批准记录

用户于 2026-11-01 明确批准 D1–D6 并要求进入工单环节。批准的是设计及范围，不代表功能已实现，也不替代建票草稿、开发前工单确认或具体 DDL 执行确认。

| ID | 推荐 | 状态 |
| --- | --- | --- |
| D1 | 用户内跨分类共享，不增加标签分类归属限制 | 已确认 |
| D2 | NFC + 固定空白折叠，1–32 码点，ASCII 大小写不敏感，DB 约束最终兜底 | 已确认 |
| D3 | tags + 未来 entry_tags 唯一关联来源，放弃持久化 entries.tags；次数实时派生 | 已确认，需求示意调整获批准 |
| D4 | 单源合并，目标保留、源硬删除，重复 404；不做成功回放、别名或撤销 | 已确认 |
| D5 | 提交后独立事务尽力审计，接受故障/宕机可能漏记；局部修复前端敏感日志 | 已确认 |
| D6 | 严格服务器写库验证路径；允许 bootstrap 开关、代理配置及 PG 测试 profile | 已确认 |

## 参考

- [Spring 6.1.13 TransactionSynchronization](https://docs.spring.io/spring-framework/docs/6.1.13/javadoc-api/org/springframework/transaction/support/TransactionSynchronization.html)
- [PostgreSQL 16 字符串与 Unicode 规范化](https://www.postgresql.org/docs/16/functions-string.html)
