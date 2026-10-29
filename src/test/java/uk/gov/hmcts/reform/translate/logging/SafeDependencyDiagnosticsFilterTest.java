package uk.gov.hmcts.reform.translate.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.reform.translate.errorhandling.RestExceptionHandler;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

class SafeDependencyDiagnosticsFilterTest {

    @Test
    void retainsDetailedFailuresAtNormalLevelsAndRespectsSourceThresholds() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        SafeDependencyDiagnosticsFilter filter = new SafeDependencyDiagnosticsFilter();

        filter.start();
        context.addTurboFilter(filter);
        Logger source = context.getLogger("org.postgresql.supportProbe");
        Logger output = context.getLogger(SafeDependencyDiagnosticsFilter.class);
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousSource = source.getLevel();
        Level previousOutput = output.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            source.setLevel(Level.WARN);
            for (Level configured : new Level[] {Level.INFO, Level.DEBUG}) {
                output.setLevel(configured);
                appender.list.clear();
                source.warn("PRIVATE diagnostic {}", "argument", new IllegalStateException("PRIVATE cause"));
                assertThat(appender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("PRIVATE diagnostic argument");
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("PRIVATE cause");
                });
                appender.list.clear();
                source.error("PRIVATE failure", new IllegalStateException("PRIVATE error cause"));
                assertThat(appender.list).singleElement().satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(event.getFormattedMessage()).contains("PRIVATE failure");
                    assertThat(event.getThrowableProxy().getMessage()).isEqualTo("PRIVATE error cause");
                });
                source.setLevel(Level.ERROR);
                appender.list.clear();
                source.warn("disabled warning");
                assertThat(appender.list).isEmpty();
                source.setLevel(Level.WARN);
            }
        } finally {
            source.setLevel(previousSource);
            output.setLevel(previousOutput);
            root.detachAppender(appender);
            appender.stop();
            context.getTurboFilterList().remove(filter);
            filter.stop();
        }
    }

    @Test
    void realMvcSqlFailuresRetainOneNormalBoundaryEvent() throws Exception {
        capture((appender, output) -> {
            var mvc = standaloneSetup(new FailingController())
                .setControllerAdvice(new RestExceptionHandler()).build();
            for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                output.setLevel(level);
                appender.list.clear();
                mvc.perform(get("/database-probe"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                    .andExpect(jsonPath("$.path").doesNotExist());
                assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .singleElement().satisfies(event -> {
                        assertThat(event.getFormattedMessage())
                            .isEqualTo("Request failed: code=INTERNAL_ERROR status=500");
                        assertThat(event.getThrowableProxy().getCause().getMessage()).isEqualTo("PRIVATE SQL cause");
                    });
            }
        });
    }

    @Test
    void rollbackFailureKeepsBothDistinctCausesWithoutDebug() throws Exception {
        capture((appender, output) -> {
            output.setLevel(Level.INFO);
            var manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            doThrow(new IllegalStateException("PRIVATE rollback cause"))
                .when(manager).rollback(any());
            Throwable failure = catchThrowable(() ->
                new TransactionTemplate(manager).execute(status -> {
                    throw new IllegalArgumentException("PRIVATE application cause");
                }));
            new RestExceptionHandler().handleUnexpectedException(new MockHttpServletRequest(), (Exception) failure);
            assertThat(appender.list).filteredOn(event -> event.getThrowableProxy() != null)
                .hasSize(2).extracting(event -> event.getThrowableProxy().getMessage())
                .containsExactly("PRIVATE application cause", "PRIVATE rollback cause");
        });
    }

    @Test
    void mvcInterceptorRollbackFailureKeepsBothCausesWithoutDebug() throws Exception {
        capture((appender, output) -> {
            output.setLevel(Level.INFO);
            var manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            doThrow(new IllegalStateException("PRIVATE rollback cause")).when(manager).rollback(any());
            var factory = new ProxyFactory((Runnable) () -> {
                throw new IllegalArgumentException("PRIVATE application cause");
            });
            factory.addAdvice(new TransactionInterceptor(manager, new MatchAlwaysTransactionAttributeSource()));
            var mvc = standaloneSetup(new RollbackController((Runnable) factory.getProxy()))
                .setControllerAdvice(new RestExceptionHandler()).build();
            appender.list.clear();
            mvc.perform(get("/rollback-probe"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal Server Error"));
            assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.ERROR
                && event.getThrowableProxy() != null)
                .extracting(event -> event.getThrowableProxy().getMessage())
                .containsExactly("PRIVATE application cause", "PRIVATE rollback cause");
        });
    }

    @Test
    void backgroundSqlDiagnosticsRemainAvailableWithoutAnMvcBoundary() throws Exception {
        capture((appender, output) -> {
            var previous = RequestContextHolder.getRequestAttributes();
            RequestContextHolder.resetRequestAttributes();
            try {
                output.setLevel(Level.INFO);
                new SqlExceptionHelper(false)
                    .logExceptions(new SQLException("PRIVATE background SQL cause", "23505"), "query failed");
                assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.ERROR)
                    .singleElement().satisfies(event -> assertThat(event.getFormattedMessage())
                        .contains("PRIVATE background SQL cause"));
            } finally {
                RequestContextHolder.setRequestAttributes(previous);
            }
        });
    }

    private void capture(LogCheck check) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger output = context.getLogger(SafeDependencyDiagnosticsFilter.class);
        Level previousLevel = output.getLevel();
        SafeDependencyDiagnosticsFilter filter = new SafeDependencyDiagnosticsFilter();
        filter.setContext(context);
        filter.start();
        context.addTurboFilter(filter);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        try {
            check.run(appender, output);
        } finally {
            root.detachAppender(appender);
            appender.stop();
            context.getTurboFilterList().remove(filter);
            filter.stop();
            output.setLevel(previousLevel);
        }
    }

    @FunctionalInterface
    private interface LogCheck {
        void run(ListAppender<ILoggingEvent> appender, Logger output) throws Exception;
    }

    @RestController
    static class RollbackController {
        private final Runnable service;

        RollbackController(Runnable service) {
            this.service = service;
        }

        @GetMapping("/rollback-probe")
        public void fail() {
            service.run();
        }
    }

    @RestController
    static class FailingController {
        @GetMapping("/database-probe")
        public String fail() {
            var cause = new SQLException("PRIVATE SQL cause", "23505");
            new SqlExceptionHelper(false).logExceptions(cause, "PRIVATE query");
            throw new IllegalStateException("database failure", cause);
        }
    }
}
