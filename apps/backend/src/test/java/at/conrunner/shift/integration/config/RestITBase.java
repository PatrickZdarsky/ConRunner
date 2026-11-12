package at.conrunner.shift.integration.config;

import at.conrunner.communications.api.NotificationRequests;
import at.conrunner.platform.exception.IllegalArgumentException;
import at.conrunner.identity.User;
import at.conrunner.identity.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import at.conrunner.shift.config.TestSecurityConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@Import(TestSecurityConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.liquibase.change-log=classpath:db/changelog/db.changelog-local.yaml",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "oidc.provider.issuer-uri=https://identity.example.test/realms/shiftcontrol",
    "oidc.provider.jwk-set-uri=https://identity.example.test/jwks",
    "oidc.provider.audience=shiftcontrol-backend"
})
@ActiveProfiles("test")
@Tag("integration")
public abstract class RestITBase {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate cleanupJdbc;

    @org.junit.jupiter.api.AfterEach
    void clearDatabase() {
        var tables = cleanupJdbc.queryForList("select tablename from pg_tables where schemaname = 'public' and tablename not in ('databasechangelog', 'databasechangeloglock')", String.class);
        if (!tables.isEmpty()) {
            String names = tables.stream().map(name -> "\"" + name.replace("\"", "\"\"") + "\"").collect(java.util.stream.Collectors.joining(", "));
            cleanupJdbc.execute("TRUNCATE TABLE " + names + " RESTART IDENTITY CASCADE");
        }
    }

    @LocalServerPort
    protected int port;

    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @MockitoBean
    NotificationRequests notificationRequests;

    @MockitoBean
    protected UserService userDirectoryService;

    protected static ObjectMapper objectMapper = new ObjectMapper();

    protected RestAssuredConfig config;

    private static PostgreSQLContainer<?> postgres;

    protected static final String HDR_TEST_USER_TYPE = "X-Test-UserType";
    protected static final String HDR_TEST_USER_ID = "X-Test-UserId";
    protected static final String HDR_TEST_USERNAME = "X-Test-Username";

    @BeforeAll
    static void init() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.enable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    protected Map<String, String> asAdminHeaders() {
        return Map.of(
            HDR_TEST_USER_TYPE, "ADMIN",
            HDR_TEST_USER_ID, "00000000-0000-0000-0000-000000000001",
            HDR_TEST_USERNAME, "admin"
        );
    }

    protected Map<String, String> asAssignedHeaders(Object userId) {
        return Map.of(
            HDR_TEST_USER_TYPE, "ASSIGNED",
            HDR_TEST_USER_ID, userId.toString(),
            HDR_TEST_USERNAME, "assigned-" + userId
        );
    }

    @DynamicPropertySource
    static void dbProps(DynamicPropertyRegistry r) {
        // Always use a dedicated database: cleanup truncates every table between tests.
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

        postgres.start();
        ensureClobDomain(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    private static void ensureClobDomain(String url, String user, String password) {
        try (var connection = java.sql.DriverManager.getConnection(url, user, password);
             var statement = connection.createStatement()) {
            statement.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'clob') THEN CREATE DOMAIN clob AS text; END IF; END $$");
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Unable to initialize test database", e);
        }
    }

    @BeforeEach
    void setUpRestClient() {
        setBasePath();
        setThreadNameToTest();
        setupUsers();
    }

    private void setBasePath() {
        if (RestAssured.requestSpecification == null) {
            RestAssured.requestSpecification = (new RequestSpecBuilder())
                .setPort(this.port)
                .setAccept(this.getRestAssuredAcceptContentType())
                .setContentType(this.getRestAssuredContentType())
                .build();
            this.config = RestAssured.config.encoderConfig(EncoderConfig.encoderConfig().defaultContentCharset(this.defaultCharset()));
        }
        RestAssured.requestSpecification.basePath(this.getBasePath());
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private void setThreadNameToTest() {
        Thread thread = Thread.currentThread();
        thread.setName(thread.threadId() + "-" + getClass().getSimpleName());
    }

    private void setupUsers() {
        Mockito.when(userDirectoryService.getUser(any())).thenAnswer(invocation -> user(invocation.getArgument(0)));
        Mockito.when(userDirectoryService.getUsers(any())).thenAnswer(invocation -> {
            java.util.Collection<java.util.UUID> ids = invocation.getArgument(0);
            return ids.stream().map(RestITBase::user).toList();
        });
        Mockito.when(userDirectoryService.getPlatformAdmins()).thenReturn(List.of());
    }

    private static User user(java.util.UUID id) {
        return User.builder().id(id).username("assigned-" + id).email(id + "@example.com").avatarUrl("https://cdn.example.test/profiles/" + id + ".png").build();
    }

    public String getBasePath() {
        return "/api/v1/";
    }

    protected int getPort() {
        return this.port;
    }

    protected ContentType getRestAssuredContentType() {
        return ContentType.JSON;
    }

    protected ContentType getRestAssuredAcceptContentType() {
        return ContentType.ANY;
    }

    protected String defaultCharset() {
        return "UTF-8";
    }

    public <T> T getRequestAsAssigned(final String uri, final Class<T> expectedObject, Object userId) {
        return this.doRequest(Method.GET, uri, "", new HashMap<>(), asAssignedHeaders(userId), 200, expectedObject);
    }

    public <T> T getRequest(final String uri, final Map<String, String> params, final Class<T> expectedObject) {
        return this.doRequest(Method.GET, uri, "", params, 200, expectedObject);
    }

    public <T> T getRequestAsAdmin(String uri, Class<T> expected) {
        return doRequest(Method.GET, uri, "", new HashMap<>(), asAdminHeaders(), 200, expected);
    }

    public <T> List<T> getRequestListAsAssigned(final String uri, final Class<T> expected, Object userId) {
        return this.doRequestList(Method.GET, uri, "", new HashMap<>(), asAssignedHeaders(userId), 200, expected);
    }

    public <T> List<T> getRequestList(final String uri, final Class<T> expectedObject) {
        return this.doRequestList(Method.GET, uri, "", 200, expectedObject);
    }

    public <T> List<T> getRequestList(final String uri, final Map<String, String> params, final Class<T> expectedObject) {
        return this.doRequestList(Method.GET, uri, "", params, 200, expectedObject);
    }

    public <T> T putRequestAsAssigned(final String uri, final Object body, final Class<T> expectedObject, Object userId) {
        return doRequest(Method.PUT, uri, body, new HashMap<>(), asAssignedHeaders(userId), 200, expectedObject);
    }

    public <T> T putRequestAsAdmin(String uri, Object body, Class<T> expected) {
        return doRequest(Method.PUT, uri, body, new HashMap<>(), asAdminHeaders(), 200, expected);
    }

    public <T> T patchRequestAsAssigned(final String uri, final Object body, final Class<T> expectedObject, Object userId) {
        return doRequest(Method.PATCH, uri, body, new HashMap<>(), asAssignedHeaders(userId), 200, expectedObject);
    }

    public <T> T postRequestAsAssigned(final String uri, final Object body, final Class<T> expectedObject, Object userId) {
        return doRequest(Method.POST, uri, body, new HashMap<>(), asAssignedHeaders(userId), 200, expectedObject);
    }

    public <T> T deleteRequestAsAssigned(final String uri, final Object body, final Class<T> expectedObject, Object userId) {
        return doRequest(Method.DELETE, uri, body, new HashMap<>(), asAssignedHeaders(userId), 200, expectedObject);
    }

    public <T> T postRequestAsAdmin(String uri, Object body, Class<T> expected) {
        return doRequest(Method.POST, uri, body, new HashMap<>(), asAdminHeaders(), 200, expected);
    }

    public <T> T postRequestAsAdmin(String uri, Object body, Class<T> expected, int expectedStatusCode) {
        return doRequest(Method.POST, uri, body, new HashMap<>(), asAdminHeaders(), expectedStatusCode, expected);
    }

    public <T> T deleteRequest(final String uri) {
        return doRequest(Method.DELETE, uri, "", 200, null);
    }

    public <T> T deleteRequestAsAdmin(final String uri) {
        return doRequest(Method.DELETE, uri, "", new HashMap<>(), asAdminHeaders(), 200, null);
    }

    public <T> T deleteRequestAsAdmin(final String uri, final Class<T> expectedObject) {
        return doRequest(Method.DELETE, uri, "", new HashMap<>(), asAdminHeaders(), 200, expectedObject);
    }

    public <T> T deleteRequest(final String uri, final Class<T> expectedObject) {
        return doRequest(Method.DELETE, uri, "", 200, expectedObject);
    }

    public <T> T doRequest(final Method method, final String url, final Object body, final int expectedStatusCode, final Class<T> expectedObject) {
        return doRequest(method, url, body, new HashMap<>(), expectedStatusCode, expectedObject);
    }

    public <T> List<T> doRequestList(final Method method, final String url, final Object body, final int expectedStatusCode, final Class<T> elementType) {
        return doRequestList(method, url, body, new HashMap<>(), expectedStatusCode, elementType);
    }

    public <T> T doRequest(final Method method, final String url, final Object body, final Map<String, String> params, final int expectedStatusCode,
                           final Class<T> expectedObject) {
        if (expectedObject == null) {
            RestAssured.given()
                .body(body)
                .params(params)
                .request(method, url)
                .then()
                .statusCode(expectedStatusCode);
            return null; // Return null for void methods
        } else if (Iterable.class.isAssignableFrom(expectedObject)) {
            throw new IllegalArgumentException("Use doRequestList for Iterable types");
        }

        return RestAssured.given()
            .body(body)
            .params(params)
            .request(method, url)
            .then()
            .statusCode(expectedStatusCode)
            .extract()
            .body()
            .as(expectedObject);
    }

    public <T> List<T> doRequestList(final Method method, final String url, final Object body,
                                     final Map<String, String> params, final int expectedStatusCode, final Class<T> elementType) {
        return RestAssured.given()
            .body(body)
            .params(params)
            .request(method, url)
            .then()
            .statusCode(expectedStatusCode)
            .extract()
            .body()
            .jsonPath()
            .getList(".", elementType); // Explicitly convert LinkedHashMap to T
    }

    public <T> List<T> doRequestList(final Method method, final String url, final Object body,
                                     final Map<String, String> params, final Map<String, String> headers, final int expectedStatusCode, final Class<T> elementType) {
        return RestAssured.given()
            .body(body)
            .params(params)
            .headers(headers)
            .request(method, url)
            .then()
            .statusCode(expectedStatusCode)
            .extract()
            .body()
            .jsonPath()
            .getList(".", elementType); // Explicitly convert LinkedHashMap to T
    }

    public <T> T doRequest(Method method, String url, Object body, Map<String, String> params, Map<String, String> headers, int expectedStatusCode,
                           Class<T> expectedObject
    ) {
        var req = RestAssured.given()
            .headers(headers)
            .body(body)
            .params(params);

        if (expectedObject == null) {
            req.request(method, url).then().statusCode(expectedStatusCode);
            return null;
        }

        return req.request(method, url)
            .then()
            .statusCode(expectedStatusCode)
            .extract()
            .body()
            .as(expectedObject);
    }

    public void doRequestWithoutResponseContent(final Method method, final String url, final Object body) {
        RestAssured.given()
            .body(body)
            .request(method, url, new Object[0])
            .then()
            .statusCode(204);
    }

    public byte[] doFileRequestWithoutResponseContent(final Method method, final String url, final Map<String, String> params, final Object body) {
        return RestAssured.given()
            .body(body)
            .params(params)
            .request(method, url, new Object[0])
            .then()
            .statusCode(200)
            .contentType(ContentType.BINARY)
            .extract()
            .asByteArray();
    }

    // note: if more advanced assertions are needed, a custom matcher should be created (instead of just checking the detail like here)
    public void doRequestAndAssertAndAssertStausCode(final Method method, final String url, final Object body, final int expectedStatusCode) {
        RestAssured.given()
            .body(body)
            .request(method, url, new Object[0])
            .then()
            .statusCode(expectedStatusCode);
    }

    public void doRequestAsAssignedAndAssertMessage(final Method method, final String url, final Object body, final int expectedStatusCode,
                                                    final String message, Object userId) {
        doRequestAsAssignedAndAssertMessage(method, url, body, expectedStatusCode, message, true, userId);
    }

    public void doRequestAsAssignedAndAssertMessage(final Method method, final String url, final Object body, final int expectedStatusCode,
                                                    final String message,
                                                    final boolean matchDetailExactly, Object userId) {
        doRequestAsAssignedAndAssertMessage(method, url, body, new HashMap<>(), expectedStatusCode, message, matchDetailExactly, userId);
    }

    public void doRequestAsAssignedAndAssertMessage(final Method method, final String url, final Object body, final Map<String, String> params,
                                                    final int expectedStatusCode, final String message, Object userId) {
        doRequestAsAssignedAndAssertMessage(method, url, body, params, expectedStatusCode, message, true, userId);
    }

    public void doRequestAsAdminAndAssertMessage(final Method method, final String url, final Object body,
                                                 final int expectedStatusCode, final String message, final boolean matchDetailExactly) {
        doRequestAsAdminAndAssertMessage(method, url, body, new HashMap<>(), expectedStatusCode, message, matchDetailExactly);
    }

    public void doRequestAsAdminAndAssertMessage(final Method method, final String url, final Object body, final Map<String, String> params,
                                                 final int expectedStatusCode, final String message, final boolean matchDetailExactly) {
        String actual = RestAssured.given()
            .headers(asAdminHeaders())
            .body(body)
            .params(params)
            .request(method, url, new Object[0])
            .then()
            .statusCode(expectedStatusCode)
            .extract()
            .asString();

        if (matchDetailExactly) {
            assertThat(extractMessageFromErrorJson(actual)).isEqualTo(message);
        } else {
            assertThat(actual).contains(message);
        }
    }

    public void doRequestAsAssignedAndAssertMessage(final Method method, final String url, final Object body, final Map<String, String> params,
                                                    final int expectedStatusCode, final String message, final boolean matchDetailExactly, Object userId) {
        String actual = RestAssured.given()
            .headers(asAssignedHeaders(userId))
            .body(body)
            .params(params)
            .request(method, url, new Object[0])
            .then()
            .statusCode(expectedStatusCode)
            .extract()
            .asString();

        if (matchDetailExactly) {
            assertThat(extractMessageFromErrorJson(actual)).isEqualTo(message);
        } else {
            assertThat(actual).contains(message);
        }
    }

    private String extractMessageFromErrorJson(String json) {
        try {
            JsonNode root = new ObjectMapper().readTree(json);
            return root.has("message") ? root.get("message").asText() : json;
        } catch (Exception _) {
            // fallback if response is not JSON
            return json;
        }
    }
}
