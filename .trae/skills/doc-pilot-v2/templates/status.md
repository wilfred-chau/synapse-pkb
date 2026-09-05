# STATUS.md 骨架模板

> 用于初始化或重构 `docs/progress/STATUS.md`。各区块按需增删行，但**区块结构与顺序保持不变**；
> 全文控制在 `docs/.doc-pilot.json` 的 `capacity_limits.status_max_lines`（默认 150）行内。
> 「最近动态」最多 8 条，超出时删最旧（dev-log 里都有）。

```markdown
# 项目进度速览（STATUS）

> AI 开发值守必读入口。上接知识库索引（../README.md），细节见 ../progress/dev-log.md。
> last-updated 隐藏标记供 check-docs.py 使用，更新正文日期时必须同步改。

<!-- last-updated: YYYY-MM-DD -->

## 模块进度总览

| 模块 | 内容 | 状态 |
| --- | --- | --- |
| ... | ... | ✅/🔄/⬜ |

## 最近动态（新→旧，最多 8 条）

- **YYYY-MM-DD**：一句话摘要（详见 dev-log 对应日期条目）。

## 当前焦点 / 待办

- [ ] ...

## 已知问题与坑位速查

| 编号/关键词 | 一句话 | 详见 |
| --- | --- | --- |
| BUG-NNN | ... | ../bugs/BUGFIX_LOG.md |

（≤10 行：只留高发与全局性坑位，按复发率/严重度精选，细节一律指向 `../bugs/` 对应条目。）

## 关键约定速查（高频引用，避免翻史料）

- ...

## 上下文恢复路线

1. 本文件 → 2. ../overview/architecture-snapshot.md → 3. 按任务选 design/qa → 4. 需要历史细节再进 history/ 对应阶段文件。
```
