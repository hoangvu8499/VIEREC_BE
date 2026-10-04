package com.vierec.modules.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.course.entity.Lesson;
import com.vierec.modules.course.repository.CourseRepository;
import com.vierec.modules.course.repository.LessonRepository;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockMvc does not parse multipart bodies itself, so this checks against the embedded Tomcat that a
 * {@code PUT multipart/form-data} request reaches the controller with its text fields and files.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LessonMultipartPutTomcatTest {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private JwtTokenProvider tokenProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"lesson_files", "files", "lessons", "courses", "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void putMultipartUpdatesTheLessonThroughTomcat() throws Exception {
        String token = transactionTemplate.execute(tx -> {
            Role role = roleRepository.findByCode(RoleCode.ADMIN).orElseGet(() -> {
                Role created = new Role();
                created.setCode(RoleCode.ADMIN);
                created.setName(RoleCode.ADMIN);
                return roleRepository.save(created);
            });
            User admin = User.builder().username("tomcatadmin").passwordHash("$2a$12$hash")
                    .firstName("A").lastName("B").status(UserStatus.ACTIVE).build();
            admin.addRole(role, null);
            return tokenProvider.generateAccessToken(new CustomUserDetails(userRepository.save(admin)));
        });
        Long lessonId = transactionTemplate.execute(tx -> {
            Course course = new Course();
            course.setName("Khoá");
            course.setDescription("Mô tả");
            course.setStatus(CourseStatus.PUBLISHED);
            Lesson lesson = new Lesson();
            lesson.setCourse(courseRepository.save(course));
            lesson.setTitle("Cũ");
            lesson.setInstructions("Cũ");
            lesson.setSortOrder(1);
            return lessonRepository.save(lesson).getId();
        });
        Long courseId = jdbcTemplate.queryForObject("SELECT course_id FROM lessons WHERE id = ?", Long.class,
                lessonId);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("title", "Tiêu đề mới");
        form.add("instructions", "Hướng dẫn mới");
        form.add("sortOrder", "3");
        form.add("documentFile", new ByteArrayResource("%PDF-1.4 moi".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "moi.pdf";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add(HttpHeaders.COOKIE, "access_token=" + token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/courses/" + courseId + "/lessons/" + lessonId, HttpMethod.PUT,
                new HttpEntity<>(form, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).path("data");
        assertThat(data.path("title").asText()).isEqualTo("Tiêu đề mới");
        assertThat(data.path("sortOrder").asInt()).isEqualTo(3);
        assertThat(data.path("files").get(0).path("originalName").asText()).isEqualTo("moi.pdf");
    }
}
