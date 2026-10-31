package com.wilfredchau.synapsepkb;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilfredchau.synapsepkb.common.exception.BusinessException;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import com.wilfredchau.synapsepkb.security.AuthenticatedUser;
import com.wilfredchau.synapsepkb.tag.model.TagName;
import com.wilfredchau.synapsepkb.tag.model.vo.TagResponse;
import com.wilfredchau.synapsepkb.tag.service.TagService;
import com.wilfredchau.synapsepkb.tag.service.audit.TagAuditCustomizer;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class TagPostgresIT {
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("A2_TEST_DB_URL");
        if (!System.getProperty("os.name").equals("Linux")
                || !"jdbc:postgresql://127.0.0.1:5432/synapse_pkb_a2_test".equals(url)) {
            throw new IllegalStateException("PostgreSQL write tests require CentOS and the isolated localhost A2 database");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("DB_USERNAME"));
        registry.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "3000");
        registry.add("spring.flyway.target", () -> "latest");
        registry.add("app.security.bootstrap-user.enabled", () -> "false");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired TagService service;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager manager;
    @SpyBean TagAuditCustomizer customizer;
    long owner;
    long other;

    @BeforeEach
    void fixtures() {
        owner = createUser();
        other = createUser();
        jdbc.execute("CREATE TABLE IF NOT EXISTS a2_commit_probe (user_id BIGINT REFERENCES pkb_users(id) DEFERRABLE INITIALLY DEFERRED)");
    }

    @Test
    void httpLifecycleIncludesNormalizationConflictsNoOpAndRepeatMerge() throws Exception {
        mvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/categories").with(authentication(auth(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].code").value("tech"))
                .andExpect(jsonPath("$.data[4].code").value("note"));
        var created = mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name", " \u673a\u5668\u5b66\u4e60  ", "userId", other))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("\u673a\u5668\u5b66\u4e60"))
                .andExpect(jsonPath("$.data.userId").doesNotExist()).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).at("/data/id").asLong();
        assertThat(jdbc.queryForObject("SELECT user_id FROM tags WHERE id=?", Long.class, id)).isEqualTo(owner);
        mvc.perform(patch("/api/tags/{id}", id).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Java\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.changed").value(true));
        long auditBefore = auditCount();
        mvc.perform(patch("/api/tags/{id}", id).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" Java \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.changed").value(false));
        assertThat(auditCount()).isEqualTo(auditBefore);
        mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"java\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("TAG_NAME_CONFLICT"));
        TagResponse target = as(owner, () -> service.create("target"));
        mvc.perform(patch("/api/tags/{id}", target.id()).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"JAVA\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("TAG_NAME_CONFLICT"));
        mvc.perform(post("/api/tags/{id}/merge", id).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetTagId\":" + target.id() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.targetTag.id").value(target.id()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tags WHERE id=?", Long.class, id)).isZero();
        long afterMerge = auditCount();
        mvc.perform(post("/api/tags/{id}/merge", id).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetTagId\":" + target.id() + "}"))
                .andExpect(status().isNotFound());
        assertThat(auditCount()).isEqualTo(afterMerge);
        assertThat(as(owner, () -> service.create("Java")).id()).isNotEqualTo(id);
    }

    @Test
    void scopesListsRenamesAndBothMergeEndpointsToOwner() throws Exception {
        TagResponse mine = as(owner, () -> service.create("same"));
        TagResponse theirs = as(other, () -> service.create("same"));
        mvc.perform(get("/api/tags").with(authentication(auth(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(mine.id()));
        long before = auditCount();
        mvc.perform(patch("/api/tags/{id}", theirs.id()).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"stolen\"}"))
                .andExpect(status().isNotFound());
        for (long[] ids : new long[][]{{mine.id(), theirs.id()}, {theirs.id(), mine.id()}}) {
            mvc.perform(post("/api/tags/{id}/merge", ids[0]).with(authentication(auth(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"targetTagId\":" + ids[1] + "}"))
                    .andExpect(status().isNotFound());
        }
        assertThat(auditCount()).isEqualTo(before);
        assertThat(as(owner, service::list)).hasSize(1);
        assertThat(as(other, service::list)).hasSize(1);
    }

    @Test
    void validatesMalformedNamesIdsAndSelfMerge() throws Exception {
        for (String body : List.of("{}", "{\"name\":\"  \"}", "{\"name\":\"" + "x".repeat(33) + "\"}")) {
            mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{")).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/tags/not-a-number").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}")).andExpect(status().isBadRequest());
        TagResponse tag = as(owner, () -> service.create("self"));
        mvc.perform(post("/api/tags/{id}/merge", tag.id()).with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"targetTagId\":" + tag.id() + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/tags/0").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void databaseMatchesJavaNormalizationAndRejectsBypasses() {
        for (int cp : new int[]{9, 10, 11, 12, 13, 32, 0x85, 0xA0, 0x1680, 0x2000, 0x2001,
                0x2002, 0x2003, 0x2004, 0x2005, 0x2006, 0x2007, 0x2008, 0x2009, 0x200A,
                0x2028, 0x2029, 0x202F, 0x205F, 0x3000}) {
            String ws = Character.toString(cp);
            String input = ws + "Cafe\u0301" + ws + ws + "\u673a\u5668" + ws;
            assertThat(jdbc.queryForObject("SELECT pkb_tag_normalize_v1(?)", String.class, input))
                    .isEqualTo(TagName.normalize(input));
        }
        for (String invalid : new String[]{"", " x ", "a  b", "Cafe\u0301", "\u200b", "\ufeff", "\u0001", "\u007f", "x".repeat(33)}) {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO tags(user_id,name) VALUES (?,?)", owner, invalid))
                    .isInstanceOf(DataAccessException.class);
        }
        String extended = new String(Character.toChars(0x20000)).repeat(32);
        assertThat(as(owner, () -> service.create(extended)).name()).isEqualTo(extended);
        as(owner, () -> service.create("Java"));
        assertThatThrownBy(() -> jdbc.update("INSERT INTO tags(user_id,name) VALUES (?, 'java')", owner))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO tags(user_id,name) VALUES (-1,'orphan')"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rollbackAndRealDeferredCommitFailureLeaveNoAudit() {
        as(owner, () -> {
            new TransactionTemplate(manager).executeWithoutResult(status -> {
                service.create("rollback");
                assertThat(auditCount()).isZero();
                status.setRollbackOnly();
            });
            assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(status -> {
                service.create("commit-failure");
                jdbc.update("INSERT INTO a2_commit_probe(user_id) VALUES (-1)");
            })).isInstanceOf(RuntimeException.class);
            return null;
        });
        assertThat(as(owner, service::list)).isEmpty();
        assertThat(auditCount()).isZero();
    }

    @Test
    void postgresAuditInsertFailureAndCustomizerFailureDoNotBreakHttpSuccess() throws Exception {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            ((AuditLogDraft) invocation.getArgument(0)).setAction("X".repeat(1000));
            return null;
        }).when(customizer).customize(any(), any());
        mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"audit-failure\"}"))
                .andExpect(status().isOk());
        assertThat(as(owner, service::list)).hasSize(1);
        assertThat(auditCount()).isZero();
        doAnswer(invocation -> { throw new IllegalStateException("customizer failed"); })
                .when(customizer).customize(any(), any());
        mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"customizer-failure\"}"))
                .andExpect(status().isOk());
        assertThat(as(owner, service::list)).hasSize(2);
        assertThat(auditCount()).isZero();
    }

    @Test
    void concurrentSameNameAndReverseMergesHaveOneWinner() throws Exception {
        assertThat(race(() -> service.create("concurrent"), () -> service.create("CONCURRENT")))
                .containsExactlyInAnyOrder(true, false);
        assertThat(as(owner, service::list)).hasSize(1);
        assertThat(auditCount()).isEqualTo(1);
        TagResponse a = as(owner, () -> service.create("a"));
        TagResponse b = as(owner, () -> service.create("b"));
        assertThat(race(() -> service.merge(a.id(), b.id()), () -> service.merge(b.id(), a.id())))
                .containsExactlyInAnyOrder(true, false);
        assertThat(as(owner, service::list)).hasSize(2);
        assertThat(auditCount()).isEqualTo(4);
    }

    @Test
    void caseOnlyRenameIsARealChange() {
        TagResponse tag = as(owner, () -> service.create("Java"));
        assertThat(as(owner, () -> service.rename(tag.id(), "JAVA")).changed()).isTrue();
        assertThat(auditCount()).isEqualTo(2);
    }

    @Test
    void lockedTagTimesOutWithConflictInsteadOfHanging() throws Exception {
        TagResponse tag = as(owner, () -> service.create("locked"));
        var pool = Executors.newSingleThreadExecutor();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var holder = pool.submit(() -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT id FROM tags WHERE user_id=? AND id=? FOR UPDATE",
                    Long.class, owner, tag.id());
            locked.countDown();
            try { release.await(15, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }));
        try {
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            mvc.perform(patch("/api/tags/{id}", tag.id()).with(authentication(auth(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"blocked\"}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("TAG_OPERATION_CONFLICT"));
            assertThat(auditCount()).isEqualTo(1);
        } finally {
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
        assertThat(as(owner, service::list).get(0).name()).isEqualTo("locked");
    }

    @Test
    void blockedAuditWriterTimesOutWithoutFailingCommittedBusiness() throws Exception {
        var pool = Executors.newSingleThreadExecutor();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var holder = pool.submit(() -> new TransactionTemplate(manager).executeWithoutResult(status -> {
            jdbc.execute("LOCK TABLE operation_logs IN ACCESS EXCLUSIVE MODE");
            locked.countDown();
            try { release.await(15, TimeUnit.SECONDS); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
        }));
        long started = System.nanoTime();
        try {
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/tags").with(authentication(auth(owner)))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"audit-timeout\"}"))
                    .andExpect(status().isOk());
            assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(12);
        } finally {
            release.countDown();
            holder.get(10, TimeUnit.SECONDS);
            pool.shutdownNow();
        }
        assertThat(as(owner, service::list)).hasSize(1);
        assertThat(auditCount()).isZero();
    }

    private List<Boolean> race(Supplier<?> first, Supplier<?> second) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try {
            var tasks = List.of(first, second).stream().map(action -> (Callable<Boolean>) () -> {
                ready.countDown();
                if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timed out");
                try { as(owner, action); return true; }
                catch (BusinessException | DataAccessException expected) { return false; }
            }).toList();
            var one = pool.submit(tasks.get(0));
            var two = pool.submit(tasks.get(1));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private long createUser() {
        String name = "a2-it-" + UUID.randomUUID().toString().substring(0, 12);
        return jdbc.queryForObject("INSERT INTO pkb_users(username,password_hash,display_name,space_key) VALUES (?,?,?,?) RETURNING id",
                Long.class, name, encoder.encode(UUID.randomUUID().toString()), name, name);
    }

    private UsernamePasswordAuthenticationToken auth(long id) {
        return UsernamePasswordAuthenticationToken.authenticated(new AuthenticatedUser(id, "a2-it", "A2", "a2"), null, List.of());
    }

    private <T> T as(long id, Supplier<T> action) {
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth(id));
        SecurityContextHolder.setContext(context);
        try { return action.get(); }
        finally { SecurityContextHolder.setContext(previous); }
    }

    private long auditCount() {
        return jdbc.queryForObject("SELECT count(*) FROM operation_logs WHERE actor_user_id=?", Long.class, owner);
    }
}
