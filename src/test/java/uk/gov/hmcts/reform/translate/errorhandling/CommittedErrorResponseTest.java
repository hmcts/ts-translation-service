package uk.gov.hmcts.reform.translate.errorhandling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CommittedErrorResponseTest {

    @Test
    void committedFailureDoesNotPublishDiagnosticsOrChangeTheResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.getWriter().write("partial response");
        response.flushBuffer();
        Logger logger = (Logger) LoggerFactory.getLogger(RestExceptionHandler.class);
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Level previousLevel = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        try {
            logger.setLevel(Level.INFO);
            assertThat(new RestExceptionHandler().handleExceptionInternal(
                new RuntimeException("Synthetic committed diagnostic"), null, HttpHeaders.EMPTY,
                HttpStatus.INTERNAL_SERVER_ERROR, new ServletWebRequest(new MockHttpServletRequest(), response)))
                .isNull();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).isEqualTo("partial response");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                .isEqualTo("Request failed: code=INTERNAL_ERROR status=500");
            assertThat(appender.list.get(0).getThrowableProxy()).isNull();
        } finally {
            logger.setLevel(previousLevel);
            rootLogger.detachAppender(appender);
            appender.stop();
        }
    }
}
