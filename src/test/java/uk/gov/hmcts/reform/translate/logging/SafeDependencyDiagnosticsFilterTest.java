package uk.gov.hmcts.reform.translate.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.reform.translate.errorhandling.RestExceptionHandler;
import uk.gov.hmcts.reform.translate.controllers.RootController;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SafeDependencyDiagnosticsFilterTest {

    @Test
    void realSqlErrorsAreDebugOnlyWithOneBoundarySummary() throws Exception {
        capture((context, appender, output) -> {
            SQLException cause = new SQLException("SYNTHETIC_PRIVATE_PHRASE", "23505");
            for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                output.setLevel(level);
                appender.list.clear();
                new SqlExceptionHelper(false).logExceptions(cause, "SYNTHETIC_PRIVATE_QUERY");
                new RestExceptionHandler().handleUnexpectedException(new MockHttpServletRequest(),
                    new IllegalStateException("database failure", cause));
                assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                    .singleElement().satisfies(event -> {
                        assertThat(event.getFormattedMessage())
                            .isEqualTo("Request failed: code=INTERNAL_ERROR status=500");
                        assertThat(event.getThrowableProxy()).isNull();
                    });
                if (level == Level.DEBUG) {
                    assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.DEBUG)
                        .anySatisfy(event -> assertThat(event.getFormattedMessage())
                            .contains("SYNTHETIC_PRIVATE_PHRASE"));
                }
            }
        });
    }

    @Test
    void poolAndContainerDiagnosticsRetainSeverityAndCausesOnlyAtDebug() throws Exception {
        capture((context, appender, output) -> {
            for (String name : List.of("com.zaxxer.hikari.pool.PoolBase",
                "org.postgresql.ssl.PGjdbcHostnameVerifier",
                "org.springframework.orm.jpa.JpaTransactionManager",
                "org.springframework.transaction.support.AbstractPlatformTransactionManager",
                "org.springframework.boot.actuate.health.HealthEndpointSupport",
                "org.apache.catalina.core.ContainerBase.[Tomcat].[localhost].[/].[dispatcherServlet]")) {
                Logger dependency = context.getLogger(name);
                Level previousLevel = dependency.getLevel();
                try {
                    dependency.setLevel(Level.WARN);
                    for (Level level : List.of(Level.INFO, Level.DEBUG)) {
                        output.setLevel(level);
                        appender.list.clear();
                        dependency.warn("SYNTHETIC_PRIVATE_CONNECTION {}", "private-id",
                            new IllegalStateException("SYNTHETIC_PRIVATE_CAUSE"));
                        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                            .singleElement().satisfies(event -> {
                                assertThat(event.getFormattedMessage())
                                    .isEqualTo("Dependency warning: code=INTERNAL_ERROR");
                                assertThat(event.getThrowableProxy()).isNull();
                            });
                        assertThat(appender.list).filteredOn(event -> event.getThrowableProxy() != null)
                            .hasSize(level == Level.DEBUG ? 1 : 0);
                    }
                    appender.list.clear();
                    dependency.setLevel(Level.OFF);
                    dependency.error("SYNTHETIC_PRIVATE_FAILURE");
                    assertThat(appender.list).isEmpty();
                } finally {
                    dependency.setLevel(previousLevel);
                }
            }
        });
    }

    @Test
    void unknownUrlsDoNotLeakThroughDispatcherServletWarningsOrResponses() throws Exception {
        capture((context, appender, output) -> {
            output.setLevel(Level.INFO);
            var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new RootController()).setControllerAdvice(new RestExceptionHandler()).build();
            appender.list.clear();
            mvc.perform(get("/SYNTHETIC_PRIVATE_IDENTIFIER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").doesNotExist());
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .singleElement().satisfies(event -> {
                    assertThat(event.getFormattedMessage())
                        .isEqualTo("Request failed: code=RESOURCE_NOT_FOUND status=404");
                    assertThat(event.getThrowableProxy()).isNull();
                });
        });
    }

    @Test
    void rollbackFailuresKeepTheOverriddenApplicationCauseOnlyAtDebug() throws Exception {
        capture((context, appender, output) -> {
            PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
            doThrow(new IllegalStateException("SYNTHETIC_PRIVATE_ROLLBACK")).when(manager).rollback(any());
            output.setLevel(Level.DEBUG);
            appender.list.clear();
            Throwable failure = catchThrowable(() -> new TransactionTemplate(manager).execute(status -> {
                throw new IllegalArgumentException("SYNTHETIC_PRIVATE_APPLICATION_CAUSE");
            }));
            assertThat(failure).isInstanceOf(IllegalStateException.class);
            new RestExceptionHandler().handleUnexpectedException(new MockHttpServletRequest(), (Exception) failure);
            assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .singleElement().satisfies(event -> {
                    assertThat(event.getFormattedMessage())
                        .isEqualTo("Request failed: code=INTERNAL_ERROR status=500");
                    assertThat(event.getThrowableProxy()).isNull();
                });
            assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.DEBUG)
                .anySatisfy(event -> assertThat(event.getThrowableProxy().getMessage())
                    .isEqualTo("SYNTHETIC_PRIVATE_APPLICATION_CAUSE"));
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
            check.run(context, appender, output);
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
        void run(LoggerContext context, ListAppender<ILoggingEvent> appender, Logger output) throws Exception;
    }
}
