package com.vierec.modules.exam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamAttemptApiTest {

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
    private Cookie learner;
    private long learnerId;
    private long courseId;

    @BeforeEach
    void setUp() {
        admin = login(user("quantri", RoleCode.ADMIN));
        User user = user("hocvien", RoleCode.TRAINEE);
        learner = login(user);
        learnerId = user.getId();
        courseId = course("An toàn hoá chất", null);
        enroll(learnerId, courseId, "ENROLLED");
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"exam_attempts", "exam_questions", "exams", "course_enrollments", "courses",
                "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void learnerTakesTheExamOnceWithoutSeeingTheAnswersAndPassingCompletesTheCourse() throws Exception {
        createExam("5", "A", "B", "C");

        String before = perform(get(myExam()), learner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Bài thi chứng chỉ"))
                .andExpect(jsonPath("$.data.durationMinutes").value(30))
                .andExpect(jsonPath("$.data.questionCount").value(3))
                .andExpect(jsonPath("$.data.attempt").value((Object) null))
                .andReturn().getResponse().getContentAsString();
        assertThat(before).doesNotContain("correctOption");

        String started = perform(post(myExam() + "/start"), learner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.attempt.remainingSeconds",
                        allOf(greaterThan(1790), lessThanOrEqualTo(1800))))
                .andExpect(jsonPath("$.data.attempt.questions.length()").value(3))
                .andExpect(jsonPath("$.data.attempt.questions[0].content").value("Câu 1"))
                .andExpect(jsonPath("$.data.attempt.questions[0].optionB").value("Câu 1 - B"))
                .andReturn().getResponse().getContentAsString();
        assertThat(started).doesNotContain("correctOption");
        JsonNode attempt = objectMapper.readTree(started).path("data").path("attempt");
        long[] ids = questionIds(attempt);

        // Starting again (reload, second tab) returns the same attempt: the timer is not reset.
        perform(post(myExam() + "/start"), learner)
                .andExpect(jsonPath("$.data.attempt.id").value(attempt.path("id").asLong()))
                .andExpect(jsonPath("$.data.attempt.status").value("IN_PROGRESS"));

        // 2 of 3 right (an unknown question id is ignored, the last answer of a question counts).
        perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON).content(json(answers(
                ids[0], "B", ids[0], "A", ids[1], "B", ids[2], "D", 999999L, "A"))), learner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.attempt.questionCount").value(3))
                .andExpect(jsonPath("$.data.attempt.correctCount").value(2))
                .andExpect(jsonPath("$.data.attempt.score").value(6.67))
                .andExpect(jsonPath("$.data.attempt.passScore").value(5))
                .andExpect(jsonPath("$.data.attempt.passed").value(true))
                .andExpect(jsonPath("$.data.attempt.questions").doesNotExist())
                .andExpect(jsonPath("$.data.attempt.remainingSeconds").doesNotExist());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM course_enrollments", String.class))
                .isEqualTo("COMPLETED");
        assertThat(jdbcTemplate.queryForObject("SELECT completed_at FROM course_enrollments", Timestamp.class))
                .isNotNull();

        // Only once.
        perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON)
                .content(json(answers(ids[2], "C"))), learner)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-702"));
        perform(post(myExam() + "/start"), learner)
                .andExpect(jsonPath("$.data.attempt.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.attempt.correctCount").value(2));
        perform(get(myExam()), learner)
                .andExpect(jsonPath("$.data.attempt.score").value(6.67))
                .andExpect(jsonPath("$.data.attempt.passed").value(true));
    }

    @Test
    void passingComparesTheExactFractionNotTheRoundedScore() throws Exception {
        // 2/3 = 6.666... rounds to 6.67 but stays below a pass score of 6.67.
        createExam("6.67", "A", "B", "C");
        long[] ids = questionIds(start());

        submit(answers(ids[0], "A", ids[1], "B"))
                .andExpect(jsonPath("$.data.attempt.score").value(6.67))
                .andExpect(jsonPath("$.data.attempt.passed").value(false));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM course_enrollments", String.class))
                .isEqualTo("ENROLLED");
    }

    @Test
    void answersCountDuringTheGraceMinuteOnly() throws Exception {
        createExam("5", "A", "B", "C");
        long[] ids = questionIds(start());
        moveDeadline(LocalDateTime.now().minusSeconds(30));

        perform(get(myExam()), learner)
                .andExpect(jsonPath("$.data.attempt.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.attempt.remainingSeconds").value(0));
        submit(answers(ids[0], "A", ids[1], "B", ids[2], "C"))
                .andExpect(jsonPath("$.data.attempt.correctCount").value(3))
                .andExpect(jsonPath("$.data.attempt.score").value(10))
                .andExpect(jsonPath("$.data.attempt.passed").value(true));
    }

    @Test
    void lateAnswersAreNotCounted() throws Exception {
        createExam("5", "A", "B", "C");
        long[] ids = questionIds(start());
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(5).withNano(0);
        moveDeadline(deadline);

        submit(answers(ids[0], "A", ids[1], "B", ids[2], "C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.correctCount").value(0))
                .andExpect(jsonPath("$.data.attempt.score").value(0))
                .andExpect(jsonPath("$.data.attempt.passed").value(false));
        assertThat(jdbcTemplate.queryForObject("SELECT submitted_at FROM exam_attempts", Timestamp.class)
                .toLocalDateTime()).isEqualTo(deadline);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM course_enrollments", String.class))
                .isEqualTo("ENROLLED");
    }

    @Test
    void anAttemptLeftOpenPastItsDeadlineIsGradedWhenRead() throws Exception {
        createExam("5", "A", "B", "C");
        start();
        moveDeadline(LocalDateTime.now().minusMinutes(2));

        perform(get(myExam()), learner)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.attempt.correctCount").value(0))
                .andExpect(jsonPath("$.data.attempt.passed").value(false));
        perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON)
                .content(json(answers())), learner)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-702"));
    }

    @Test
    void theDeadlineAndPassScoreAreFixedAtStart() throws Exception {
        createExam("5", "A", "B", "C");
        start();
        perform(put(exam()).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings(60, "9"))), admin)
                .andExpect(status().isOk());

        perform(get(myExam()), learner)
                .andExpect(jsonPath("$.data.durationMinutes").value(60))
                .andExpect(jsonPath("$.data.attempt.passScore").value(5))
                .andExpect(jsonPath("$.data.attempt.remainingSeconds", lessThanOrEqualTo(1800)));
    }

    @Test
    void onlyApprovedLearnersOfTheCourseTakeItsExam() throws Exception {
        createExam("5", "A");

        perform(get(myExam()), admin)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-701"));
        mockMvc.perform(get(myExam())).andExpect(status().isUnauthorized());

        jdbcTemplate.update("UPDATE course_enrollments SET status = 'PENDING'");
        perform(post(myExam() + "/start"), learner)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-701"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam_attempts", Integer.class)).isZero();

        jdbcTemplate.update("UPDATE course_enrollments SET status = 'COMPLETED'");
        perform(get(myExam()), learner).andExpect(status().isOk());

        long deleted = course("Khoá đã xoá", LocalDateTime.now());
        enroll(learnerId, deleted, "ENROLLED");
        perform(get("/api/v1/courses/" + deleted + "/my-exam"), learner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-201"));
    }

    @Test
    void reportsAMissingOrEmptyExamAndASubmitBeforeTheStart() throws Exception {
        perform(get(myExam()), learner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-701"));

        createExam("5");
        perform(post(myExam() + "/start"), learner)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-701"));
        perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON)
                .content(json(answers())), learner)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-703"));
    }

    @Test
    void validatesTheAnswers() throws Exception {
        createExam("5", "A");
        start();
        Map<String, Object> answer = new HashMap<>();
        answer.put("questionId", 1);
        answer.put("selectedOption", null);
        Map<String, Object> body = new HashMap<>();
        body.put("answers", java.util.Collections.singletonList(answer));

        perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON).content(json(body)), learner)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("answers[0].selectedOption"));
        perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON).content("{}"), learner)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("answers"));
    }

    // ------------------------------------------------------------------ helpers

    private String exam() {
        return "/api/v1/courses/" + courseId + "/exam";
    }

    private String myExam() {
        return "/api/v1/courses/" + courseId + "/my-exam";
    }

    /** Exam of 30 minutes with one question per correct option ("Câu 1", "Câu 2"...). */
    private void createExam(String passScore, String... correctOptions) throws Exception {
        perform(put(exam()).contentType(MediaType.APPLICATION_JSON).content(json(settings(30, passScore))), admin)
                .andExpect(status().isOk());
        for (int i = 0; i < correctOptions.length; i++) {
            Map<String, Object> question = new HashMap<>();
            String content = "Câu " + (i + 1);
            question.put("content", content);
            for (String option : new String[] {"A", "B", "C", "D"}) {
                question.put("option" + option, content + " - " + option);
            }
            question.put("correctOption", correctOptions[i]);
            perform(post(exam() + "/questions").contentType(MediaType.APPLICATION_JSON).content(json(question)),
                    admin).andExpect(status().isCreated());
        }
    }

    private JsonNode start() throws Exception {
        String body = perform(post(myExam() + "/start"), learner)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("attempt");
    }

    private ResultActions submit(Map<String, Object> body) throws Exception {
        return perform(post(myExam() + "/submit").contentType(MediaType.APPLICATION_JSON).content(json(body)),
                learner);
    }

    private static long[] questionIds(JsonNode attempt) {
        JsonNode questions = attempt.path("questions");
        long[] ids = new long[questions.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = questions.path(i).path("id").asLong();
        }
        return ids;
    }

    /** Pairs of (question id, option). */
    private static Map<String, Object> answers(Object... pairs) {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            Map<String, Object> answer = new HashMap<>();
            answer.put("questionId", pairs[i]);
            answer.put("selectedOption", pairs[i + 1]);
            answers.add(answer);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("answers", answers);
        return body;
    }

    private static Map<String, Object> settings(int duration, String passScore) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "Bài thi chứng chỉ");
        body.put("durationMinutes", duration);
        body.put("passScore", passScore);
        return body;
    }

    private void moveDeadline(LocalDateTime deadline) {
        jdbcTemplate.update("UPDATE exam_attempts SET deadline_at = ?", Timestamp.valueOf(deadline));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Cookie cookie) throws Exception {
        return mockMvc.perform(request.cookie(cookie));
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private long course(String name, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO courses (name, price, status, created_at, updated_at, deleted_at) "
                + "VALUES (?, 100000, 'PUBLISHED', ?, ?, ?)", name, LocalDateTime.now(), LocalDateTime.now(), deletedAt);
        return jdbcTemplate.queryForObject("SELECT id FROM courses WHERE name = ?", Long.class, name);
    }

    private void enroll(long userId, long course, String status) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO course_enrollments (user_id, course_id, status, price, approved_at, "
                + "enrolled_at, updated_at) VALUES (?, ?, ?, 100000, ?, ?, ?)", userId, course, status, now, now, now);
    }

    private User user(String username, String roleCode) {
        if (!roleRepository.findByCode(roleCode).isPresent()) {
            Role role = new Role();
            role.setCode(roleCode);
            role.setName(roleCode);
            roleRepository.save(role);
        }
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
