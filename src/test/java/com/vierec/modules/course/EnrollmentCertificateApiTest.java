package com.vierec.modules.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.course.dto.CourseRequest;
import com.vierec.modules.course.entity.CourseStatus;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.FileSystemUtils;

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Profile self-service, enrollments and certificates. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentCertificateApiTest {

    private static final String COURSES = "/api/v1/courses";
    private static final String ME = "/api/v1/auth/me";
    private static final String PASSWORD = "Matkhau@123";
    private static final byte[] PDF = "%PDF-1.4 certificate".getBytes(StandardCharsets.UTF_8);

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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${app.upload.dir}")
    private String uploadDir;

    private Cookie admin;
    private Cookie trainee;
    private Cookie otherTrainee;
    private Long traineeId;
    private Long instructorId;

    @BeforeEach
    void setUp() {
        role(RoleCode.ADMIN);
        role(RoleCode.TRAINEE);
        admin = login(user("quantri", RoleCode.ADMIN, "001"));
        User traineeUser = user("hocvien", RoleCode.TRAINEE, "002");
        traineeId = traineeUser.getId();
        trainee = login(traineeUser);
        otherTrainee = login(user("hocvienkhac", RoleCode.TRAINEE, "003"));
        instructorId = user("giangvien", RoleCode.TRAINEE, "004").getId();
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"certificates", "course_enrollments", "lesson_files", "files", "lessons",
                "courses", "user_roles", "users", "businesses"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        FileSystemUtils.deleteRecursively(Paths.get(uploadDir).toFile());
    }

    // ------------------------------------------------------------------ profile

    @Test
    void traineeUpdatesOwnProfileButNotStatusOrRoles() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Văn An");
        body.put("email", "AN@Example.com");
        body.put("address", "12 Nguyễn Huệ");
        body.put("status", "LOCKED");
        body.put("roles", Collections.singletonList(RoleCode.ADMIN));

        updateMe(trainee, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Văn An"))
                .andExpect(jsonPath("$.data.lastName").value("Họ"))
                .andExpect(jsonPath("$.data.email").value("an@example.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder(RoleCode.TRAINEE)));
    }

    @Test
    void profileUpdateValidatesAndChecksDuplicates() throws Exception {
        updateMe(trainee, Collections.singletonMap("cccd", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[0].field").value("cccd"));
        updateMe(trainee, Collections.singletonMap("cccd", "099000000001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-104"));
        updateMe(trainee, Collections.singletonMap("phoneNumber", "0990000001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-103"));
    }

    @Test
    void profileUpdateNeedsLogin() throws Exception {
        mockMvc.perform(put(ME).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordChecksTheCurrentPassword() throws Exception {
        changePassword("Sai@12345", "Moi@12345")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("VRC-401-002"));
        changePassword(PASSWORD, "khongdacbiet")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));

        changePassword(PASSWORD, "Moi@12345").andExpect(status().isNoContent());
        String hash = userRepository.findById(traineeId).orElseThrow(IllegalStateException::new).getPasswordHash();
        assertThat(passwordEncoder.matches("Moi@12345", hash)).isTrue();
    }

    // ------------------------------------------------------------------ enrollments

    @Test
    void traineeEnrollsCancelsAndEnrollsAgain() throws Exception {
        long courseId = courseId("Phòng cháy", CourseStatus.PUBLISHED);

        enroll(courseId, trainee)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.courseName").value("Phòng cháy"))
                .andExpect(jsonPath("$.data.instructorName").value("Họ giangvien"))
                .andExpect(jsonPath("$.data.lessonCount").value(0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.completedAt").value(nullValue()));
        enroll(courseId, trainee)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-202"));

        cancel(courseId, trainee).andExpect(status().isNoContent());
        cancel(courseId, trainee)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-204"));

        // The unique (user, course) row is reused.
        enroll(courseId, trainee)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_enrollments", Long.class)).isEqualTo(1);
    }

    @Test
    void onlyPublishedCoursesCanBeEnrolled() throws Exception {
        long draft = courseId("Nháp", CourseStatus.DRAFT);
        enroll(draft, trainee)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-201"));
        enroll(999_999L, trainee).andExpect(status().isNotFound());
        mockMvc.perform(post(COURSES + "/" + draft + "/enrollments")).andExpect(status().isUnauthorized());
    }

    @Test
    void courseDetailAndListShowTheCallersEnrollmentStatus() throws Exception {
        long courseId = courseId("Sơ cứu", CourseStatus.PUBLISHED);
        enroll(courseId, trainee).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES + "/" + courseId).cookie(trainee))
                .andExpect(jsonPath("$.data.myEnrollmentStatus").value("PENDING"));
        mockMvc.perform(get(COURSES).cookie(trainee))
                .andExpect(jsonPath("$.data.content[0].myEnrollmentStatus").value("PENDING"));
        mockMvc.perform(get(COURSES + "/" + courseId).cookie(otherTrainee))
                .andExpect(jsonPath("$.data.myEnrollmentStatus").value(nullValue()));
        mockMvc.perform(get(COURSES + "/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myEnrollmentStatus").value(nullValue()));
    }

    @Test
    void courseListCanLeaveOutTheCoursesTheCallerAlreadyLearns() throws Exception {
        long pending = courseId("Chờ duyệt", CourseStatus.PUBLISHED);
        long learning = courseId("Đang học", CourseStatus.PUBLISHED);
        long completed = courseId("Đã xong", CourseStatus.PUBLISHED);
        long cancelled = courseId("Đã huỷ", CourseStatus.PUBLISHED);
        courseId("Chưa đăng ký", CourseStatus.PUBLISHED);
        enroll(pending, trainee).andExpect(status().isCreated());
        setStatus(learning, enrollmentId(learning, trainee), "ENROLLED", admin).andExpect(status().isOk());
        setStatus(completed, enrollmentId(completed, trainee), "COMPLETED", admin).andExpect(status().isOk());
        enroll(cancelled, trainee).andExpect(status().isCreated());
        cancel(cancelled, trainee).andExpect(status().isNoContent());

        mockMvc.perform(get(COURSES).param("excludeLearning", "true").cookie(trainee))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[*].name",
                        containsInAnyOrder("Chờ duyệt", "Đã huỷ", "Chưa đăng ký")));
        mockMvc.perform(get(COURSES).cookie(trainee)).andExpect(jsonPath("$.data.totalElements").value(5));
        mockMvc.perform(get(COURSES).param("excludeLearning", "true").cookie(otherTrainee))
                .andExpect(jsonPath("$.data.totalElements").value(5));
        mockMvc.perform(get(COURSES).param("excludeLearning", "true"))
                .andExpect(jsonPath("$.data.totalElements").value(5));
    }

    @Test
    void myEnrollmentsHideCancelledOnesUnlessAskedFor() throws Exception {
        long first = courseId("Khoá 1", CourseStatus.PUBLISHED);
        long second = courseId("Khoá 2", CourseStatus.PUBLISHED);
        long third = courseId("Khoá 3", CourseStatus.PUBLISHED);
        enroll(first, trainee).andExpect(status().isCreated());
        enroll(second, trainee).andExpect(status().isCreated());
        enroll(third, trainee).andExpect(status().isCreated());
        cancel(third, trainee).andExpect(status().isNoContent());
        enroll(first, otherTrainee).andExpect(status().isCreated());

        mockMvc.perform(get(ME + "/enrollments").cookie(trainee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].courseName", containsInAnyOrder("Khoá 1", "Khoá 2")));
        mockMvc.perform(get(ME + "/enrollments").param("status", "CANCELLED").cookie(trainee))
                .andExpect(jsonPath("$.data.content[*].courseName", containsInAnyOrder("Khoá 3")));
        mockMvc.perform(get(ME + "/enrollments").param("size", "1").cookie(trainee))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        mockMvc.perform(get(ME + "/enrollments").param("size", "101").cookie(trainee))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminListsEnrollmentsAndMarksThemCompleted() throws Exception {
        long courseId = courseId("Thoát hiểm", CourseStatus.PUBLISHED);
        long enrollmentId = enrollmentId(courseId, trainee);
        enroll(courseId, otherTrainee).andExpect(status().isCreated());

        mockMvc.perform(get(COURSES + "/" + courseId + "/enrollments").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].username", containsInAnyOrder("hocvien", "hocvienkhac")));
        mockMvc.perform(get(COURSES + "/" + courseId + "/enrollments").cookie(trainee))
                .andExpect(status().isForbidden());

        setStatus(courseId, enrollmentId, "COMPLETED", trainee).andExpect(status().isForbidden());
        setStatus(courseId, enrollmentId, "COMPLETED", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());
        setStatus(courseId, 999_999L, "COMPLETED", admin)
                .andExpect(jsonPath("$.code").value("VRC-404-204"));

        cancel(courseId, trainee)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-203"));
        enroll(courseId, trainee)
                .andExpect(jsonPath("$.code").value("VRC-409-202"));
    }

    @Test
    void adminSearchesEnrollmentsOfEveryCourse() throws Exception {
        long fire = courseId("Phòng cháy", CourseStatus.PUBLISHED);
        long aid = courseId("Sơ cứu", CourseStatus.PUBLISHED);
        enroll(fire, trainee).andExpect(status().isCreated());
        long approved = enrollmentId(aid, otherTrainee);
        setStatus(aid, approved, "ENROLLED", admin).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/enrollments").param("status", "PENDING").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Phòng cháy"))
                .andExpect(jsonPath("$.data.content[0].username").value("hocvien"));
        mockMvc.perform(get("/api/v1/enrollments").cookie(admin))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/enrollments").param("keyword", " sơ CỨU ").cookie(admin))
                .andExpect(jsonPath("$.data.content[*].username", containsInAnyOrder("hocvienkhac")));
        mockMvc.perform(get("/api/v1/enrollments").param("keyword", "họ hocvienkhac").cookie(admin))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/enrollments").cookie(trainee)).andExpect(status().isForbidden());
    }

    @Test
    void revenueCountsThePriceOfApprovedEnrollmentsWhenTheLearnerAsked() throws Exception {
        long courseId = courseId("Phòng cháy", CourseStatus.PUBLISHED);
        long first = enrollmentId(courseId, trainee);
        long second = enrollmentId(courseId, otherTrainee);
        mockMvc.perform(get("/api/v1/enrollments").cookie(admin))
                .andExpect(jsonPath("$.data.content[*].price", containsInAnyOrder(250_000, 250_000)))
                .andExpect(jsonPath("$.data.content[0].approvedAt").value(nullValue()));
        revenue().andExpect(jsonPath("$.data.month.amount").value(0));

        // A price change does not touch what learners who already asked were told to transfer.
        jdbcTemplate.update("UPDATE courses SET price = 400000 WHERE id = ?", courseId);
        setStatus(courseId, first, "ENROLLED", admin)
                .andExpect(jsonPath("$.data.price").value(250_000))
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty());
        setStatus(courseId, second, "COMPLETED", admin).andExpect(status().isOk());

        LocalDate today = LocalDate.now();
        revenue()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.week.amount").value(500_000))
                .andExpect(jsonPath("$.data.week.enrollments").value(2))
                .andExpect(jsonPath("$.data.month.from").value(today.withDayOfMonth(1).toString()))
                .andExpect(jsonPath("$.data.year.from").value(today.withDayOfYear(1).toString()))
                .andExpect(jsonPath("$.data.year.amount").value(500_000));

        setStatus(courseId, first, "CANCELLED", admin).andExpect(jsonPath("$.data.approvedAt").value(nullValue()));
        jdbcTemplate.update("UPDATE course_enrollments SET approved_at = ? WHERE id = ?",
                today.minusYears(1).atStartOfDay(), second);
        revenue().andExpect(jsonPath("$.data.year.amount").value(0));
        mockMvc.perform(get("/api/v1/enrollments/revenue").cookie(trainee)).andExpect(status().isForbidden());
    }

    @Test
    void monthlyRevenueListsWhoPaidForApprovedEnrollmentsOnly() throws Exception {
        long fire = courseId("Phòng cháy", CourseStatus.PUBLISHED);
        long firstAid = courseId("Sơ cứu", CourseStatus.PUBLISHED);
        long mine = enrollmentId(fire, trainee);
        long other = enrollmentId(fire, otherTrainee);
        long otherFirstAid = enrollmentId(firstAid, otherTrainee);
        enrollmentId(firstAid, trainee); // stays PENDING: not paid for yet
        setStatus(fire, mine, "ENROLLED", admin).andExpect(status().isOk());
        setStatus(fire, other, "COMPLETED", admin).andExpect(status().isOk());
        setStatus(firstAid, otherFirstAid, "ENROLLED", admin).andExpect(status().isOk());
        YearMonth thisMonth = YearMonth.now();
        jdbcTemplate.update("UPDATE course_enrollments SET approved_at = ? WHERE id = ?",
                thisMonth.minusMonths(1).atDay(15).atStartOfDay(), otherFirstAid);

        monthlyRevenue(thisMonth.toString(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(thisMonth.atDay(1).toString()))
                .andExpect(jsonPath("$.data.to").value(thisMonth.atEndOfMonth().toString()))
                .andExpect(jsonPath("$.data.amount").value(500_000))
                .andExpect(jsonPath("$.data.enrollments").value(2))
                .andExpect(jsonPath("$.data.matchedAmount").value(500_000))
                .andExpect(jsonPath("$.data.courses.length()").value(1))
                .andExpect(jsonPath("$.data.courses[0].courseName").value("Phòng cháy"))
                .andExpect(jsonPath("$.data.courses[0].amount").value(500_000))
                .andExpect(jsonPath("$.data.items.totalElements").value(2))
                .andExpect(jsonPath("$.data.items.content[0].id").value(other))
                .andExpect(jsonPath("$.data.items.content[0].username").value("hocvienkhac"))
                .andExpect(jsonPath("$.data.items.content[0].price").value(250_000))
                .andExpect(jsonPath("$.data.items.content[1].id").value(mine));

        // The keyword narrows the list and its sum, not the month totals.
        monthlyRevenue(thisMonth.toString(), "HOCVIENKHAC")
                .andExpect(jsonPath("$.data.amount").value(500_000))
                .andExpect(jsonPath("$.data.matchedAmount").value(250_000))
                .andExpect(jsonPath("$.data.items.content.length()").value(1))
                .andExpect(jsonPath("$.data.items.content[0].id").value(other));

        monthlyRevenue(thisMonth.minusMonths(1).toString(), null)
                .andExpect(jsonPath("$.data.amount").value(250_000))
                .andExpect(jsonPath("$.data.courses[0].courseName").value("Sơ cứu"))
                .andExpect(jsonPath("$.data.items.content[0].id").value(otherFirstAid));
        mockMvc.perform(get("/api/v1/enrollments/revenue/monthly").cookie(admin))
                .andExpect(jsonPath("$.data.from").value(thisMonth.atDay(1).toString()));

        monthlyRevenue("2026-13", null).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/enrollments/revenue/monthly").cookie(trainee))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ certificates

    @Test
    void certificateNeedsACompletedEnrollmentAndIsIssuedOnce() throws Exception {
        long courseId = courseId("PCCC", CourseStatus.PUBLISHED);
        long enrollmentId = enrollmentId(courseId, trainee);

        issue(courseId, enrollmentId, pdf("chung-chi.pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-401"));

        setStatus(courseId, enrollmentId, "COMPLETED", admin).andExpect(status().isOk());
        issue(courseId, enrollmentId, new MockMultipartFile("file", "chung-chi.docx", "application/msword", PDF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-301"));
        mockMvc.perform(multipart(certificateUrl(courseId, enrollmentId)).cookie(admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("file"));

        issue(courseId, enrollmentId, pdf("chung-chi.pdf"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", matchesPattern("VRC-\\d{4}-[A-Z2-9]{8}")))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.enrollmentId").value(enrollmentId))
                .andExpect(jsonPath("$.data.courseName").value("PCCC"))
                .andExpect(jsonPath("$.data.fullName").value("Họ hocvien"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-01-01"))
                .andExpect(jsonPath("$.data.cccd").value("099000000002"))
                .andExpect(jsonPath("$.data.fileUrl", matchesPattern("/api/v1/files/\\d+")));

        issue(courseId, enrollmentId, pdf("lan-2.pdf"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-401"));
        setStatus(courseId, enrollmentId, "ENROLLED", admin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-402"));
        issue(courseId, enrollmentId, pdf("chung-chi.pdf"), trainee).andExpect(status().isForbidden());
    }

    @Test
    void certificateIsListedVerifiedPubliclyAndDownloadableOnlyByOwnerAndAdmins() throws Exception {
        JsonNode certificate = issuedCertificate();
        String code = certificate.path("code").asText();
        String fileUrl = certificate.path("fileUrl").asText();

        mockMvc.perform(get(ME + "/certificates").cookie(trainee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].code").value(code));
        mockMvc.perform(get(ME + "/certificates").cookie(otherTrainee))
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        mockMvc.perform(get("/api/v1/certificates/" + code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.fullName").value("Họ hocvien"))
                .andExpect(jsonPath("$.data.courseName").value("PCCC"))
                .andExpect(jsonPath("$.data.cccdMasked").value("0990******02"))
                .andExpect(jsonPath("$.data.cccd").doesNotExist())
                .andExpect(jsonPath("$.data.dateOfBirth").doesNotExist())
                .andExpect(jsonPath("$.data.fileUrl").doesNotExist());
        mockMvc.perform(get("/api/v1/certificates/VRC-2026-KHONGCO"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-401"));

        mockMvc.perform(get(fileUrl).cookie(trainee)).andExpect(status().isOk());
        mockMvc.perform(get(fileUrl).cookie(admin)).andExpect(status().isOk());
        mockMvc.perform(get(fileUrl).cookie(otherTrainee))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("VRC-403-002"));
        mockMvc.perform(get(fileUrl)).andExpect(status().isUnauthorized());
    }

    @Test
    void ownerAndManagersOfTheirBusinessSeeTheCertificateAndDownloadIt() throws Exception {
        JsonNode certificate = issuedCertificate();
        String code = certificate.path("code").asText();
        String fileUrl = certificate.path("fileUrl").asText();

        mockMvc.perform(get(ME + "/certificates/" + code.toLowerCase()).cookie(trainee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.userId").value(traineeId))
                .andExpect(jsonPath("$.data.cccd").value("099000000002"));
        mockMvc.perform(get(ME + "/certificates/" + code).cookie(otherTrainee))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-401"));
        mockMvc.perform(get(fileUrl).param("download", "true").cookie(trainee))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment")));
        mockMvc.perform(get(fileUrl).cookie(trainee))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")));

        role(RoleCode.BUSINESS);
        long businessId = business("0101234567");
        jdbcTemplate.update("UPDATE users SET business_id = ? WHERE id = ?", businessId, traineeId);
        Cookie manager = login(manager("quanly", businessId, "005"));
        Cookie otherManager = login(manager("quanlykhac", business("0109876543"), "006"));

        mockMvc.perform(get("/api/v1/my-business/certificates").cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].code").value(code))
                .andExpect(jsonPath("$.data.content[0].userId").value(traineeId));
        mockMvc.perform(get("/api/v1/my-business/certificates").param("keyword", "pccc").cookie(manager))
                .andExpect(jsonPath("$.data.content", hasSize(1)));
        mockMvc.perform(get("/api/v1/my-business/certificates").param("keyword", "khong-co").cookie(manager))
                .andExpect(jsonPath("$.data.content", hasSize(0)));
        mockMvc.perform(get("/api/v1/my-business/certificates").cookie(otherManager))
                .andExpect(jsonPath("$.data.content", hasSize(0)));
        mockMvc.perform(get("/api/v1/my-business/certificates/" + code).cookie(manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Họ hocvien"));
        mockMvc.perform(get("/api/v1/my-business/certificates/" + code).cookie(otherManager))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/my-business/certificates").cookie(trainee))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(fileUrl).cookie(manager)).andExpect(status().isOk());
        mockMvc.perform(get(fileUrl).cookie(otherManager)).andExpect(status().isForbidden());
    }

    @Test
    void identityFieldsAreLockedOnceACertificateIsIssued() throws Exception {
        issuedCertificate();

        for (String field : new String[] {"firstName", "lastName", "cccd", "dateOfBirth"}) {
            String value = "cccd".equals(field) ? "099000000099" : "dateOfBirth".equals(field) ? "1990-02-02" : "Mới";
            updateMe(trainee, Collections.singletonMap(field, value))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("VRC-409-105"));
        }
        // Resending the current values and changing other fields is still allowed.
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "hocvien");
        body.put("cccd", "099000000002");
        body.put("address", "Địa chỉ mới");
        updateMe(trainee, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.address").value("Địa chỉ mới"));

        // The issued certificate keeps the snapshot even after an admin renames the user.
        mockMvc.perform(put("/api/v1/users/" + traineeId).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Đổi tên\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(ME + "/certificates").cookie(trainee))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Họ hocvien"));
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

    /** {@code suffix} makes CCCD and phone number unique. */
    private User user(String username, String roleCode, String suffix) {
        return transactionTemplate.execute(tx -> {
            User user = User.builder().username(username).passwordHash(passwordEncoder.encode(PASSWORD))
                    .firstName(username).lastName("Họ").cccd("099000000" + suffix)
                    .phoneNumber("0990000" + suffix).email(username + "@example.com")
                    .dateOfBirth(LocalDate.of(1995, 1, 1)).build();
            user.addRole(roleRepository.findByCode(roleCode).orElseThrow(IllegalStateException::new), null);
            return userRepository.save(user);
        });
    }

    private long business(String taxCode) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO businesses (name, tax_code, address, phone_number, email, status, "
                + "created_at, updated_at) VALUES ('Công ty Xanh', ?, 'Hà Nội', '0241234567', 'lienhe@xanh.vn', "
                + "'ACTIVE', ?, ?)", taxCode, now, now);
        return jdbcTemplate.queryForObject("SELECT id FROM businesses WHERE tax_code = ?", Long.class, taxCode);
    }

    /** Manager (role BUSINESS) of {@code businessId}. */
    private User manager(String username, long businessId, String suffix) {
        User manager = user(username, RoleCode.BUSINESS, suffix);
        jdbcTemplate.update("UPDATE users SET business_id = ? WHERE id = ?", businessId, manager.getId());
        return manager;
    }

    private Cookie login(User user) {
        String token = transactionTemplate.execute(tx ->
                tokenProvider.generateAccessToken(new CustomUserDetails(userRepository.findById(user.getId())
                        .orElseThrow(IllegalStateException::new))));
        return new Cookie("access_token", token);
    }

    private long courseId(String name, CourseStatus status) throws Exception {
        CourseRequest request = CourseRequest.builder().name(name).description("Mô tả")
                .instructorId(instructorId).price(250_000L).status(status).build();
        String body = mockMvc.perform(post(COURSES).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private ResultActions updateMe(Cookie caller, Map<String, ?> body) throws Exception {
        return mockMvc.perform(put(ME).cookie(caller).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions changePassword(String current, String next) throws Exception {
        Map<String, String> body = new HashMap<>();
        body.put("currentPassword", current);
        body.put("newPassword", next);
        return mockMvc.perform(put(ME + "/password").cookie(trainee).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions revenue() throws Exception {
        return mockMvc.perform(get("/api/v1/enrollments/revenue").cookie(admin));
    }

    private ResultActions monthlyRevenue(String month, String keyword) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/enrollments/revenue/monthly").param("month", month)
                .cookie(admin);
        return mockMvc.perform(keyword == null ? request : request.param("keyword", keyword));
    }

    private ResultActions enroll(long courseId, Cookie caller) throws Exception {
        return mockMvc.perform(post(COURSES + "/" + courseId + "/enrollments").cookie(caller));
    }

    private long enrollmentId(long courseId, Cookie caller) throws Exception {
        String body = enroll(courseId, caller).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private ResultActions cancel(long courseId, Cookie caller) throws Exception {
        return mockMvc.perform(delete(COURSES + "/" + courseId + "/enrollments/me").cookie(caller));
    }

    private ResultActions setStatus(long courseId, long enrollmentId, String status, Cookie caller)
            throws Exception {
        return mockMvc.perform(put(COURSES + "/" + courseId + "/enrollments/" + enrollmentId).cookie(caller)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private static String certificateUrl(long courseId, long enrollmentId) {
        return COURSES + "/" + courseId + "/enrollments/" + enrollmentId + "/certificate";
    }

    private ResultActions issue(long courseId, long enrollmentId, MockMultipartFile file) throws Exception {
        return issue(courseId, enrollmentId, file, admin);
    }

    private ResultActions issue(long courseId, long enrollmentId, MockMultipartFile file, Cookie caller)
            throws Exception {
        return mockMvc.perform(multipart(certificateUrl(courseId, enrollmentId)).file(file).cookie(caller));
    }

    /** Course "PCCC", trainee enrolled, completed and certified. */
    private JsonNode issuedCertificate() throws Exception {
        long courseId = courseId("PCCC", CourseStatus.PUBLISHED);
        long enrollmentId = enrollmentId(courseId, trainee);
        setStatus(courseId, enrollmentId, "COMPLETED", admin).andExpect(status().isOk());
        String body = issue(courseId, enrollmentId, pdf("chung-chi.pdf")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data");
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", PDF);
    }
}
