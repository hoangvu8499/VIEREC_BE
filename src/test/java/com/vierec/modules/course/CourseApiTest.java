package com.vierec.modules.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.course.dto.CourseRequest;
import com.vierec.modules.course.entity.CourseStatus;
import com.vierec.modules.file.service.FileCategory;
import com.vierec.modules.file.service.FileStorageService;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseApiTest {

    private static final String COURSES = "/api/v1/courses";
    private static final byte[] PDF = "%PDF-1.4 test document".getBytes(StandardCharsets.UTF_8);
    private static final String YOUTUBE_URL = "https://youtu.be/dQw4w9WgXcQ";
    private static final byte[] MP4 = "0123456789abcdefghij".getBytes(StandardCharsets.UTF_8);

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
    private FileStorageService fileStorageService;
    @Value("${app.upload.dir}")
    private String uploadDir;

    private Cookie admin;
    private Cookie trainee;
    private Long instructorId;

    @BeforeEach
    void setUp() {
        role(RoleCode.ADMIN);
        role(RoleCode.TRAINEE);
        admin = login(user("quantri", RoleCode.ADMIN, UserStatus.ACTIVE));
        trainee = login(user("hocvien", RoleCode.TRAINEE, UserStatus.ACTIVE));
        instructorId = user("giangvien", RoleCode.TRAINEE, UserStatus.ACTIVE).getId();
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"certificates", "course_enrollments", "lesson_files", "files", "lessons",
                "courses", "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        FileSystemUtils.deleteRecursively(Paths.get(uploadDir).toFile());
    }

    // ------------------------------------------------------------------ list

    @Test
    void listReturnsTenCoursesPerPageNewestFirst() throws Exception {
        for (int i = 1; i <= 12; i++) {
            createCourse(course("Khoá " + i, CourseStatus.PUBLISHED)).andExpect(status().isCreated());
        }

        mockMvc.perform(get(COURSES).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(10)))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Khoá 12"))
                .andExpect(jsonPath("$.data.content[0].instructorId").value(instructorId))
                .andExpect(jsonPath("$.data.content[0].instructorName").value("Họ giangvien"))
                .andExpect(jsonPath("$.data.content[0].createdByUsername").value("quantri"))
                .andExpect(jsonPath("$.data.content[0].lessonCount").value(0));

        mockMvc.perform(get(COURSES).param("page", "1").cookie(admin))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void listCountsLessons() throws Exception {
        long courseId = createdCourseId("Có bài học");
        createLesson(courseId, 1).andExpect(status().isCreated());
        createLesson(courseId, 2).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES).cookie(admin))
                .andExpect(jsonPath("$.data.content[0].lessonCount").value(2));
    }

    @Test
    void traineeSeesOnlyPublishedCoursesAndAdminCanFilterByStatus() throws Exception {
        createCourse(course("Nháp", CourseStatus.DRAFT)).andExpect(status().isCreated());
        createCourse(course("Đã mở", CourseStatus.PUBLISHED)).andExpect(status().isCreated());
        createCourse(course("Lưu trữ", CourseStatus.ARCHIVED)).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES).cookie(trainee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Đã mở")));
        // A trainee cannot widen the list by asking for another status.
        mockMvc.perform(get(COURSES).param("status", "DRAFT").cookie(trainee))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Đã mở")));

        mockMvc.perform(get(COURSES).cookie(admin))
                .andExpect(jsonPath("$.data.totalElements").value(3));
        mockMvc.perform(get(COURSES).param("status", "DRAFT").cookie(admin))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Nháp")));
    }

    @Test
    void listSearchesByName() throws Exception {
        createCourse(course("Phòng cháy chữa cháy", CourseStatus.PUBLISHED)).andExpect(status().isCreated());
        createCourse(course("Sơ cứu", CourseStatus.PUBLISHED)).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES).param("keyword", "CHÁY").cookie(admin))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Phòng cháy chữa cháy")));
    }

    @Test
    void listIsPublicButShowsAnonymousUsersOnlyPublishedCourses() throws Exception {
        createCourse(course("Nháp", CourseStatus.DRAFT)).andExpect(status().isCreated());
        createCourse(course("Đã mở", CourseStatus.PUBLISHED)).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Đã mở")));
        mockMvc.perform(get(COURSES).param("status", "DRAFT"))
                .andExpect(jsonPath("$.data.content[*].name", containsInAnyOrder("Đã mở")));
    }

    @Test
    void listRejectsNegativePage() throws Exception {
        mockMvc.perform(get(COURSES).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"));
    }

    @Test
    void createEndpointsNeedLogin() throws Exception {
        mockMvc.perform(post(COURSES).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(course("A", CourseStatus.DRAFT))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(COURSES + "/1/lessons").file(document("a.pdf")))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ create course

    @Test
    void createCourseReturnsTheNewRow() throws Exception {
        createCourse(course("  Phòng cháy  ", CourseStatus.DRAFT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("Phòng cháy"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.price").value(150_000))
                .andExpect(jsonPath("$.data.instructorUsername").value("giangvien"))
                .andExpect(jsonPath("$.data.createdByUsername").value("quantri"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM courses");
        assertThat(row.get("instructor_id")).isEqualTo(instructorId);
        assertThat(row.get("created_by")).isNotNull();
    }

    @Test
    void createCourseRequiresEveryField() throws Exception {
        mockMvc.perform(post(COURSES).cookie(admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("name", "description", "instructorId", "price", "status")));

        CourseRequest blank = course(" ", CourseStatus.DRAFT);
        blank.setDescription("   ");
        createCourse(blank)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "description")));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM courses", Long.class)).isZero();
    }

    @Test
    void createCourseRejectsUnknownStatusAsFieldError() throws Exception {
        String body = "{\"name\":\"A\",\"description\":\"B\",\"instructorId\":" + instructorId
                + ",\"status\":\"OPEN\"}";
        mockMvc.perform(post(COURSES).cookie(admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message", containsString("PUBLISHED")));
    }

    @Test
    void coursePriceMustBeAPositiveAmountOfDong() throws Exception {
        CourseRequest request = course("A", CourseStatus.DRAFT);
        request.setPrice(999L);
        createCourse(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));
        request.setPrice(1_000_000_001L);
        createCourse(request).andExpect(status().isBadRequest());
        request.setPrice(1_000L);
        createCourse(request).andExpect(status().isCreated());
    }

    @Test
    void createCourseChecksTheInstructor() throws Exception {
        CourseRequest request = course("A", CourseStatus.DRAFT);
        request.setInstructorId(999_999L);
        createCourse(request).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-202"));

        request.setInstructorId(user("dakhoa", RoleCode.TRAINEE, UserStatus.LOCKED).getId());
        createCourse(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VRC-400-201"));
    }

    @Test
    void onlyAdminsCreateCourses() throws Exception {
        mockMvc.perform(post(COURSES).cookie(trainee).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(course("A", CourseStatus.DRAFT))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ create lesson

    @Test
    void createLessonStoresTheDocumentOnServerAndKeepsTheYoutubeLink() throws Exception {
        long courseId = createdCourseId("Khoá");

        String body = createLesson(courseId, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Bài 1"))
                .andExpect(jsonPath("$.data.sortOrder").value(1))
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andExpect(jsonPath("$.data.files[0].fileType").value("DOCUMENT"))
                .andExpect(jsonPath("$.data.files[0].originalName").value("tai-lieu.pdf"))
                .andExpect(jsonPath("$.data.files[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.files[0].sizeBytes").value(PDF.length))
                .andExpect(jsonPath("$.data.videoUrl").value(YOUTUBE_URL))
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(body).path("data");
        String documentUrl = data.path("files").get(0).path("url").asText();
        assertThat(data.path("documentUrl").asText()).isEqualTo(documentUrl).startsWith("/api/v1/files/");

        // Bytes are on the server disk under app.upload.dir, at the relative path kept in files.url.
        for (Map<String, Object> row : jdbcTemplate.queryForList("SELECT url, size_bytes FROM files")) {
            Path stored = Paths.get(uploadDir).resolve((String) row.get("url"));
            assertThat(stored).exists();
            assertThat(Files.size(stored)).isEqualTo(((Number) row.get("size_bytes")).longValue());
            assertThat((String) row.get("url")).startsWith("lessons/").doesNotContain("tai-lieu");
        }

        mockMvc.perform(get(documentUrl).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("tai-lieu.pdf")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void videoUploadedBeforeYoutubeOnlyCanStillBeStreamedWithRangeRequests() throws Exception {
        long courseId = createdCourseId("Khoá");
        long lessonId = lessonData(createLesson(courseId, 1)).path("id").asLong();
        String videoUrl = legacyVideo(lessonId);

        mockMvc.perform(get(videoUrl).cookie(admin).header(HttpHeaders.RANGE, "bytes=0-4"))
                .andExpect(status().isPartialContent())
                .andExpect(content().bytes("01234".getBytes(StandardCharsets.UTF_8)));
        mockMvc.perform(get(videoUrl)).andExpect(status().isUnauthorized());
    }

    @Test
    void createLessonRequiresTextFieldsAndTheDocument() throws Exception {
        long courseId = createdCourseId("Khoá");

        mockMvc.perform(multipart(COURSES + "/" + courseId + "/lessons").cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "title", "instructions", "sortOrder", "documentFile")));

        // An empty file input still sends a part; it counts as missing.
        mockMvc.perform(lessonForm(courseId, "1")
                        .file(new MockMultipartFile("documentFile", "rong.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("documentFile"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value("rong.pdf"));

        mockMvc.perform(lessonForm(courseId, "abc").file(document("a.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sortOrder"))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid value"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isZero();
    }

    @Test
    void videoIsAnOptionalYoutubeLinkAndUploadsAreIgnored() throws Exception {
        long courseId = createdCourseId("Khoá");

        mockMvc.perform(lessonForm(courseId, "1").file(document("a.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andExpect(jsonPath("$.data.files[0].fileType").value("DOCUMENT"))
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());

        // A video file part from an old client is not a field any more: it is ignored, nothing is stored.
        mockMvc.perform(lessonForm(courseId, "2").file(document("a.pdf"))
                        .file(new MockMultipartFile("videoFile", "b.mp4", "video/mp4", MP4))
                        .param("videoUrl", "  " + YOUTUBE_URL + "  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andExpect(jsonPath("$.data.videoUrl").value(YOUTUBE_URL));

        assertThat(jdbcTemplate.queryForList("SELECT video_url FROM lessons ORDER BY sort_order", String.class))
                .containsExactly(null, YOUTUBE_URL);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isEqualTo(2);
        mockMvc.perform(get(COURSES + "/" + courseId).cookie(admin))
                .andExpect(jsonPath("$.data.lessons[1].videoUrl").value(YOUTUBE_URL));
    }

    @Test
    void lessonVideosAndFilesOpenOnlyAfterTheEnrollmentIsApproved() throws Exception {
        long courseId = createdCourseId("Khoá");
        JsonNode lesson = lessonData(mockMvc.perform(lessonForm(courseId, "1").file(document("tai-lieu.pdf"))
                .param("videoUrl", YOUTUBE_URL)));
        String fileUrl = lesson.path("files").get(0).path("url").asText();
        String detail = COURSES + "/" + courseId;

        // Visitors only get the outline.
        mockMvc.perform(get(detail))
                .andExpect(jsonPath("$.data.lessons[0].title").value("Bài 1"))
                .andExpect(jsonPath("$.data.lessons[0].videoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.lessons[0].documentUrl").doesNotExist())
                .andExpect(jsonPath("$.data.lessons[0].files", hasSize(0)));

        // So do learners whose payment is not approved yet.
        String body = mockMvc.perform(post(detail + "/enrollments").cookie(trainee))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long enrollmentId = objectMapper.readTree(body).path("data").path("id").asLong();
        mockMvc.perform(get(detail).cookie(trainee))
                .andExpect(jsonPath("$.data.myEnrollmentStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.lessons[0].videoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.lessons[0].files", hasSize(0)));
        mockMvc.perform(get(fileUrl).cookie(trainee))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-002"));

        mockMvc.perform(put(detail + "/enrollments/" + enrollmentId).cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ENROLLED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(detail).cookie(trainee))
                .andExpect(jsonPath("$.data.lessons[0].videoUrl").value(YOUTUBE_URL))
                .andExpect(jsonPath("$.data.lessons[0].files", hasSize(1)));
        mockMvc.perform(get(fileUrl).cookie(trainee)).andExpect(status().isOk());
    }

    @Test
    void videoUrlMustBeAYoutubeLink() throws Exception {
        long courseId = createdCourseId("Khoá");
        for (String bad : new String[] {"youtu.be/dQw4w9WgXcQ", "javascript:alert(1)", "https://a b.com",
                "https://drive.google.com/file/d/xyz/view", "https://www.youtube.com/watch?v=ngan",
                "https://vimeo.com/123456"}) {
            mockMvc.perform(lessonForm(courseId, "1").file(document("a.pdf")).param("videoUrl", bad))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("videoUrl"));
        }
        mockMvc.perform(lessonForm(courseId, "1").file(document("a.pdf")).param("videoUrl", "   "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist());
        assertThat(jdbcTemplate.queryForObject("SELECT video_url FROM lessons", String.class)).isNull();
    }

    @Test
    void createLessonRejectsWrongFileTypesWithoutStoringAnything() throws Exception {
        long courseId = createdCourseId("Khoá");

        mockMvc.perform(lessonForm(courseId, "1").file(document("bai.mp4")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-301"));
        mockMvc.perform(lessonForm(courseId, "1").file(document("bai.exe")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-301"))
                .andExpect(jsonPath("$.message", containsString("pdf")));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isZero();
        assertThat(Paths.get(uploadDir)).doesNotExist();
    }

    @Test
    void duplicateSortOrderIsRejectedBeforeAnyFileIsStored() throws Exception {
        long courseId = createdCourseId("Khoá");
        createLesson(courseId, 1).andExpect(status().isCreated());
        createLesson(courseId, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-201"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isEqualTo(1);
        assertThat(storedFileCount()).isEqualTo(1);
    }

    @Test
    void rolledBackTransactionDeletesTheWrittenFile() throws Exception {
        transactionTemplate.execute(tx -> {
            fileStorageService.store(document("a.pdf"), FileCategory.DOCUMENT, "lessons");
            tx.setRollbackOnly();
            return null;
        });

        assertThat(storedFileCount()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isZero();
    }

    @Test
    void createLessonNeedsAnExistingCourseAndAnAdmin() throws Exception {
        createLesson(999_999L, 1).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-201"));

        long courseId = createdCourseId("Khoá");
        mockMvc.perform(multipart(COURSES + "/" + courseId + "/lessons").file(document("a.pdf"))
                        .param("title", "T").param("instructions", "I").param("sortOrder", "1").cookie(trainee))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownFileIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/files/999999").cookie(admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-301"));
    }

    // ------------------------------------------------------------------ course detail

    @Test
    void detailIsPublicAndListsLessonsInCourseOrder() throws Exception {
        long courseId = createdCourseId("Khoá");
        createLesson(courseId, 2).andExpect(status().isCreated());
        createLesson(courseId, 1).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES + "/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lessons", hasSize(2)))
                .andExpect(jsonPath("$.data.lessons[0].sortOrder").value(1));
        mockMvc.perform(get(COURSES + "/" + courseId).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(courseId))
                .andExpect(jsonPath("$.data.name").value("Khoá"))
                .andExpect(jsonPath("$.data.instructorName").value("Họ giangvien"))
                .andExpect(jsonPath("$.data.createdByUsername").value("quantri"))
                .andExpect(jsonPath("$.data.lessons", hasSize(2)))
                .andExpect(jsonPath("$.data.lessons[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data.lessons[1].sortOrder").value(2))
                .andExpect(jsonPath("$.data.lessons[0].files", hasSize(1)))
                .andExpect(jsonPath("$.data.lessons[0].files[0].fileType").value("DOCUMENT"))
                .andExpect(jsonPath("$.data.lessons[0].videoUrl").value(YOUTUBE_URL));
    }

    @Test
    void detailOfUnpublishedCourseIsOnlyVisibleToAdmins() throws Exception {
        String body = createCourse(course("Nháp", CourseStatus.DRAFT)).andReturn().getResponse().getContentAsString();
        long courseId = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(get(COURSES + "/" + courseId))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-201"));
        mockMvc.perform(get(COURSES + "/" + courseId).cookie(trainee)).andExpect(status().isNotFound());
        mockMvc.perform(get(COURSES + "/" + courseId).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.lessons", hasSize(0)));
    }

    // ------------------------------------------------------------------ update / delete course

    @Test
    void updateCourseReplacesEveryField() throws Exception {
        long courseId = createdCourseId("Cũ");
        Long otherInstructor = user("giangvien2", RoleCode.TRAINEE, UserStatus.ACTIVE).getId();
        CourseRequest request = course("  Mới  ", CourseStatus.ARCHIVED);
        request.setDescription("Mô tả mới");
        request.setInstructorId(otherInstructor);
        request.setPrice(2_500_000L);

        updateCourse(courseId, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Mới"))
                .andExpect(jsonPath("$.data.description").value("Mô tả mới"))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.data.price").value(2_500_000))
                .andExpect(jsonPath("$.data.instructorUsername").value("giangvien2"))
                .andExpect(jsonPath("$.data.createdByUsername").value("quantri"));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM courses WHERE id = ?", courseId);
        assertThat(row.get("name")).isEqualTo("Mới");
        assertThat(row.get("instructor_id")).isEqualTo(otherInstructor);
        assertThat(((Number) row.get("price")).longValue()).isEqualTo(2_500_000L);
    }

    @Test
    void updateCourseValidatesLikeCreate() throws Exception {
        long courseId = createdCourseId("Khoá");
        mockMvc.perform(put(COURSES + "/" + courseId).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("name", "description", "instructorId", "price", "status")));
        updateCourse(999_999L, course("A", CourseStatus.DRAFT))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-201"));
    }

    @Test
    void updateCourseOnlyRequiresANewInstructorToBeActive() throws Exception {
        long courseId = createdCourseId("Khoá");
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE id = ?", instructorId);
        updateCourse(courseId, course("Giữ giảng viên", CourseStatus.PUBLISHED)).andExpect(status().isOk());

        CourseRequest request = course("Đổi giảng viên", CourseStatus.PUBLISHED);
        request.setInstructorId(user("dakhoa", RoleCode.TRAINEE, UserStatus.LOCKED).getId());
        updateCourse(courseId, request)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VRC-400-201"));
    }

    @Test
    void deleteCourseIsSoft() throws Exception {
        long courseId = createdCourseId("Sẽ xoá");
        createLesson(courseId, 1).andExpect(status().isCreated());

        mockMvc.perform(delete(COURSES + "/" + courseId).cookie(admin)).andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM courses WHERE id = ?", Object.class, courseId))
                .isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lessons", Long.class)).isEqualTo(1);
        mockMvc.perform(get(COURSES).cookie(admin)).andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get(COURSES + "/" + courseId).cookie(admin)).andExpect(status().isNotFound());
        mockMvc.perform(delete(COURSES + "/" + courseId).cookie(admin)).andExpect(status().isNotFound());
        updateCourse(courseId, course("A", CourseStatus.DRAFT)).andExpect(status().isNotFound());
        createLesson(courseId, 2).andExpect(status().isNotFound());
    }

    @Test
    void onlyAdminsUpdateAndDeleteCourses() throws Exception {
        long courseId = createdCourseId("Khoá");
        String json = objectMapper.writeValueAsString(course("A", CourseStatus.DRAFT));

        mockMvc.perform(put(COURSES + "/" + courseId).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(COURSES + "/" + courseId).cookie(trainee).contentType(MediaType.APPLICATION_JSON)
                .content(json)).andExpect(status().isForbidden());
        mockMvc.perform(delete(COURSES + "/" + courseId)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(COURSES + "/" + courseId).cookie(trainee)).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ update / delete lesson

    @Test
    void updateLessonWithoutFilesKeepsTheCurrentFiles() throws Exception {
        long courseId = createdCourseId("Khoá");
        JsonNode created = lessonData(createLesson(courseId, 1));

        String body = mockMvc.perform(lessonUpdate(courseId, created.path("id").asLong(), "5")
                        .file(new MockMultipartFile("documentFile", "", "application/octet-stream", new byte[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Sửa 5"))
                .andExpect(jsonPath("$.data.instructions").value("Hướng dẫn mới"))
                .andExpect(jsonPath("$.data.sortOrder").value(5))
                .andReturn().getResponse().getContentAsString();

        JsonNode updated = objectMapper.readTree(body).path("data");
        assertThat(updated.path("files")).isEqualTo(created.path("files"));
        assertThat(updated.path("documentUrl")).isEqualTo(created.path("documentUrl"));
    }

    @Test
    void updateLessonReplacesTheDocumentAndKeepsTheOldFile() throws Exception {
        long courseId = createdCourseId("Khoá");
        JsonNode created = lessonData(createLesson(courseId, 1));
        long lessonId = created.path("id").asLong();

        String withDocument = mockMvc.perform(lessonUpdate(courseId, lessonId, "1").file(document("moi.docx")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andExpect(jsonPath("$.data.files[0].originalName").value("moi.docx"))
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(withDocument).path("data");
        assertThat(data.path("documentUrl").asText()).isEqualTo(data.path("files").get(0).path("url").asText());
        assertThat(data.path("files").get(0).path("fileId"))
                .isNotEqualTo(created.path("files").get(0).path("fileId"));
        // The replaced document is unlinked but kept, on disk and in files.
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lesson_files", Long.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isEqualTo(2);
        assertThat(storedFileCount()).isEqualTo(2);
    }

    @Test
    void updateLessonReplacesTheVideoLinkAndCanRemoveAnOldVideoFile() throws Exception {
        long courseId = createdCourseId("Khoá");
        long lessonId = lessonData(createLesson(courseId, 1)).path("id").asLong();
        legacyVideo(lessonId);

        mockMvc.perform(lessonUpdate(courseId, lessonId, "1").param("videoUrl", "https://youtu.be/aaaaaaaaaaa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoUrl").value("https://youtu.be/aaaaaaaaaaa"))
                .andExpect(jsonPath("$.data.files", hasSize(2)));

        // Leaving videoUrl out clears it; removeVideo unlinks the uploaded video but keeps the file.
        mockMvc.perform(lessonUpdate(courseId, lessonId, "1").param("removeVideo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.files", hasSize(1)))
                .andExpect(jsonPath("$.data.files[0].fileType").value("DOCUMENT"));
        assertThat(jdbcTemplate.queryForObject("SELECT video_url FROM lessons", String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM files", Long.class)).isEqualTo(2);
    }

    @Test
    void updateLessonChecksSortOrderFileTypeAndRequiredFields() throws Exception {
        long courseId = createdCourseId("Khoá");
        long lessonId = lessonData(createLesson(courseId, 1)).path("id").asLong();
        createLesson(courseId, 2).andExpect(status().isCreated());

        mockMvc.perform(lessonUpdate(courseId, lessonId, "2"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VRC-409-201"));
        mockMvc.perform(lessonUpdate(courseId, lessonId, "1").file(document("sai.exe")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VRC-400-301"));
        mockMvc.perform(multipart(HttpMethod.PUT, COURSES + "/" + courseId + "/lessons/" + lessonId).cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("title", "instructions", "sortOrder")));
        assertThat(storedFileCount()).isEqualTo(2);
    }

    @Test
    void lessonMustBelongToALiveCourse() throws Exception {
        long courseId = createdCourseId("Khoá");
        long otherCourseId = createdCourseId("Khoá khác");
        long lessonId = lessonData(createLesson(courseId, 1)).path("id").asLong();

        mockMvc.perform(lessonUpdate(otherCourseId, lessonId, "1"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-203"));
        mockMvc.perform(delete(COURSES + "/" + otherCourseId + "/lessons/" + lessonId).cookie(admin))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-203"));

        mockMvc.perform(delete(COURSES + "/" + courseId).cookie(admin)).andExpect(status().isNoContent());
        mockMvc.perform(lessonUpdate(courseId, lessonId, "1"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("VRC-404-201"));
    }

    @Test
    void deleteLessonIsSoftAndFreesItsSortOrder() throws Exception {
        long courseId = createdCourseId("Khoá");
        long lessonId = lessonData(createLesson(courseId, 1)).path("id").asLong();

        mockMvc.perform(delete(COURSES + "/" + courseId + "/lessons/" + lessonId).cookie(admin))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM lessons WHERE id = ?", Object.class, lessonId))
                .isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM lesson_files", Long.class)).isEqualTo(1);
        mockMvc.perform(get(COURSES + "/" + courseId)).andExpect(jsonPath("$.data.lessons", hasSize(0)));
        mockMvc.perform(get(COURSES).cookie(admin)).andExpect(jsonPath("$.data.content[0].lessonCount").value(0));
        mockMvc.perform(delete(COURSES + "/" + courseId + "/lessons/" + lessonId).cookie(admin))
                .andExpect(status().isNotFound());
        createLesson(courseId, 1).andExpect(status().isCreated());
    }

    @Test
    void onlyAdminsUpdateAndDeleteLessons() throws Exception {
        long courseId = createdCourseId("Khoá");
        long lessonId = lessonData(createLesson(courseId, 1)).path("id").asLong();

        mockMvc.perform(lessonUpdate(courseId, lessonId, "1", trainee)).andExpect(status().isForbidden());
        mockMvc.perform(delete(COURSES + "/" + courseId + "/lessons/" + lessonId).cookie(trainee))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(COURSES + "/" + courseId + "/lessons/" + lessonId))
                .andExpect(status().isUnauthorized());
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

    private User user(String username, String roleCode, UserStatus status) {
        return transactionTemplate.execute(tx -> {
            User user = User.builder().username(username).passwordHash("$2a$12$hash")
                    .firstName(username).lastName("Họ").status(status).build();
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

    private CourseRequest course(String name, CourseStatus status) {
        return CourseRequest.builder().name(name).description("Mô tả " + name)
                .instructorId(instructorId).price(150_000L).status(status).build();
    }

    private ResultActions createCourse(CourseRequest request) throws Exception {
        return mockMvc.perform(post(COURSES).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private long createdCourseId(String name) throws Exception {
        String body = createCourse(course(name, CourseStatus.PUBLISHED)).andReturn().getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private ResultActions createLesson(long courseId, int sortOrder) throws Exception {
        return mockMvc.perform(lessonForm(courseId, String.valueOf(sortOrder))
                .file(document("tai-lieu.pdf")).param("videoUrl", YOUTUBE_URL));
    }

    private ResultActions updateCourse(long courseId, CourseRequest request) throws Exception {
        return mockMvc.perform(put(COURSES + "/" + courseId).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private JsonNode lessonData(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data");
    }

    /** PUT form with the text fields only, as admin; add file parts to replace files. */
    private MockMultipartHttpServletRequestBuilder lessonUpdate(long courseId, long lessonId, String sortOrder) {
        return lessonUpdate(courseId, lessonId, sortOrder, admin);
    }

    private MockMultipartHttpServletRequestBuilder lessonUpdate(long courseId, long lessonId, String sortOrder,
                                                                Cookie caller) {
        MockMultipartHttpServletRequestBuilder builder =
                multipart(HttpMethod.PUT, COURSES + "/" + courseId + "/lessons/" + lessonId);
        builder.param("title", "Sửa " + sortOrder).param("instructions", "Hướng dẫn mới")
                .param("sortOrder", sortOrder).cookie(caller);
        return builder;
    }

    private MockMultipartHttpServletRequestBuilder lessonForm(long courseId, String sortOrder) {
        MockMultipartHttpServletRequestBuilder builder = multipart(COURSES + "/" + courseId + "/lessons");
        builder.param("title", "Bài " + sortOrder).param("instructions", "Đọc tài liệu rồi xem video")
                .param("sortOrder", sortOrder).cookie(admin);
        return builder;
    }

    private long storedFileCount() throws Exception {
        Path root = Paths.get(uploadDir);
        if (!Files.exists(root)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private static MockMultipartFile document(String name) {
        return new MockMultipartFile("documentFile", name, "application/pdf", PDF);
    }

    /** A video file linked the way lessons had them before videos became YouTube only; returns its URL. */
    private String legacyVideo(long lessonId) throws Exception {
        Path stored = Paths.get(uploadDir).resolve("lessons/cu.mp4");
        Files.createDirectories(stored.getParent());
        Files.write(stored, MP4);
        jdbcTemplate.update("INSERT INTO files (url, original_name, content_type, size_bytes, created_at) "
                + "VALUES ('lessons/cu.mp4', 'cu.mp4', 'video/mp4', ?, ?)", MP4.length, LocalDateTime.now());
        long fileId = jdbcTemplate.queryForObject("SELECT id FROM files WHERE url = 'lessons/cu.mp4'", Long.class);
        jdbcTemplate.update("INSERT INTO lesson_files (lesson_id, file_id, file_type, sort_order) "
                + "VALUES (?, ?, 'VIDEO', 2)", lessonId, fileId);
        return "/api/v1/files/" + fileId;
    }
}
