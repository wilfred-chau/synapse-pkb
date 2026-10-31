# SPKB-9 交付与提交清单

本轮交付 A2 非 AI 管理闭环；验证结果见 [A2 验证记录](a2-verification.md)，范围与后续依赖见 [正式设计](../design/a2-category-tag-design.md)。未代执行暂存、提交或推送；Jira 状态由用户流转。

## Commit Message 草案

```text
[SPKB-9] feat(tags): add user-scoped category and tag management

- Add fixed categories and normalized tags with transactional rename and merge.
- Write success audits after commit and keep credentials out of debug logs.
- Separate test credentials and redact previously documented defaults.
- Verify Java 17 tests, PostgreSQL constraints and browser management flows.
- Keep AI recommendations and entry associations explicitly deferred.
```

## 精准 Git Add 清单

在仓库根目录使用。每条路径均为本轮相关文件，不包含整个 backend/frontend 目录；执行前再次核对自己的后续改动。

```powershell
git add -- `
  .env.example `
  backend/pom.xml `
  backend/src/main/java/com/wilfredchau/synapsepkb/SynapsePkbApplication.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/common/exception/GlobalExceptionHandler.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/operationlog/aspect/AuditOperationAspect.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/operationlog/model/AuditLogDraft.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/operationlog/service/impl/OperationLogServiceImpl.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/user/UserBootstrapper.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/security/CurrentUser.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/category/controller/CategoryController.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/category/model/Category.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/category/model/vo/CategoryResponse.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/controller/TagController.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/controller/TagExceptionHandler.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/entity/Tag.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/mapper/TagMapper.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/TagErrorCode.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/TagName.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/dto/MergeTagRequest.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/dto/TagNameRequest.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/vo/MergeTagResponse.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/vo/RenameTagResponse.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/model/vo/TagResponse.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/service/TagService.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/service/audit/TagAuditCustomizer.java `
  backend/src/main/java/com/wilfredchau/synapsepkb/tag/service/impl/TagServiceImpl.java `
  backend/src/main/resources/application.yml `
  backend/src/main/resources/db/migration/V3__create_tags.sql `
  backend/src/main/resources/db/seed/G3__seed_tags.sql `
  backend/src/main/resources/mapper/tag/TagMapper.xml `
  backend/src/test/resources/application.yml `
  backend/src/test/java/com/wilfredchau/synapsepkb/AuditTransactionTests.java `
  backend/src/test/java/com/wilfredchau/synapsepkb/SynapsePkbApplicationTests.java `
  backend/src/test/java/com/wilfredchau/synapsepkb/TagNameTests.java `
  backend/src/test/java/com/wilfredchau/synapsepkb/TagPostgresIT.java `
  frontend/package.json `
  frontend/package-lock.json `
  frontend/playwright.config.ts `
  frontend/e2e/tags.spec.ts `
  frontend/src/App.tsx `
  frontend/src/api.ts `
  frontend/src/TagManager.tsx `
  frontend/vite.config.ts `
  docs/README.md `
  docs/.doc-pilot/tasks/TASK-20261101-A2-TAGS.json `
  docs/design/a2-category-tag-design.md `
  docs/overview/architecture-snapshot.md `
  docs/progress/STATUS.md `
  docs/progress/dev-log.md `
  docs/bugs/BUGFIX_LOG.md `
  docs/deploy/DEPLOYMENT.md `
  docs/qa/a2-verification.md `
  docs/qa/a2-delivery.md
```

不暂存 `.env.local`、`docs/plans-local/`、`backend/target/`、`frontend/dist/`、node_modules、服务器备份或测试数据库数据。运行态任务 JSON 此前已由用户纳入 Git，本次仅更新同一文件，不递归暂存其他任务。
