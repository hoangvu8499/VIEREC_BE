package com.vierec.modules.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.course.entity.CourseVideos;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessApiTest {

    private static final String YOUTUBE_ID = "abcdefghijk";

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

    private Cookie admin;

    @BeforeEach
    void setUp() {
        role(RoleCode.TRAINEE);
        role(RoleCode.BUSINESS);
        admin = login(user("quantri", RoleCode.ADMIN));
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"lesson_progress", "exam_attempts", "exam_questions", "exams",
                "course_enrollments", "lessons", "courses", "user_roles", "users", "businesses"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    // ------------------------------------------------------------------ admin

    @Test
    void adminCreatesABusinessWithItsFirstManager() throws Exception {
        perform(post("/api/v1/businesses").contentType(MediaType.APPLICATION_JSON)
                .content(json(business("0101234567", "mtxanh.admin"))), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Công ty Môi trường Xanh"))
                .andExpect(jsonPath("$.data.taxCode").value("0101234567"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.managerCount").value(1))
                .andExpect(jsonPath("$.data.memberCount").value(0));
        long businessId = businessId("0101234567");

        perform(get("/api/v1/businesses/" + businessId + "/managers"), admin)
                .andExpect(jsonPath("$.data[0].username").value("mtxanh.admin"))
                .andExpect(jsonPath("$.data[0].roles", contains("BUSINESS")))
                .andExpect(jsonPath("$.data[0].businessName").value("Công ty Môi trường Xanh"));
        perform(get("/api/v1/businesses?keyword=xanh"), admin)
                .andExpect(jsonPath("$.data.content[0].id").value(businessId));

        // Same tax code, then a taken manager username: nothing is created.
        perform(post("/api/v1/businesses").contentType(MediaType.APPLICATION_JSON)
                .content(json(business("0101234567", "khac.admin"))), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-801"));
        perform(post("/api/v1/businesses").contentType(MediaType.APPLICATION_JSON)
                .content(json(business("0109999999", "mtxanh.admin"))), admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-101"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM businesses", Integer.class)).isEqualTo(1);

        Map<String, Object> invalid = business("12345", "x");
        invalid.put("name", " ");
        perform(post("/api/v1/businesses").contentType(MediaType.APPLICATION_JSON).content(json(invalid)), admin)
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminsManageBusinesses() throws Exception {
        Cookie trainee = login(user("hocvien", RoleCode.TRAINEE));
        perform(get("/api/v1/businesses"), trainee).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/businesses")).andExpect(status().isUnauthorized());
        perform(get("/api/v1/my-business"), trainee).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ business manager

    @Test
    void aManagerCreatesLearnersAndSeesOnlyTheirOwnBusiness() throws Exception {
        Cookie manager = managerOf(createBusiness("0101234567", "mtxanh.admin"), "mtxanh.admin");
        Cookie otherManager = managerOf(createBusiness("0107654321", "khac.admin"), "khac.admin");

        perform(post("/api/v1/my-business/members").contentType(MediaType.APPLICATION_JSON)
                .content(json(learner("nv.an", "001099000001"))), manager)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("nv.an"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn nv.an"));
        long learnerId = userId("nv.an");
        assertThat(jdbcTemplate.queryForObject("SELECT r.code FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ?", String.class, learnerId)).isEqualTo("TRAINEE");

        perform(get("/api/v1/my-business"), manager)
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.managerCount").value(1));
        perform(get("/api/v1/my-business/members"), manager)
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(learnerId));
        perform(get("/api/v1/my-business/members"), otherManager)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        perform(get("/api/v1/my-business/members/" + learnerId), otherManager)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-802"));
        // A manager is not a learner of the business.
        perform(get("/api/v1/my-business/members/" + userId("mtxanh.admin")), manager)
                .andExpect(status().isNotFound());

        // Same identity rules as the registration.
        perform(post("/api/v1/my-business/members").contentType(MediaType.APPLICATION_JSON)
                .content(json(learner("nv.binh", "001099000001"))), manager)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-104"));
    }

    @Test
    void managersOfAnInactiveBusinessOrWithoutBusinessAreRefused() throws Exception {
        long businessId = createBusiness("0101234567", "mtxanh.admin");
        Cookie manager = managerOf(businessId, "mtxanh.admin");
        Map<String, Object> update = business("0101234567", null);
        update.remove("manager");
        update.put("status", "INACTIVE");
        perform(put("/api/v1/businesses/" + businessId).contentType(MediaType.APPLICATION_JSON)
                .content(json(update)), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        perform(get("/api/v1/my-business/members"), manager)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-802"));
        perform(get("/api/v1/my-business"), login(user("lac.loai", RoleCode.BUSINESS)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-801"));
    }

    @Test
    void aManagerEnrollsLearnersAndFollowsTheirProgressAndExam() throws Exception {
        long businessId = createBusiness("0101234567", "mtxanh.admin");
        Cookie manager = managerOf(businessId, "mtxanh.admin");
        perform(post("/api/v1/my-business/members").contentType(MediaType.APPLICATION_JSON)
                .content(json(learner("nv.an", "001099000001"))), manager).andExpect(status().isCreated());
        long learnerId = userId("nv.an");
        long courseId = course("Phòng cháy chữa cháy", 1_500_000);
        long lessonId = lesson(courseId, "https://www.youtube.com/watch?v=" + YOUTUBE_ID);
        long outsider = user("tu.do", RoleCode.TRAINEE).getId();

        perform(post("/api/v1/my-business/enrollments").contentType(MediaType.APPLICATION_JSON)
                .content(json(enroll(courseId, learnerId, outsider))), manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-801"));
        perform(post("/api/v1/my-business/enrollments").contentType(MediaType.APPLICATION_JSON)
                .content(json(enroll(courseId, learnerId))), manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enrolled[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.enrolled[0].businessName").value("Công ty Môi trường Xanh"))
                .andExpect(jsonPath("$.data.totalAmount").value(1_500_000));
        perform(post("/api/v1/my-business/enrollments").contentType(MediaType.APPLICATION_JSON)
                .content(json(enroll(courseId, learnerId))), manager)
                .andExpect(jsonPath("$.data.enrolled.length()").value(0))
                .andExpect(jsonPath("$.data.skipped[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
        perform(get("/api/v1/enrollments?status=PENDING"), admin)
                .andExpect(jsonPath("$.data.content[0].businessName").value("Công ty Môi trường Xanh"));

        // Approved: the learner watches half of the video, the exam stays closed.
        jdbcTemplate.update("UPDATE course_enrollments SET status = 'ENROLLED', approved_at = ?",
                LocalDateTime.now());
        createExam(courseId);
        Cookie learner = login(userRepository.findById(learnerId).orElseThrow(IllegalStateException::new));
        saveProgress(courseId, learner, lessonId, "youtube-" + YOUTUBE_ID, 100, 50)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percent").value(50))
                .andExpect(jsonPath("$.data.examReady").value(false));
        perform(post("/api/v1/courses/" + courseId + "/my-exam/start"), learner)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-703"));

        // Values only grow; unknown videos are skipped.
        saveProgress(courseId, learner, lessonId, "youtube-" + YOUTUBE_ID, 100, 85)
                .andExpect(jsonPath("$.data.percent").value(85));
        saveProgress(courseId, learner, lessonId, "youtube-" + YOUTUBE_ID, 100, 10)
                .andExpect(jsonPath("$.data.percent").value(85))
                .andExpect(jsonPath("$.data.examReady").value(true));
        saveProgress(courseId, learner, lessonId, "file-999", 100, 100)
                .andExpect(jsonPath("$.data.videos.length()").value(1));
        perform(post("/api/v1/courses/" + courseId + "/my-exam/start"), learner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.status").value("IN_PROGRESS"));

        perform(get("/api/v1/my-business/members"), manager)
                .andExpect(jsonPath("$.data.content[0].learningCount").value(1));
        perform(get("/api/v1/my-business/members/" + learnerId), manager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.learningCount").value(1))
                .andExpect(jsonPath("$.data.courses[0].courseName").value("Phòng cháy chữa cháy"))
                .andExpect(jsonPath("$.data.courses[0].status").value("ENROLLED"))
                .andExpect(jsonPath("$.data.courses[0].progress.percent").value(85))
                .andExpect(jsonPath("$.data.courses[0].progress.videos").doesNotExist())
                .andExpect(jsonPath("$.data.courses[0].exam.submitted").value(false))
                .andExpect(jsonPath("$.data.courses[0].certificate").value((Object) null));
        perform(get("/api/v1/businesses/" + businessId + "/members/" + learnerId), admin)
                .andExpect(jsonPath("$.data.courses[0].progress.percent").value(85));
    }

    @Test
    void onlyApprovedLearnersSaveProgress() throws Exception {
        Cookie learner = login(user("hocvien", RoleCode.TRAINEE));
        long courseId = course("Khoá chưa đăng ký", 0);
        long lessonId = lesson(courseId, "https://youtu.be/" + YOUTUBE_ID);

        saveProgress(courseId, learner, lessonId, "youtube-" + YOUTUBE_ID, 100, 50)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-201"));
        perform(get("/api/v1/courses/" + courseId + "/my-progress"), learner)
                .andExpect(status().isForbidden());
    }

    @Test
    void recognisesTheSameYoutubeLinksAsTheFrontend() {
        assertThat(CourseVideos.youtubeVideoId("https://www.youtube.com/watch?v=" + YOUTUBE_ID + "&t=10"))
                .isEqualTo(YOUTUBE_ID);
        assertThat(CourseVideos.youtubeVideoId("https://youtu.be/" + YOUTUBE_ID)).isEqualTo(YOUTUBE_ID);
        assertThat(CourseVideos.youtubeVideoId("https://m.youtube.com/shorts/" + YOUTUBE_ID)).isEqualTo(YOUTUBE_ID);
        assertThat(CourseVideos.youtubeVideoId("https://www.youtube-nocookie.com/embed/" + YOUTUBE_ID))
                .isEqualTo(YOUTUBE_ID);
        assertThat(CourseVideos.youtubeVideoId("https://www.youtube.com/watch?v=abc123")).isNull();
        assertThat(CourseVideos.youtubeVideoId("https://drive.google.com/file/d/abc/view")).isNull();
        assertThat(CourseVideos.youtubeVideoId("không phải link")).isNull();
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions saveProgress(long courseId, Cookie learner, long lessonId, String key, int duration,
                                       int watched) throws Exception {
        Map<String, Object> video = new HashMap<>();
        video.put("lessonId", lessonId);
        video.put("videoKey", key);
        video.put("durationSeconds", duration);
        video.put("watchedSeconds", watched);
        return perform(put("/api/v1/courses/" + courseId + "/my-progress").contentType(MediaType.APPLICATION_JSON)
                .content(json(Collections.singletonMap("videos", Collections.singletonList(video)))), learner);
    }

    private void createExam(long courseId) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO exams (course_id, title, duration_minutes, pass_score, created_at, "
                + "updated_at) VALUES (?, 'Bài thi', 30, 5, ?, ?)", courseId, now, now);
        long examId = jdbcTemplate.queryForObject("SELECT id FROM exams WHERE course_id = ?", Long.class, courseId);
        jdbcTemplate.update("INSERT INTO exam_questions (exam_id, content, option_a, option_b, option_c, option_d, "
                + "correct_option, sort_order, created_at, updated_at) VALUES (?, 'Câu 1', 'A', 'B', 'C', 'D', 'A', "
                + "1, ?, ?)", examId, now, now);
    }

    private static Map<String, Object> enroll(long courseId, Long... userIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("courseId", courseId);
        body.put("userIds", Arrays.asList(userIds));
        return body;
    }

    private static Map<String, Object> business(String taxCode, String managerUsername) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Công ty Môi trường Xanh");
        body.put("taxCode", taxCode);
        body.put("address", "12 Nguyễn Huệ, TP. Hồ Chí Minh");
        body.put("phoneNumber", "0281234567");
        body.put("email", "lienhe@moitruongxanh.vn");
        if (managerUsername != null) {
            Map<String, Object> manager = new HashMap<>();
            manager.put("username", managerUsername);
            manager.put("password", "Matkhau@123");
            manager.put("firstName", "Bình");
            manager.put("lastName", "Trần");
            manager.put("phoneNumber", "09" + String.format("%08d", Math.abs(managerUsername.hashCode()) % 100000000));
            manager.put("email", managerUsername + "@moitruongxanh.vn");
            body.put("manager", manager);
        }
        return body;
    }

    private static Map<String, Object> learner(String username, String cccd) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "Matkhau@123");
        body.put("firstName", username);
        body.put("lastName", "Nguyễn");
        body.put("cccd", cccd);
        body.put("dateOfBirth", "1995-05-20");
        body.put("address", "Hà Nội");
        body.put("phoneNumber", "08" + String.format("%08d", Math.abs(username.hashCode()) % 100000000));
        body.put("email", username + "@example.com");
        return body;
    }

    private long createBusiness(String taxCode, String managerUsername) throws Exception {
        perform(post("/api/v1/businesses").contentType(MediaType.APPLICATION_JSON)
                .content(json(business(taxCode, managerUsername))), admin)
                .andExpect(status().isCreated());
        return businessId(taxCode);
    }

    /** Logs in as a manager created with {@link #createBusiness}. */
    private Cookie managerOf(long businessId, String username) {
        assertThat(jdbcTemplate.queryForObject("SELECT business_id FROM users WHERE username = ?", Long.class,
                username)).isEqualTo(businessId);
        return login(userRepository.findByUsername(username).orElseThrow(IllegalStateException::new));
    }

    private long businessId(String taxCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM businesses WHERE tax_code = ?", Long.class, taxCode);
    }

    private long userId(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    private long course(String name, long price) {
        jdbcTemplate.update("INSERT INTO courses (name, price, status, created_at, updated_at) "
                + "VALUES (?, ?, 'PUBLISHED', ?, ?)", name, price, LocalDateTime.now(), LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT id FROM courses WHERE name = ?", Long.class, name);
    }

    private long lesson(long courseId, String videoUrl) {
        jdbcTemplate.update("INSERT INTO lessons (course_id, title, video_url, sort_order, created_at, updated_at) "
                + "VALUES (?, 'Bài 1', ?, 1, ?, ?)", courseId, videoUrl, LocalDateTime.now(), LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT id FROM lessons WHERE course_id = ?", Long.class, courseId);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Cookie cookie) throws Exception {
        return mockMvc.perform(request.cookie(cookie));
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private void role(String code) {
        if (!roleRepository.findByCode(code).isPresent()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(code);
            roleRepository.save(role);
        }
    }

    private User user(String username, String roleCode) {
        role(roleCode);
        return transactionTemplate.execute(tx -> {
            User user = User.builder().username(username).passwordHash("$2a$12$hash")
                    .firstName(username).lastName("Họ").email(username + "@example.com").build();
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
}
