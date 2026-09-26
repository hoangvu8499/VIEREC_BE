package com.vierec.modules.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterApiTest {

    private static final String URL = "/api/v1/auth/register";
    private static final String PASSWORD = "Matkhau@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void seedRoles() {
        // The H2 schema is generated from the entities and starts empty; the real DB already has these rows.
        if (!roleRepository.findByCode(RoleCode.TRAINEE).isPresent()) {
            Role trainee = new Role();
            trainee.setCode(RoleCode.TRAINEE);
            trainee.setName("Trainee");
            roleRepository.save(trainee);
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ------------------------------------------------------------------ success

    @Test
    void registersTraineeWithHashedPassword() throws Exception {
        register(validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.username").value("nguyenvana"))
                .andExpect(jsonPath("$.data.cccd").value("001099012345"))
                .andExpect(jsonPath("$.data.phoneNumber").value("0901234567"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1999-04-08"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.TRAINEE)))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        transactionTemplate.executeWithoutResult(tx -> {
            User saved = userRepository.findByUsername("nguyenvana").orElseThrow(IllegalStateException::new);
            assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2a$12$");
            assertThat(passwordEncoder.matches(PASSWORD, saved.getPasswordHash())).isTrue();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUserRoles()).hasSize(1)
                    .allSatisfy(userRole -> assertThat(userRole.getAssignedAt()).isNotNull());
        });
    }

    @Test
    void storesEmailInLowerCase() throws Exception {
        RegisterRequest request = validRequest();
        request.setEmail("NguyenVanA@Vierec.com");
        register(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("nguyenvana@vierec.com"));
    }

    // ------------------------------------------------------------------ required fields

    @Test
    void rejectsEmptyBodyListingEveryField() throws Exception {
        register(new RegisterRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "username", "password", "firstName", "lastName", "cccd",
                        "dateOfBirth", "address", "phoneNumber", "email")));
    }

    @Test
    void rejectsWhitespaceOnlyValues() throws Exception {
        RegisterRequest request = validRequest();
        request.setFirstName("   ");
        request.setLastName("");
        request.setAddress(" ");
        register(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("firstName", "lastName", "address")));
    }

    // ------------------------------------------------------------------ format rules

    @ParameterizedTest
    @ValueSource(strings = {"00109901234", "0010990123456", "00109901234a", "001 99012345", "-01099012345"})
    void rejectsCccdThatIsNotTwelveDigits(String cccd) throws Exception {
        RegisterRequest request = validRequest();
        request.setCccd(cccd);
        expectFieldError(request, "cccd");
    }

    @ParameterizedTest
    @ValueSource(strings = {"090123456", "09012345678", "090123456a", "+840901234", "090-123-456"})
    void rejectsPhoneThatIsNotTenDigits(String phone) throws Exception {
        RegisterRequest request = validRequest();
        request.setPhoneNumber(phone);
        expectFieldError(request, "phoneNumber");
    }

    @ParameterizedTest
    @ValueSource(strings = {"nguyenvana.vierec.com", "nguyenvana@", "@vierec.com", "nguyen vana@vierec.com",
            "nguyenvana@vierec", "nguyenvana@@vierec.com"})
    void rejectsInvalidEmail(String email) throws Exception {
        RegisterRequest request = validRequest();
        request.setEmail(email);
        expectFieldError(request, "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abc@123", "Matkhau123", "abcdefgh"})
    void rejectsPasswordShorterThanEightOrWithoutSpecialCharacter(String password) throws Exception {
        RegisterRequest request = validRequest();
        request.setPassword(password);
        expectFieldError(request, "password");
    }

    @Test
    void neverEchoesRejectedPassword() throws Exception {
        RegisterRequest request = validRequest();
        request.setPassword("abc12345");
        register(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'password')].rejectedValue", contains("******")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd@efg", "12345678!", "matkhau#vierec", "mat khau1"})
    void acceptsPasswordOfEightCharsWithSpecialCharacter(String password) throws Exception {
        RegisterRequest request = validRequest();
        request.setPassword(password);
        register(request).andExpect(status().isCreated());
    }

    @Test
    void rejectsDateOfBirthInTheFuture() throws Exception {
        RegisterRequest request = validRequest();
        request.setDateOfBirth(LocalDate.now().plusDays(1));
        expectFieldError(request, "dateOfBirth");
    }

    // ------------------------------------------------------------------ uniqueness

    @Test
    void rejectsDuplicateUsername() throws Exception {
        register(validRequest()).andExpect(status().isCreated());
        RegisterRequest other = anotherPerson();
        other.setUsername("nguyenvana");
        register(other).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VRC-409-101"));
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() throws Exception {
        register(validRequest()).andExpect(status().isCreated());
        RegisterRequest other = anotherPerson();
        other.setEmail("NGUYENVANA@vierec.com");
        register(other).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VRC-409-102"));
    }

    @Test
    void rejectsDuplicatePhoneNumber() throws Exception {
        register(validRequest()).andExpect(status().isCreated());
        RegisterRequest other = anotherPerson();
        other.setPhoneNumber("0901234567");
        register(other).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VRC-409-103"));
    }

    @Test
    void rejectsDuplicateCccd() throws Exception {
        register(validRequest()).andExpect(status().isCreated());
        RegisterRequest other = anotherPerson();
        other.setCccd("001099012345");
        register(other).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VRC-409-104"));
    }

    @Test
    void rejectsValuesStillHeldBySoftDeletedUser() throws Exception {
        register(validRequest()).andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE username = 'nguyenvana'");
        register(validRequest()).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions register(RegisterRequest request) throws Exception {
        return mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private void expectFieldError(RegisterRequest request, String field) throws Exception {
        register(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
    }

    private static RegisterRequest validRequest() {
        return RegisterRequest.builder()
                .username("nguyenvana")
                .password(PASSWORD)
                .firstName("Văn A")
                .lastName("Nguyễn")
                .cccd("001099012345")
                .dateOfBirth(LocalDate.of(1999, 4, 8))
                .address("12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh")
                .phoneNumber("0901234567")
                .email("nguyenvana@vierec.com")
                .build();
    }

    private static RegisterRequest anotherPerson() {
        return RegisterRequest.builder()
                .username("tranthib")
                .password(PASSWORD)
                .firstName("Thị B")
                .lastName("Trần")
                .cccd("079199054321")
                .dateOfBirth(LocalDate.of(2000, 1, 15))
                .address("34 Lê Lợi, Quận 1, TP. Hồ Chí Minh")
                .phoneNumber("0912345678")
                .email("tranthib@vierec.com")
                .build();
    }
}
