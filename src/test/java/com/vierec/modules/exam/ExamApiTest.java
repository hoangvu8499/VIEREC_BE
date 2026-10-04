package com.vierec.modules.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.JwtTokenProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.Cookie;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamApiTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

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
    private Cookie trainee;
    private long courseId;

    @BeforeEach
    void setUp() {
        admin = login(user("quantri", RoleCode.ADMIN));
        trainee = login(user("hocvien", RoleCode.TRAINEE));
        courseId = course("An toàn hoá chất", null);
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"exam_questions", "exams", "courses", "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    // ------------------------------------------------------------------ exam settings

    @Test
    void adminCreatesTheExamOfACourseThenChangesItsSettings() throws Exception {
        perform(get(exam(courseId)), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-701"));

        perform(put(exam(courseId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings("  Bài thi chứng chỉ  ", 30, "5"))), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Bài thi chứng chỉ"))
                .andExpect(jsonPath("$.data.durationMinutes").value(30))
                .andExpect(jsonPath("$.data.passScore").value(5))
                .andExpect(jsonPath("$.data.questionCount").value(0));

        perform(put(exam(courseId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings("Bài thi cuối khoá", 45, "7.5"))), admin)
                .andExpect(status().isOk());
        perform(get(exam(courseId)), admin)
                .andExpect(jsonPath("$.data.title").value("Bài thi cuối khoá"))
                .andExpect(jsonPath("$.data.durationMinutes").value(45))
                .andExpect(jsonPath("$.data.passScore").value(7.5));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exams", Integer.class)).isEqualTo(1);
    }

    @Test
    void validatesTheSettings() throws Exception {
        perform(put(exam(courseId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings(" ", 0, "10.5"))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("title", "durationMinutes",
                        "passScore")));
        perform(put(exam(courseId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings("Bài thi", 30, "0"))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("passScore"));

        long deleted = course("Khoá đã xoá", LocalDateTime.now());
        perform(put(exam(deleted)).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings("Bài thi", 30, "5"))), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-201"));
    }

    @Test
    void onlyAdminsManageExams() throws Exception {
        perform(get(exam(courseId)), trainee).andExpect(status().isForbidden());
        perform(get("/api/v1/exams/question-template"), trainee).andExpect(status().isForbidden());
        mockMvc.perform(get(exam(courseId))).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ questions by hand

    @Test
    void adminAddsEditsAndDeletesQuestions() throws Exception {
        createExam();
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(question("Câu 1", "A"))),
                admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.content").value("Câu 1"))
                .andExpect(jsonPath("$.data.optionB").value("Câu 1 - B"))
                .andExpect(jsonPath("$.data.correctOption").value("A"))
                .andExpect(jsonPath("$.data.sortOrder").value(1));
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(question("Câu 2", "D"))),
                admin)
                .andExpect(jsonPath("$.data.sortOrder").value(2));
        long first = questionId(0);

        Map<String, Object> edited = question("  Câu 1 đã sửa  ", "C");
        perform(put(questions() + "/" + first).contentType(MediaType.APPLICATION_JSON).content(json(edited)),
                admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Câu 1 đã sửa"))
                .andExpect(jsonPath("$.data.correctOption").value("C"));

        perform(get(exam(courseId)), admin)
                .andExpect(jsonPath("$.data.questionCount").value(2))
                .andExpect(jsonPath("$.data.questions[*].content", contains("Câu 1 đã sửa", "Câu 2")));

        perform(delete(questions() + "/" + first), admin).andExpect(status().isNoContent());
        perform(get(exam(courseId)), admin)
                .andExpect(jsonPath("$.data.questions[*].content", contains("Câu 2")));
        perform(delete(questions() + "/" + first), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-702"));
    }

    @Test
    void validatesAQuestion() throws Exception {
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(question("Câu 1", "A"))),
                admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-701"));

        createExam();
        Map<String, Object> blank = question(" ", "A");
        blank.put("optionC", "");
        blank.remove("correctOption");
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(blank)), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("content", "optionC",
                        "correctOption")));
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(question("Câu", "E"))),
                admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("correctOption"));
    }

    // ------------------------------------------------------------------ Excel

    @Test
    void theTemplateHasTheQuestionSheetAndAGuide() throws Exception {
        byte[] template = perform(get("/api/v1/exams/question-template"), admin)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", XLSX))
                .andExpect(header().string("Content-Disposition", containsString("mau-cau-hoi-bai-thi.xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template))) {
            assertThat(workbook.getSheetName(0)).isEqualTo("Câu hỏi");
            assertThat(workbook.getSheetName(1)).isEqualTo("Hướng dẫn");
            Row header = workbook.getSheetAt(0).getRow(0);
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Câu hỏi");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("Đáp án đúng");
            assertThat(workbook.getSheetAt(0).getLastRowNum()).isZero();
        }
    }

    @Test
    void importAppendsOrReplacesTheQuestions() throws Exception {
        createExam();
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(question("Câu cũ", "A"))),
                admin);

        // Row 3 is blank and skipped; numbers are read as typed; the answer may be lower case.
        byte[] file = workbook(
                new Object[] {1, "  Câu nhập 1 ", "Đáp án A", "Đáp án B", "Đáp án C", "Đáp án D", " b "},
                null,
                new Object[] {null, "Câu nhập 2", 100, 200, 300, 400, "D"});
        importFile(file, "APPEND")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount").value(2))
                .andExpect(jsonPath("$.data.exam.questionCount").value(3))
                .andExpect(jsonPath("$.data.exam.questions[*].content",
                        contains("Câu cũ", "Câu nhập 1", "Câu nhập 2")))
                .andExpect(jsonPath("$.data.exam.questions[*].sortOrder", contains(1, 2, 3)))
                .andExpect(jsonPath("$.data.exam.questions[1].correctOption").value("B"))
                .andExpect(jsonPath("$.data.exam.questions[2].optionA").value("100"));

        importFile(file, "REPLACE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.questions[*].content", contains("Câu nhập 1", "Câu nhập 2")))
                .andExpect(jsonPath("$.data.exam.questions[*].sortOrder", contains(1, 2)));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exam_questions", Integer.class)).isEqualTo(2);
    }

    @Test
    void aFileWithInvalidRowsChangesNothing() throws Exception {
        createExam();
        perform(post(questions()).contentType(MediaType.APPLICATION_JSON).content(json(question("Câu cũ", "A"))),
                admin);

        byte[] file = workbook(
                new Object[] {1, "Câu đúng", "A", "B", "C", "D", "A"},
                new Object[] {2, "Thiếu đáp án C", "A", "B", "", "D", "E"},
                new Object[] {3, "", "A", "B", "C", "D", ""});
        importFile(file, "REPLACE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-701"))
                .andExpect(jsonPath("$.errors[*].field", contains("rows[3].optionC", "rows[3].correctOption",
                        "rows[4].content", "rows[4].correctOption")))
                .andExpect(jsonPath("$.errors[1].rejectedValue").value("E"))
                .andExpect(jsonPath("$.errors[2].rejectedValue").value(""));

        perform(get(exam(courseId)), admin)
                .andExpect(jsonPath("$.data.questions[*].content", contains("Câu cũ")));
    }

    @Test
    void rejectsFilesThatCannotBeImported() throws Exception {
        importFile(workbook(new Object[] {1, "Câu", "A", "B", "C", "D", "A"}), "APPEND")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-701"));

        createExam();
        importFile(workbook(), "APPEND")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-703"));
        perform(multipart(questions() + "/import")
                .file(new MockMultipartFile("file", "cau-hoi.xlsx", XLSX, "not excel".getBytes()))
                .param("mode", "APPEND"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-702"));
        perform(multipart(questions() + "/import")
                .file(new MockMultipartFile("file", "cau-hoi.csv", "text/csv", "a,b".getBytes()))
                .param("mode", "APPEND"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-301"));
        perform(multipart(questions() + "/import"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("file", "mode")));
    }

    @Test
    void theFilledInTemplateCanBeImported() throws Exception {
        createExam();
        byte[] template = perform(get("/api/v1/exams/question-template"), admin)
                .andReturn().getResponse().getContentAsByteArray();
        byte[] filled;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row row = workbook.getSheetAt(0).createRow(1);
            Object[] values = {1, "Câu từ file mẫu", "A", "B", "C", "D", "C"};
            for (int i = 0; i < values.length; i++) {
                row.createCell(i).setCellValue(String.valueOf(values[i]));
            }
            workbook.write(out);
            filled = out.toByteArray();
        }
        importFile(filled, "APPEND")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exam.questions[*].content", contains("Câu từ file mẫu")));
    }

    // ------------------------------------------------------------------ helpers

    private static String exam(long id) {
        return "/api/v1/courses/" + id + "/exam";
    }

    private String questions() {
        return exam(courseId) + "/questions";
    }

    private void createExam() throws Exception {
        perform(put(exam(courseId)).contentType(MediaType.APPLICATION_JSON)
                .content(json(settings("Bài thi chứng chỉ", 30, "5"))), admin)
                .andExpect(status().isOk());
    }

    private long questionId(int index) throws Exception {
        String body = perform(get(exam(courseId)), admin).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("questions").path(index).path("id").asLong();
    }

    private ResultActions importFile(byte[] file, String mode) throws Exception {
        return perform(multipart(questions() + "/import")
                .file(new MockMultipartFile("file", "cau-hoi.xlsx", XLSX, file))
                .param("mode", mode), admin);
    }

    private ResultActions perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                  Cookie cookie) throws Exception {
        return mockMvc.perform(request.cookie(cookie));
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private static Map<String, Object> settings(String title, int duration, String passScore) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("durationMinutes", duration);
        body.put("passScore", passScore);
        return body;
    }

    private static Map<String, Object> question(String content, String correct) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        for (String option : new String[] {"A", "B", "C", "D"}) {
            body.put("option" + option, content.trim() + " - " + option);
        }
        body.put("correctOption", correct);
        return body;
    }

    /** Question sheet with a header row, then one row per array ({@code null} = an empty row). */
    private static byte[] workbook(Object[]... rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Câu hỏi");
            sheet.createRow(0).createCell(1).setCellValue("Câu hỏi");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                if (rows[i] == null) {
                    continue;
                }
                for (int column = 0; column < rows[i].length; column++) {
                    Object value = rows[i][column];
                    if (value instanceof Number) {
                        row.createCell(column).setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        row.createCell(column).setCellValue((String) value);
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private long course(String name, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO courses (name, price, status, created_at, updated_at, deleted_at) "
                + "VALUES (?, 100000, 'PUBLISHED', ?, ?, ?)", name, LocalDateTime.now(), LocalDateTime.now(), deletedAt);
        return jdbcTemplate.queryForObject("SELECT id FROM courses WHERE name = ?", Long.class, name);
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
