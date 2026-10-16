package uk.gov.hmcts.reform.translate.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import uk.gov.hmcts.reform.translate.security.ErrorCode;

/** Controls dependency diagnostics emitted before or outside the MVC advice. */
@Slf4j
public class SafeDependencyDiagnosticsFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
                              Object[] arguments, Throwable exception) {
        String name = logger.getName();
        boolean boundaryLogsFailure = name.equals("org.hibernate.engine.jdbc.spi.SqlExceptionHelper")
            || name.startsWith("org.hibernate.orm.jdbc.batch")
            || name.startsWith("org.springframework.transaction.interceptor.")
            || name.equals("org.springframework.transaction.support.TransactionTemplate")
            || name.equals("org.springframework.web.servlet.PageNotFound")
            || name.startsWith("org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver")
            || name.equals("org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver");
        boolean dependency = boundaryLogsFailure || name.startsWith("org.hibernate.")
            || name.equals("org.springframework.boot.actuate.health.HealthEndpointSupport")
            || name.startsWith("com.zaxxer.hikari.")
            || name.startsWith("org.postgresql.")
            || name.startsWith("org.springframework.transaction.")
            || name.startsWith("org.springframework.orm.jpa.")
            || name.startsWith("org.apache.catalina.core.ContainerBase.")
            || name.startsWith("org.apache.coyote.");
        if (format == null || !level.isGreaterOrEqual(Level.WARN) || !dependency) {
            return FilterReply.NEUTRAL;
        }
        if (!level.isGreaterOrEqual(logger.getEffectiveLevel())) {
            return FilterReply.DENY;
        }
        // JDBC/resolver messages precede an owned boundary event; avoid duplicate failure summaries.
        if (!boundaryLogsFailure) {
            if (level == Level.ERROR) {
                log.error("Dependency failure: code={}", ErrorCode.INTERNAL_ERROR);
            } else {
                log.warn("Dependency warning: code={}", ErrorCode.INTERNAL_ERROR);
            }
        }
        FormattingTuple diagnostic = MessageFormatter.arrayFormat(format, arguments);
        log.debug("Original dependency diagnostic: logger={} message={}", name, diagnostic.getMessage(),
            exception != null ? exception : diagnostic.getThrowable());
        return FilterReply.DENY;
    }
}
