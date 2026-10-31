package com.wilfredchau.synapsepkb;

import static org.assertj.core.api.Assertions.*;

import com.wilfredchau.synapsepkb.operationlog.annotation.AuditOperation;
import com.wilfredchau.synapsepkb.operationlog.model.AuditInvocationContext;
import com.wilfredchau.synapsepkb.operationlog.model.AuditLogDraft;
import com.wilfredchau.synapsepkb.operationlog.service.AuditOperationCustomizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:audit_tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@Import(AuditTransactionTests.Fixtures.class)
class AuditTransactionTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ProbeService probe;
    @Autowired ProbeCustomizer customizer;

    @BeforeEach
    void reset() {
        customizer.failPreparation = false;
        customizer.failWrite = false;
        customizer.activeTransactionSeen = false;
        jdbc.update("DELETE FROM operation_logs");
    }

    @Test
    void advisorRunsInsideTransactionAndWaitsForOuterCommit() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            probe.change("committed", false);
            assertThat(count()).isZero();
            assertThat(customizer.activeTransactionSeen).isTrue();
        });
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void outerRollbackLeavesNoSuccessEventOrBusinessChange() {
        String before = name();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            probe.change("rolled-back", false);
            status.setRollbackOnly();
        });
        assertThat(name()).isEqualTo(before);
        assertThat(count()).isZero();
    }

    @Test
    void businessExceptionLeavesNoSuccessEvent() {
        String before = name();
        assertThatThrownBy(() -> probe.change("failed", true)).isInstanceOf(IllegalStateException.class);
        assertThat(name()).isEqualTo(before);
        assertThat(count()).isZero();
    }

    @Test
    void realAuditInsertFailureDoesNotUndoBusinessOrEscapeAfterCommit() {
        customizer.failWrite = true;
        assertThatCode(() -> probe.change("audit-write-failed", false)).doesNotThrowAnyException();
        assertThat(name()).isEqualTo("audit-write-failed");
        assertThat(count()).isZero();
    }

    @Test
    void customizerFailureDoesNotUndoBusiness() {
        customizer.failPreparation = true;
        assertThatCode(() -> probe.change("customizer-failed", false)).doesNotThrowAnyException();
        assertThat(name()).isEqualTo("customizer-failed");
        assertThat(count()).isZero();
    }

    @Test
    void auditSnapshotCannotBeChangedAfterRegistration() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            probe.change("snapshot", false);
            customizer.lastDraft.setAction("MUTATED");
        });
        assertThat(jdbc.queryForObject("SELECT action FROM operation_logs", String.class)).isEqualTo("TX_PROBE");
    }

    long count() { return jdbc.queryForObject("SELECT count(*) FROM operation_logs", Long.class); }
    String name() { return jdbc.queryForObject("SELECT display_name FROM pkb_users WHERE username='pkb-admin'", String.class); }

    @TestConfiguration
    static class Fixtures {
        @Bean ProbeService probeService(JdbcTemplate jdbc) { return new ProbeService(jdbc); }
        @Bean ProbeCustomizer probeCustomizer() { return new ProbeCustomizer(); }
    }

    static class ProbeService {
        private final JdbcTemplate jdbc;
        ProbeService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

        @Transactional
        @AuditOperation(action = "TX_PROBE", targetType = "TEST", customizer = ProbeCustomizer.class)
        public void change(String name, boolean fail) {
            jdbc.update("UPDATE pkb_users SET display_name=? WHERE username='pkb-admin'", name);
            if (fail) throw new IllegalStateException("business failed");
        }
    }

    static class ProbeCustomizer implements AuditOperationCustomizer {
        boolean failPreparation;
        boolean failWrite;
        boolean activeTransactionSeen;
        AuditLogDraft lastDraft;

        public void customize(AuditLogDraft draft, AuditInvocationContext context) {
            activeTransactionSeen = TransactionSynchronizationManager.isActualTransactionActive();
            if (failPreparation) throw new IllegalStateException("customizer failed");
            if (failWrite) draft.setAction("X".repeat(1000));
            lastDraft = draft;
        }
    }
}
