package com.vierec.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.dto.AssignRolesRequest;
import com.vierec.modules.user.dto.CreateUserRequest;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserApiTest {

    private static final String USERS = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User superAdminUser;
    private User adminUser;
    private Cookie superAdmin;
    private Cookie admin;
    private Cookie trainee;

    @BeforeEach
    void setUp() {
        role(RoleCode.SUPER_ADMIN);
        role(RoleCode.ADMIN);
        role(RoleCode.TRAINEE);
        superAdminUser = user("sieuquantri", RoleCode.SUPER_ADMIN);
        adminUser = user("quantri", RoleCode.ADMIN);
        superAdmin = login(superAdminUser);
        admin = login(adminUser);
        trainee = login(user("hocvien", RoleCode.TRAINEE));
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM user_roles");
        jdbcTemplate.update("DELETE FROM users");
    }

    // ------------------------------------------------------------------ roles

    @Test
    void listRolesReturnsEveryRoleForAdmins() throws Exception {
        mockMvc.perform(get("/api/v1/roles").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].code",
                        containsInAnyOrder(RoleCode.SUPER_ADMIN, RoleCode.ADMIN, RoleCode.TRAINEE)))
                .andExpect(jsonPath("$.data[0].id").isNumber())
                .andExpect(jsonPath("$.data[0].name").isNotEmpty());

        mockMvc.perform(get("/api/v1/roles").cookie(trainee)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ create

    @Test
    void adminCreatesAUserWithRoles() throws Exception {
        createUser(admin, newUser("nhanvien", RoleCode.TRAINEE, RoleCode.ADMIN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("nhanvien"))
                .andExpect(jsonPath("$.data.email").value("nhanvien@vierec.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.TRAINEE, RoleCode.ADMIN)))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM users WHERE username = 'nhanvien'");
        assertThat((String) row.get("password_hash")).startsWith("$2a$").isNotEqualTo("Matkhau@123");
        assertThat(jdbcTemplate.queryForList("SELECT assigned_by FROM user_roles WHERE user_id = ?", Long.class,
                row.get("id"))).containsOnly(adminUser.getId());
    }

    @Test
    void createUserKeepsTheGivenStatus() throws Exception {
        CreateUserRequest request = newUser("bikhoa", RoleCode.TRAINEE);
        request.setStatus(UserStatus.LOCKED);
        createUser(admin, request).andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void createUserValidatesLikeRegistrationAndNeedsRoles() throws Exception {
        mockMvc.perform(post(USERS).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("username", "password", "firstName",
                        "lastName", "cccd", "dateOfBirth", "address", "phoneNumber", "email", "roles")));

        createUser(admin, newUser("khongvaitro", "KHONG_CO"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-102"))
                .andExpect(jsonPath("$.message").value("Role not found: KHONG_CO"));

        createUser(admin, newUser("quantri", RoleCode.TRAINEE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VRC-409-101"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class)).isEqualTo(3);
    }

    @Test
    void onlySuperAdminCreatesSuperAdmins() throws Exception {
        createUser(admin, newUser("leoquyen", RoleCode.SUPER_ADMIN))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VRC-403-101"));
        createUser(superAdmin, newUser("sieuquantri2", RoleCode.SUPER_ADMIN))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.SUPER_ADMIN)));
    }

    @Test
    void createUserNeedsAnAdmin() throws Exception {
        createUser(trainee, newUser("a1234", RoleCode.TRAINEE)).andExpect(status().isForbidden());
        mockMvc.perform(post(USERS).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser("a1234", RoleCode.TRAINEE))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ assign roles

    @Test
    void assignRolesReplacesTheRolesAndRecordsWhoGrantedThem() throws Exception {
        User target = user("nhanvien", RoleCode.TRAINEE);

        assignRoles(admin, target.getId(), RoleCode.ADMIN, RoleCode.TRAINEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.ADMIN, RoleCode.TRAINEE)));
        // TRAINEE was kept, so its assignment row is untouched (seeded with assigned_by NULL).
        assertThat(jdbcTemplate.queryForList("SELECT r.code, ur.assigned_by FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? ORDER BY r.code", target.getId()))
                .extracting(row -> row.get("CODE") + "=" + row.get("ASSIGNED_BY"))
                .containsExactly("ADMIN=" + adminUser.getId(), "TRAINEE=null");

        assignRoles(admin, target.getId(), RoleCode.TRAINEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.TRAINEE)));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_roles WHERE user_id = ?", Long.class,
                target.getId())).isEqualTo(1);
    }

    @Test
    void onlySuperAdminTouchesTheSuperAdminRole() throws Exception {
        User target = user("nhanvien", RoleCode.TRAINEE);
        assignRoles(admin, target.getId(), RoleCode.SUPER_ADMIN)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VRC-403-101"));
        assignRoles(admin, superAdminUser.getId(), RoleCode.TRAINEE)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VRC-403-101"));

        assignRoles(superAdmin, target.getId(), RoleCode.SUPER_ADMIN).andExpect(status().isOk());
        assignRoles(superAdmin, target.getId(), RoleCode.TRAINEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.TRAINEE)));
    }

    @Test
    void nobodyChangesTheirOwnRoles() throws Exception {
        assignRoles(superAdmin, superAdminUser.getId(), RoleCode.TRAINEE)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VRC-403-102"));
        assignRoles(admin, adminUser.getId(), RoleCode.ADMIN, RoleCode.TRAINEE)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("VRC-403-102"));
    }

    @Test
    void assignRolesValidatesInput() throws Exception {
        User target = user("nhanvien", RoleCode.TRAINEE);
        mockMvc.perform(put(USERS + "/" + target.getId() + "/roles").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("roles"));
        assignRoles(admin, target.getId(), "KHONG_CO")
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-102"));
        assignRoles(admin, 999_999L, RoleCode.TRAINEE)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-101"));
        assignRoles(trainee, target.getId(), RoleCode.ADMIN).andExpect(status().isForbidden());
    }

    @Test
    void updateUserNoLongerChangesRoles() throws Exception {
        User target = user("nhanvien", RoleCode.TRAINEE);
        mockMvc.perform(put(USERS + "/" + target.getId()).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Mới\",\"roles\":[\"SUPER_ADMIN\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Mới"))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.TRAINEE)));
    }

    // ------------------------------------------------------------------ read / delete

    @Test
    void getSearchAndSoftDelete() throws Exception {
        User target = user("nhanvien", RoleCode.TRAINEE);

        mockMvc.perform(get(USERS + "/" + target.getId()).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("nhanvien"));
        mockMvc.perform(get(USERS).param("keyword", "nhanvien").cookie(admin))
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(delete(USERS + "/" + target.getId()).cookie(admin)).andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", Object.class,
                target.getId())).isNotNull();
        mockMvc.perform(get(USERS + "/" + target.getId()).cookie(admin)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ helpers

    private void role(String code) {
        if (!roleRepository.findByCode(code).isPresent()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(code);
            roleRepository.save(role);
        }
    }

    private User user(String username, String roleCode) {
        return transactionTemplate.execute(tx -> {
            User user = User.builder().username(username).passwordHash("$2a$12$hash")
                    .firstName(username).lastName("Họ").status(UserStatus.ACTIVE).build();
            user.addRole(roleRepository.findByCode(roleCode).orElseThrow(IllegalStateException::new), null);
            return userRepository.save(user);
        });
    }

    private Cookie login(User user) {
        String token = transactionTemplate.execute(tx ->
                tokenProvider.generateAccessToken(new CustomUserDetails(userRepository.findById(user.getId())
                        .orElseThrow(IllegalStateException::new))));
        return new Cookie("access_token", token);
    }

    private static CreateUserRequest newUser(String username, String... roles) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword("Matkhau@123");
        request.setFirstName("Văn A");
        request.setLastName("Nguyễn");
        request.setCccd(String.format("%012d", Math.abs(username.hashCode()) % 1_000_000_000_000L));
        request.setDateOfBirth(LocalDate.of(1999, 4, 8));
        request.setAddress("12 Nguyễn Huệ");
        request.setPhoneNumber(String.format("09%08d", Math.abs(username.hashCode()) % 100_000_000));
        request.setEmail(username + "@vierec.com");
        request.setRoles(new HashSet<>(Arrays.asList(roles)));
        return request;
    }

    private ResultActions createUser(Cookie caller, CreateUserRequest request) throws Exception {
        return mockMvc.perform(post(USERS).cookie(caller).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions assignRoles(Cookie caller, long userId, String... roles) throws Exception {
        AssignRolesRequest request = new AssignRolesRequest(new HashSet<>(Arrays.asList(roles)));
        return mockMvc.perform(put(USERS + "/" + userId + "/roles").cookie(caller)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)));
    }
}
