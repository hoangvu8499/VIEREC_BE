package com.vierec.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.infrastructure.mail.EmailService;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetApiTest {

    private static final String EMAIL = "nguyenvana@vierec.com";
    private static final String PASSWORD = "Matkhau@123";
    private static final String NEW_PASSWORD = "MatkhauMoi@456";
    private static final Pattern CODE = Pattern.compile("\\b(\\d{6})\\b");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockBean
    private EmailService emailService;

    @BeforeEach
    void registerUser() throws Exception {
        when(emailService.send(anyString(), anyString(), anyString())).thenReturn(true);
        if (!roleRepository.findByCode(RoleCode.TRAINEE).isPresent()) {
            Role trainee = new Role();
            trainee.setCode(RoleCode.TRAINEE);
            trainee.setName("Trainee");
            roleRepository.save(trainee);
        }
        RegisterRequest request = RegisterRequest.builder()
                .username("nguyenvana").password(PASSWORD).firstName("Văn A").lastName("Nguyễn")
                .cccd("001099012345").dateOfBirth(LocalDate.of(1999, 4, 8)).address("12 Nguyễn Huệ")
                .phoneNumber("0901234567").email(EMAIL).build();
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());
    }

    @AfterEach
    void cleanUp() {
        reset(emailService);
        jdbcTemplate.update("DELETE FROM password_reset_codes");
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void codeFromTheEmailSetsANewPasswordOnce() throws Exception {
        forgot("NguyenVanA@Vierec.com").andExpect(status().isOk());
        String code = sentCode();
        assertThat(jdbcTemplate.queryForObject("SELECT code_hash FROM password_reset_codes", String.class))
                .hasSize(64).doesNotContain(code);

        resetPassword(wrongCode(code)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-003"));
        resetPassword(code).andExpect(status().isOk());

        login(PASSWORD).andExpect(status().isUnauthorized());
        login(NEW_PASSWORD).andExpect(status().isOk());
        resetPassword(code).andExpect(jsonPath("$.code").value("VRC-400-003"));
    }

    @Test
    void unknownEmailGetsTheSameAnswerAndNoEmail() throws Exception {
        forgot("khongco@vierec.com").andExpect(status().isOk());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
        resetPassword("khongco@vierec.com", "123456", NEW_PASSWORD).andExpect(jsonPath("$.code").value("VRC-400-003"));
    }

    @Test
    void codeIsDroppedAfterFiveWrongTries() throws Exception {
        forgot(EMAIL);
        String code = sentCode();
        for (int i = 0; i < 5; i++) {
            resetPassword(wrongCode(code)).andExpect(jsonPath("$.code").value("VRC-400-003"));
        }
        resetPassword(code).andExpect(jsonPath("$.code").value("VRC-400-003"));
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void aNewCodeIsSentAtMostOnceAMinuteAndReplacesTheOldOne() throws Exception {
        forgot(EMAIL);
        String first = sentCode();
        forgot(EMAIL).andExpect(status().isOk());
        verify(emailService, times(1)).send(eq(EMAIL), anyString(), anyString());

        jdbcTemplate.update("UPDATE password_reset_codes SET created_at = ?", LocalDateTime.now().minusMinutes(2));
        forgot(EMAIL);
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).send(eq(EMAIL), anyString(), text.capture());
        String second = codeIn(text.getValue());

        if (!second.equals(first)) {
            resetPassword(first).andExpect(jsonPath("$.code").value("VRC-400-003"));
        }
        resetPassword(second).andExpect(status().isOk());
    }

    @Test
    void expiredCodeIsRefused() throws Exception {
        forgot(EMAIL);
        String code = sentCode();
        jdbcTemplate.update("UPDATE password_reset_codes SET expires_at = ?", LocalDateTime.now().minusSeconds(1));
        resetPassword(code).andExpect(jsonPath("$.code").value("VRC-400-003"));
    }

    @Test
    void resetKeepsALockedAccountLocked() throws Exception {
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED', failed_login_count = 5");
        forgot(EMAIL);
        resetPassword(sentCode()).andExpect(status().isOk());
        login(NEW_PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VRC-403-003"));
    }

    @Test
    void resetValidatesTheCodeAndThePassword() throws Exception {
        resetPassword(EMAIL, "12ab", "ngan")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    // ------------------------------------------------------------------ helpers

    private String sentCode() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(eq(EMAIL), anyString(), text.capture());
        return codeIn(text.getValue());
    }

    private static String codeIn(String text) {
        Matcher matcher = CODE.matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private static String wrongCode(String code) {
        return String.format("%06d", (Integer.parseInt(code) + 1) % 1_000_000);
    }

    private ResultActions forgot(String email) throws Exception {
        return postJson("/api/v1/auth/forgot-password", body("email", email));
    }

    private ResultActions resetPassword(String code) throws Exception {
        return resetPassword(EMAIL, code, NEW_PASSWORD);
    }

    private ResultActions resetPassword(String email, String code, String password) throws Exception {
        Map<String, String> body = body("email", email);
        body.put("code", code);
        body.put("newPassword", password);
        return postJson("/api/v1/auth/reset-password", body);
    }

    private ResultActions login(String password) throws Exception {
        Map<String, String> body = body("username", "nguyenvana");
        body.put("password", password);
        return postJson("/api/v1/auth/login", body);
    }

    private ResultActions postJson(String url, Map<String, String> body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private static Map<String, String> body(String key, String value) {
        Map<String, String> body = new HashMap<>();
        body.put(key, value);
        return body;
    }
}
