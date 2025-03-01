package uk.gov.hmcts.reform.translate.errorhandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.translate.TestIdamConfiguration;
import uk.gov.hmcts.reform.translate.config.SecurityConfiguration;
import uk.gov.hmcts.reform.translate.controllers.DictionaryController;
import uk.gov.hmcts.reform.translate.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.translate.security.filter.PutDictionaryEndpointFilter;
import uk.gov.hmcts.reform.translate.service.DictionaryService;

import java.lang.reflect.Method;

import static org.mockito.BDDMockito.given;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.translate.controllers.ControllerConstants.DICTIONARY_URL;
import static uk.gov.hmcts.reform.translate.errorhandling.BadRequestError.BAD_SCHEMA;

@WebMvcTest(controllers = DictionaryController.class,
    includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = MapperConfig.class),
    excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE,
        classes = {PutDictionaryEndpointFilter.class, SecurityConfiguration.class,
            JwtGrantedAuthoritiesConverter.class}))
@AutoConfigureMockMvc(addFilters = false)
@ImportAutoConfiguration(TestIdamConfiguration.class)
class RestExceptionHandlerTest {

    private static final String ERROR_PATH_ERROR = "$.error";
    private static final String ERROR_PATH_MESSAGE = "$.message";
    private static final String ERROR_PATH_STATUS = "$.status";

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected DictionaryService service;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("should return correct response when generic ApiException is thrown")
    @Test
    void shouldReturnGenericApiExceptionResponse() throws Exception {

        // GIVEN
        String myUniqueExceptionMessage = "Generic Api Exception";
        ApiException expectedException = new ApiException(myUniqueExceptionMessage);

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        assertHttpErrorResponse(result, HttpStatus.INTERNAL_SERVER_ERROR, expectedException.getMessage());
    }

    @DisplayName("should return correct response when RequestErrorException is thrown")
    @Test
    void shouldReturnRequestErrorExceptionResponse() throws Exception {

        // GIVEN
        RequestErrorException expectedException =
            new RequestErrorException("invalidRole");

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        result
            .andExpect(status().is(HttpStatus.FORBIDDEN.value()))
            .andExpect(jsonPath("$").doesNotExist());
    }

    @DisplayName("should return correct response, without details, when AuthorizationException (401 Unauthorized) "
        + "is thrown")
    @Test
    void shouldAuthorizationExceptionResponse() throws Exception {

        // GIVEN
        UnauthorizedException expectedException =
            new UnauthorizedException("you are not authorized");

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        result
            .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()))
            .andExpect(jsonPath("$").doesNotExist());
    }

    @DisplayName("should return correct response, without details, when RoleMissingException (403 Forbidden) is thrown")
    @Test
    void shouldReturnRoleMissingExceptionResponse() throws Exception {

        // GIVEN
        RoleMissingException expectedException =
            new RoleMissingException(String.format(RoleMissingException.ERROR_MESSAGE, "test-role"));

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        result
            .andExpect(status().is(HttpStatus.FORBIDDEN.value()))
            .andExpect(jsonPath("$").doesNotExist());
    }

    @DisplayName("should return correct response when EnglishPhraseUniqueConstraintException is thrown")
    @Test
    void shouldReturnEnglishPhraseUniqueConstraintExceptionResponse() throws Exception {

        EnglishPhraseUniqueConstraintException expectedException =
            new EnglishPhraseUniqueConstraintException("myUniqueExceptionMessage", null);

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        assertHttpErrorResponse(result, HttpStatus.CONFLICT, expectedException.getMessage());

    }

    @DisplayName("should return correct response when BadRequestException is thrown")
    @Test
    void shouldReturnBadRequestExceptionResponse() throws Exception {

        BadRequestException expectedException =
            new BadRequestException("myUniqueExceptionMessage");

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        assertHttpErrorResponse(result, HttpStatus.BAD_REQUEST, expectedException.getMessage());

    }

    @DisplayName("should return correct response when HttpMessageConversionException is thrown")
    @Test
    void shouldReturnHttpMessageConversionExceptionResponse() throws Exception {

        // GIVEN
        HttpMessageConversionException expectedException =
            new HttpMessageConversionException("Conversion problem");

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        assertHttpErrorResponse(result, HttpStatus.BAD_REQUEST, expectedException.getMessage());
    }

    @DisplayName("should return correct response when HttpMessageNotReadableException is thrown")
    @Test
    void shouldReturnHttpMessageNotReadableExceptionResponse() throws Exception {

        // GIVEN
        HttpMessageNotReadableException expectedException =
            new HttpMessageNotReadableException("Unreadable message",
                                                new MockClientHttpResponse("test".getBytes(), HttpStatus.OK));

        /// WHEN
        setupMockServiceToThrowException(expectedException);
        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        assertHttpErrorResponse(result, HttpStatus.BAD_REQUEST, BAD_SCHEMA);
    }

    @DisplayName("should return correct response when MethodArgumentNotValidException is thrown")
    @Test
    void shouldReturnMethodArgumentNotValidExceptionResponse() throws Exception {

        BindingResult bindingResult = new BeanPropertyBindingResult("", "objectName");

        Method[] methods = RestExceptionHandlerTest.class.getMethods();
        MethodParameter methodParameter = new MethodParameter(methods[0], 0);

        /// WHEN
        given(service.getDictionaryContents()).willAnswer(invocation -> {
            throw new MethodArgumentNotValidException(methodParameter, bindingResult);
        });

        ResultActions result = this.mockMvc.perform(get(DICTIONARY_URL)
                                                        .contentType(MediaType.APPLICATION_JSON));

        // THEN
        assertHttpErrorResponse(result, HttpStatus.BAD_REQUEST, BAD_SCHEMA);
    }

    private void setupMockServiceToThrowException(Exception expectedException) {
        // configure chosen mock service to throw exception when controller is run
        given(service.getDictionaryContents()).willThrow(expectedException);
    }

    private void assertHttpErrorResponse(ResultActions result,
                                         HttpStatus expectedStatus,
                                         String expectedMessage) throws Exception {
        result
            .andExpect(status().is(expectedStatus.value()))
            .andExpect(jsonPath(ERROR_PATH_STATUS).value(expectedStatus.value()))
            .andExpect(jsonPath(ERROR_PATH_ERROR).value(getReasonPhrase(expectedStatus)))
            .andExpect(jsonPath(ERROR_PATH_MESSAGE).value(expectedMessage));
    }

    private String getReasonPhrase(HttpStatus expectedStatus) {
        return expectedStatus.value() == HttpError.DEFAULT_STATUS
            ? HttpError.DEFAULT_ERROR
            : expectedStatus.getReasonPhrase();
    }

    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    private class UnauthorizedException extends ApiException {

        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
