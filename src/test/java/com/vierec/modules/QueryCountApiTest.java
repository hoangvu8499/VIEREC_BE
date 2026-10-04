package com.vierec.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.JwtTokenProvider;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManagerFactory;
import javax.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards against N+1 queries: the number of SQL statements of a list must not grow with the number of rows.
 * Each test measures a request, adds rows, and measures the same request again. Lists stay shorter than a page:
 * a full page adds the COUNT query.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QueryCountApiTest {

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
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private Cookie admin;
    private long businessId;
    private long courseId;
    private int sequence;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        role(RoleCode.TRAINEE);
        role(RoleCode.BUSINESS);
        role(RoleCode.ADMIN);
        admin = login(adminUser());
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO businesses (name, tax_code, address, phone_number, email, status, "
                + "created_at, updated_at) VALUES ('Công ty Xanh', '0101234567', 'Hà Nội', '0241234567', "
                + "'lienhe@xanh.vn', 'ACTIVE', ?, ?)", now, now);
        businessId = jdbcTemplate.queryForObject("SELECT id FROM businesses", Long.class);
        courseId = course();
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"lesson_progress", "course_enrollments", "lessons", "courses",
                "user_roles", "users", "businesses"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void approvalQueueLoadsUsersRolesAndBusinessesInFixedQueries() throws Exception {
        MockHttpServletRequestBuilder queue = get("/api/v1/enrollments").param("status", "PENDING");
        enrollMembers(2);
        long few = statements(queue, admin);
        enrollMembers(4);
        assertThat(statements(queue, admin)).isEqualTo(few);
    }

    @Test
    void courseListLoadsInstructorsInFixedQueries() throws Exception {
        MockHttpServletRequestBuilder list = get("/api/v1/courses");
        long few = statements(list, admin);
        for (int i = 0; i < 6; i++) {
            course();
        }
        assertThat(statements(list, admin)).isEqualTo(few);
    }

    @Test
    void userListLoadsRolesInFixedQueries() throws Exception {
        MockHttpServletRequestBuilder list = get("/api/v1/users");
        members(2);
        long few = statements(list, admin);
        members(4);
        assertThat(statements(list, admin)).isEqualTo(few);
    }

    @Test
    void businessMembersLoadInFixedQueries() throws Exception {
        MockHttpServletRequestBuilder list = get("/api/v1/businesses/" + businessId + "/members");
        enrollMembers(2);
        long few = statements(list, admin);
        enrollMembers(4);
        assertThat(statements(list, admin)).isEqualTo(few);
    }

    @Test
    void memberDetailLoadsEveryCourseProgressInFixedQueries() throws Exception {
        long member = members(1).get(0);
        MockHttpServletRequestBuilder detail = get("/api/v1/businesses/" + businessId + "/members/" + member);
        enroll(member, courseId, "ENROLLED");
        long one = statements(detail, admin);
        for (int i = 0; i < 4; i++) {
            enroll(member, course(), "ENROLLED");
        }
        assertThat(statements(detail, admin)).isEqualTo(one);
    }

    @Test
    void businessEnrollQueriesGrowOnlyByOneInsertPerLearner() throws Exception {
        Cookie manager = login(manager());
        List<Long> few = members(2);
        List<Long> many = members(8);
        long forFew = statements(enrollRequest(few), manager) - few.size();
        long otherCourse = course();
        courseId = otherCourse;
        assertThat(statements(enrollRequest(many), manager) - many.size()).isEqualTo(forFew);
    }

    // ------------------------------------------------------------------ helpers

    private long statements(MockHttpServletRequestBuilder request, Cookie cookie) throws Exception {
        statistics.clear();
        mockMvc.perform(request.cookie(cookie)).andExpect(status().is2xxSuccessful());
        return statistics.getPrepareStatementCount();
    }

    private MockHttpServletRequestBuilder enrollRequest(List<Long> userIds) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("courseId", courseId);
        body.put("userIds", userIds);
        return post("/api/v1/my-business/enrollments").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private void enrollMembers(int count) {
        for (Long userId : members(count)) {
            enroll(userId, courseId, "PENDING");
        }
    }

    private void enroll(long userId, long course, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO course_enrollments (user_id, course_id, status, price, enrolled_at, "
                + "approved_at, updated_at) VALUES (?, ?, ?, 100000, ?, ?, ?)", userId, course, status, now,
                "PENDING".equals(status) ? null : now, now);
    }

    /** Learners of the business, each with the TRAINEE role. */
    private List<Long> members(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(insertUser("hv" + (++sequence), RoleCode.TRAINEE, businessId));
        }
        return ids;
    }

    private User manager() {
        long id = insertUser("quanly" + (++sequence), RoleCode.BUSINESS, businessId);
        return userRepository.findById(id).orElseThrow(IllegalStateException::new);
    }

    private User adminUser() {
        long id = insertUser("quantri", RoleCode.ADMIN, null);
        return userRepository.findById(id).orElseThrow(IllegalStateException::new);
    }

    private long insertUser(String username, String roleCode, Long business) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO users (username, password_hash, first_name, last_name, email, phone_number, "
                + "status, business_id, created_at, updated_at) VALUES (?, '$2a$12$hash', ?, 'Nguyễn', ?, ?, "
                + "'ACTIVE', ?, ?, ?)", username, username, username + "@example.com",
                "09" + String.format("%08d", Math.abs(username.hashCode()) % 100000000), business, now, now);
        long id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
        Integer roleId = roleRepository.findByCode(roleCode).map(Role::getId).orElseThrow(IllegalStateException::new);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id, assigned_at) VALUES (?, ?, ?)", id, roleId,
                now);
        return id;
    }

    private long course() {
        long instructor = insertUser("gv" + (++sequence), RoleCode.ADMIN, null);
        String name = "Khoá " + (++sequence);
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO courses (name, price, status, instructor_id, created_by, created_at, "
                + "updated_at) VALUES (?, 100000, 'PUBLISHED', ?, ?, ?, ?)", name, instructor, instructor, now, now);
        long id = jdbcTemplate.queryForObject("SELECT id FROM courses WHERE name = ?", Long.class, name);
        jdbcTemplate.update("INSERT INTO lessons (course_id, title, video_url, sort_order, created_at, updated_at) "
                + "VALUES (?, 'Bài 1', 'https://youtu.be/abcdefghijk', 1, ?, ?)", id, now, now);
        return id;
    }

    private void role(String code) {
        if (!roleRepository.findByCode(code).isPresent()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(code);
            roleRepository.save(role);
        }
    }

    private Cookie login(User user) {
        String token = transactionTemplate.execute(tx ->
                tokenProvider.generateAccessToken(new CustomUserDetails(userRepository.findById(user.getId())
                        .orElseThrow(IllegalStateException::new))));
        return new Cookie("access_token", token);
    }
}
