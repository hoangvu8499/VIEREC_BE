package com.vierec.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.auth.dto.LoginRequest;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import javax.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginApiTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String ACCESS = "access_token";
    private static final String REFRESH = "refresh_token";
    private static final int THIRTY_MINUTES = 30 * 60;
    private static final int THIRTY_DAYS = 30 * 24 * 60 * 60;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void registerUser() throws Exception {
        if (!roleRepository.findByCode(RoleCode.TRAINEE).isPresent()) {
            Role trainee = new Role();
            trainee.setCode(RoleCode.TRAINEE);
            trainee.setName("Trainee");
            roleRepository.save(trainee);
        }
        RegisterRequest request = RegisterRequest.builder()
                .username("nguyenvana").password(PASSWORD).firstName("Văn A").lastName("Nguyễn")
                .cccd("001099012345").dateOfBirth(LocalDate.of(1999, 4, 8)).address("12 Nguyễn Huệ")
                .phoneNumber("0901234567").email("nguyenvana@vierec.com").build();
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ------------------------------------------------------------------ login

    @Test
    void loginSetsHttpOnlyCookiesWithThirtyMinuteAndThirtyDayLifetimes() throws Exception {
        MvcResult result = login("nguyenvana", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiresIn").value(THIRTY_MINUTES))
                .andExpect(jsonPath("$.data.refreshExpiresIn").value(THIRTY_DAYS))
                .andExpect(jsonPath("$.data.user.username").value("nguyenvana"))
                .andExpect(jsonPath("$.data.user.roles[0]").value(RoleCode.TRAINEE))
                .andReturn();

        // Tokens live in cookies only, never in the body.
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("accessToken", "refreshToken", ACCESS, REFRESH);

        String accessHeader = setCookieHeader(result, ACCESS);
        assertThat(accessHeader).contains("Max-Age=" + THIRTY_MINUTES, "Path=/;", "HttpOnly", "SameSite=Lax");
        String refreshHeader = setCookieHeader(result, REFRESH);
        assertThat(refreshHeader).contains("Max-Age=" + THIRTY_DAYS, "Path=/api/v1/auth", "HttpOnly", "SameSite=Lax");
    }

    @Test
    void loginAcceptsEmailInsteadOfUsername() throws Exception {
        login("nguyenvana@vierec.com", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void loginRejectsWrongPasswordWithoutSettingCookies() throws Exception {
        MvcResult result = login("nguyenvana", "SaiMatKhau@1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("VRC-401-002"))
                .andReturn();
        assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
    }

    @Test
    void loginRejectsUnknownUserWithSameError() throws Exception {
        login("khongtontai", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("VRC-401-002"));
    }

    @Test
    void loginRejectsLockedAccount() throws Exception {
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE username = 'nguyenvana'");
        login("nguyenvana", PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-003"));
    }

    @Test
    void fiveWrongPasswordsInARowLockTheAccount() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("nguyenvana", "SaiMatKhau@1").andExpect(jsonPath("$.code").value("VRC-401-002"));
        }
        login("nguyenvana@vierec.com", "SaiMatKhau@1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-003"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM users WHERE username = 'nguyenvana'",
                String.class)).isEqualTo("LOCKED");

        // The right password does not open a locked account; only an admin does.
        login("nguyenvana", PASSWORD).andExpect(jsonPath("$.code").value("VRC-403-003"));
        login("nguyenvana", "SaiMatKhau@1").andExpect(jsonPath("$.code").value("VRC-403-003"));
        assertThat(failedLogins()).isEqualTo(5);
    }

    @Test
    void aSuccessfulLoginResetsTheCount() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("nguyenvana", "SaiMatKhau@1");
        }
        login("nguyenvana", PASSWORD).andExpect(status().isOk());
        assertThat(failedLogins()).isZero();
        for (int i = 0; i < 4; i++) {
            login("nguyenvana", "SaiMatKhau@1");
        }
        login("nguyenvana", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void superAdminIsOnlyBlockedForFifteenMinutes() throws Exception {
        if (!roleRepository.findByCode(RoleCode.SUPER_ADMIN).isPresent()) {
            Role role = new Role();
            role.setCode(RoleCode.SUPER_ADMIN);
            role.setName("Super admin");
            roleRepository.save(role);
        }
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id, assigned_at) SELECT u.id, r.id, "
                + "CURRENT_TIMESTAMP FROM users u, roles r WHERE u.username = 'nguyenvana' AND r.code = ?",
                RoleCode.SUPER_ADMIN);

        for (int i = 0; i < 5; i++) {
            login("nguyenvana", "SaiMatKhau@1").andExpect(jsonPath("$.code").value("VRC-401-002"));
        }
        login("nguyenvana", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("VRC-429-001"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM users WHERE username = 'nguyenvana'",
                String.class)).isEqualTo("ACTIVE");

        jdbcTemplate.update("UPDATE users SET last_failed_login_at = ? WHERE username = 'nguyenvana'",
                LocalDateTime.now().minusMinutes(16));
        login("nguyenvana", PASSWORD).andExpect(status().isOk());
        assertThat(failedLogins()).isZero();
    }

    @Test
    void loginRejectsSoftDeletedAccount() throws Exception {
        jdbcTemplate.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE username = 'nguyenvana'");
        login("nguyenvana", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresUsernameAndPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    private int failedLogins() {
        return jdbcTemplate.queryForObject("SELECT failed_login_count FROM users WHERE username = 'nguyenvana'",
                Integer.class);
    }

    // ------------------------------------------------------------------ using the cookies

    @Test
    void accessCookieAuthenticatesRequests() throws Exception {
        MvcResult result = login("nguyenvana", PASSWORD).andReturn();
        mockMvc.perform(get("/api/v1/auth/me").cookie(result.getResponse().getCookie(ACCESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("nguyenvana"));
    }

    @Test
    void requestWithoutCookieIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        MvcResult result = login("nguyenvana", PASSWORD).andReturn();
        String refreshToken = result.getResponse().getCookie(REFRESH).getValue();
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie(ACCESS, refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedAccessTokenIsRejected() throws Exception {
        MvcResult result = login("nguyenvana", PASSWORD).andReturn();
        String token = result.getResponse().getCookie(ACCESS).getValue();
        mockMvc.perform(get("/api/v1/auth/me").cookie(new Cookie(ACCESS, token + "x")))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refreshIssuesNewCookiesFromRefreshCookie() throws Exception {
        MvcResult loginResult = login("nguyenvana", PASSWORD).andReturn();
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(loginResult.getResponse().getCookie(REFRESH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresIn").value(THIRTY_MINUTES))
                .andExpect(jsonPath("$.data.user.username").value("nguyenvana"))
                .andReturn();

        assertThat(setCookieHeader(refreshResult, ACCESS)).contains("Max-Age=" + THIRTY_MINUTES);
        assertThat(setCookieHeader(refreshResult, REFRESH)).contains("Max-Age=" + THIRTY_DAYS);
        mockMvc.perform(get("/api/v1/auth/me").cookie(refreshResult.getResponse().getCookie(ACCESS)))
                .andExpect(status().isOk());
    }

    @Test
    void refreshWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("VRC-401-004"));
    }

    @Test
    void refreshRejectsAccessToken() throws Exception {
        MvcResult result = login("nguyenvana", PASSWORD).andReturn();
        String accessToken = result.getResponse().getCookie(ACCESS).getValue();
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie(REFRESH, accessToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRejectsLockedAccount() throws Exception {
        MvcResult result = login("nguyenvana", PASSWORD).andReturn();
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE username = 'nguyenvana'");
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(result.getResponse().getCookie(REFRESH)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ logout

    @Test
    void logoutExpiresBothCookiesWithMatchingPaths() throws Exception {
        MvcResult loginResult = login("nguyenvana", PASSWORD).andReturn();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(loginResult.getResponse().getCookie(ACCESS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        assertThat(setCookieHeader(result, ACCESS)).startsWith(ACCESS + "=;")
                .contains("Max-Age=0", "Path=/;", "HttpOnly");
        assertThat(setCookieHeader(result, REFRESH)).startsWith(REFRESH + "=;")
                .contains("Max-Age=0", "Path=/api/v1/auth", "HttpOnly");
    }

    @Test
    void logoutWorksWithoutValidAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(setCookieHeader(result, ACCESS)).contains("Max-Age=0");
        assertThat(setCookieHeader(result, REFRESH)).contains("Max-Age=0");
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))));
    }

    private static String setCookieHeader(MvcResult result, String name) {
        List<String> headers = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        return headers.stream().filter(h -> h.startsWith(name + "=")).findFirst()
                .orElseThrow(() -> new AssertionError("No Set-Cookie for " + name + " in " + headers));
    }
}
