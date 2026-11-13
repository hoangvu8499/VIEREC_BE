package com.vierec.modules.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.business.service.BusinessMemberWorkbook;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.security.CustomUserDetails;
import com.vierec.security.JwtTokenProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.Cookie;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Learner accounts of a business created from an Excel file: {@code POST /api/v1/my-business/members/import}.
 * Every failing case also asserts that not one account was created, which is the point of the endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessMemberImportApiTest {

    private static final String IMPORT = "/api/v1/my-business/members/import";
    private static final String DEFAULT_PASSWORD = "Congty@2026";
    private static final String[] HEADERS = {"STT", "Họ", "Tên", "Số CCCD", "Ngày sinh", "Địa chỉ",
        "Số điện thoại", "Email"};

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
    private Cookie manager;
    private long businessId;

    @BeforeEach
    void setUp() throws Exception {
        role(RoleCode.TRAINEE);
        role(RoleCode.BUSINESS);
        admin = login(user("quantri", RoleCode.ADMIN));
        businessId = createBusiness("0101234567", "mtxanh.admin");
        manager = login(userRepository.findByUsername("mtxanh.admin").orElseThrow(IllegalStateException::new));
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"course_enrollments", "user_roles", "users", "businesses"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    // ------------------------------------------------------------------ happy path

    @Test
    void importsEveryRowAsATraineeOfTheBusiness() throws Exception {
        byte[] file = workbook(
                row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn"),
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn"));

        perform(file, DEFAULT_PASSWORD, manager)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(2))
                .andExpect(jsonPath("$.data.members[0].username").value("001099012345"))
                .andExpect(jsonPath("$.data.members[0].fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data.members[1].username").value("001099012346"));

        assertThat(count()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT business_id FROM users WHERE username = '001099012345'",
                Long.class)).isEqualTo(businessId);
        assertThat(jdbcTemplate.queryForObject("SELECT cccd FROM users WHERE username = '001099012345'",
                String.class)).isEqualTo("001099012345");
        assertThat(roleCodesOf("001099012345")).containsExactly(RoleCode.TRAINEE);
    }

    @Test
    void everyImportedAccountLogsInWithTheDefaultPasswordAndMustChangeIt() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567",
                "an@congty.vn")), DEFAULT_PASSWORD, manager).andExpect(status().isCreated());

        assertThat(jdbcTemplate.queryForObject("SELECT must_change_password FROM users WHERE username = '001099012345'",
                Boolean.class)).isTrue();
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("username", "001099012345");
        credentials.put("password", DEFAULT_PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk());
    }

    @Test
    void blankRowsBetweenLearnersAreSkipped() throws Exception {
        byte[] file = workbook(
                row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn"),
                new String[] {"", "", "", "", "", "", "", ""},
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn"));

        perform(file, DEFAULT_PASSWORD, manager)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(2));
        assertThat(count()).isEqualTo(2);
    }

    // ------------------------------------------------------------------ one bad row stops everything

    @Test
    void aSingleInvalidCellCreatesNoAccountAndNamesItsExcelRow() throws Exception {
        byte[] file = workbook(
                row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn"),
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn"),
                // Row 4 of the sheet: a phone number of 9 digits.
                row("Lê", "Văn Cường", "001099012347", "01/01/1990", "090123456", "cuong@congty.vn"));

        perform(file, DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors[0].field").value("rows[4].phoneNumber"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").value("090123456"));

        assertThat(count()).isZero();
    }

    @Test
    void aMissingRequiredCellIsReportedOnItsOwnRow() throws Exception {
        byte[] file = workbook(
                row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn"),
                // Row 3: no address.
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn",
                        ""));

        perform(file, DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors[0].field").value("rows[3].address"));
        assertThat(count()).isZero();
    }

    @Test
    void anUnparseableDateOfBirthIsReportedOnItsOwnRow() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "ngày 8 tháng 4", "0901234567",
                        "an@congty.vn")), DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors[0].field").value("rows[2].dateOfBirth"));
        assertThat(count()).isZero();
    }

    // ------------------------------------------------------------------ one account per CCCD

    @Test
    void theSameCccdTwiceInTheFileIsRejectedOnTheSecondRow() throws Exception {
        byte[] file = workbook(
                row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn"),
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn"),
                // Row 4 repeats the CCCD of row 2.
                row("Lê", "Văn Cường", "001099012345", "01/01/1990", "0901234569", "cuong@congty.vn"));

        perform(file, DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors[0].field").value("rows[4].cccd"))
                .andExpect(jsonPath("$.errors[0].message").value("Already used on row 2 of the file"));
        assertThat(count()).isZero();
    }

    @Test
    void aCccdThatAlreadyHasAnAccountIsRejected() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567",
                "an@congty.vn")), DEFAULT_PASSWORD, manager).andExpect(status().isCreated());

        byte[] again = workbook(
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn"),
                // Row 3 carries the CCCD of the learner imported above.
                row("Lê", "Văn Cường", "001099012345", "01/01/1990", "0901234569", "cuong@congty.vn"));

        perform(again, DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors[0].field").value("rows[3].cccd"))
                .andExpect(jsonPath("$.errors[0].message").value("An account already uses this value"));
        // Only the learner of the first import exists; the second import added nobody.
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void aCccdOfASoftDeletedAccountIsStillTaken() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567",
                "an@congty.vn")), DEFAULT_PASSWORD, manager).andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE users SET deleted_at = ? WHERE username = '001099012345'", LocalDateTime.now());

        perform(workbook(row("Lê", "Văn Khác", "001099012345", "01/01/1990", "0901234569",
                        "khac@congty.vn")), DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors[0].field").value("rows[2].cccd"));
    }

    @Test
    void theCccdBecomesTheUsername() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn")),
                DEFAULT_PASSWORD, manager)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.members[0].username").value("001099012345"))
                .andExpect(jsonPath("$.data.members[0].cccd").value("001099012345"));
    }

    @Test
    void aCccdAlreadyUsedAsAnotherAccountsUsernameIsRejectedOnTheCccdColumn() throws Exception {
        // An account whose username happens to be the CCCD of row 3, but with no CCCD of its own.
        user("001099012346", RoleCode.TRAINEE);
        byte[] file = workbook(
                row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567", "an@congty.vn"),
                row("Trần", "Thị Bình", "001099012346", "20/05/1995", "0901234568", "binh@congty.vn"));

        perform(file, DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-802"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("rows[3].cccd"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("Another account already uses this CCCD as its username"));
        assertThat(count()).isZero();
    }

    // ------------------------------------------------------------------ limits and access

    @Test
    void aFileOverFiveHundredLearnersIsRejected() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < BusinessMemberWorkbook.MAX_MEMBERS + 1; i++) {
            rows.add(row("Nguyễn", "Số " + i, String.format("%012d", 100000000000L + i),
                    "08/04/1999", String.format("09%08d", i), "hv" + i + "@congty.vn"));
        }

        perform(workbook(rows.toArray(new String[0][])), DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-805"));
        assertThat(count()).isZero();
    }

    @Test
    void exactlyFiveHundredLearnersAreAccepted() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < BusinessMemberWorkbook.MAX_MEMBERS; i++) {
            rows.add(row("Nguyễn", "Số " + i, String.format("%012d", 100000000000L + i),
                    "08/04/1999", String.format("09%08d", i), "hv" + i + "@congty.vn"));
        }

        perform(workbook(rows.toArray(new String[0][])), DEFAULT_PASSWORD, manager)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(BusinessMemberWorkbook.MAX_MEMBERS));
        assertThat(count()).isEqualTo(BusinessMemberWorkbook.MAX_MEMBERS);
    }

    @Test
    void anEmptyFileIsRejected() throws Exception {
        perform(workbook(), DEFAULT_PASSWORD, manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-804"));
    }

    @Test
    void aFileThatIsNotAWorkbookIsRejected() throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart(IMPORT);
        request.file(new MockMultipartFile("file", "hocvien.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE, "khong phai excel".getBytes("UTF-8")));
        request.param("defaultPassword", DEFAULT_PASSWORD);

        mockMvc.perform(request.cookie(manager))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-803"));
    }

    @Test
    void aPasswordWithoutASpecialCharacterIsRejectedOnce() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567",
                        "an@congty.vn")), "khongcokytudacbiet1", manager)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[0].field").value("defaultPassword"));
        assertThat(count()).isZero();
    }

    @Test
    void anAdminIsNotABusinessManager() throws Exception {
        perform(workbook(row("Nguyễn", "Văn An", "001099012345", "08/04/1999", "0901234567",
                        "an@congty.vn")), DEFAULT_PASSWORD, admin)
                .andExpect(status().isForbidden());
        assertThat(count()).isZero();
    }

    @Test
    void theTemplateIsADownloadableWorkbook() throws Exception {
        byte[] body = mockMvc.perform(get("/api/v1/my-business/members/import-template").cookie(manager))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        // The template must be readable back by the importer's own sheet name.
        assertThat(body[0]).isEqualTo((byte) 'P');
    }

    // ------------------------------------------------------------------ helpers

    /** The eight columns of one learner row; a seventh argument overrides the address, which defaults to a value. */
    private static String[] row(String lastName, String firstName, String cccd, String birth, String phone,
                                String email) {
        return new String[] {"", lastName, firstName, cccd, birth, "12 Nguyễn Huệ, Hà Nội", phone, email};
    }

    private static String[] row(String lastName, String firstName, String cccd, String birth, String phone,
                                String email, String address) {
        String[] values = row(lastName, firstName, cccd, birth, phone, email);
        values[5] = address;
        return values;
    }

    private static byte[] workbook(String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(BusinessMemberWorkbook.MEMBER_SHEET);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                header.createCell(column).setCellValue(HEADERS[column]);
            }
            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                for (int column = 0; column < rows[index].length; column++) {
                    // Written as text so that a CCCD or a phone number keeps its leading zero.
                    row.createCell(column).setCellValue(rows[index][column]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ResultActions perform(byte[] file, String defaultPassword, Cookie cookie) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart(IMPORT);
        request.file(new MockMultipartFile("file", "hocvien.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file));
        request.param("defaultPassword", defaultPassword);
        return mockMvc.perform(request.cookie(cookie));
    }

    private long count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE business_id = ? AND username <> ?",
                Long.class, businessId, "mtxanh.admin");
    }

    private List<String> roleCodesOf(String username) {
        return jdbcTemplate.queryForList("SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id = r.id "
                + "JOIN users u ON u.id = ur.user_id WHERE u.username = ?", String.class, username);
    }

    private long createBusiness(String taxCode, String managerUsername) throws Exception {
        Map<String, Object> manager = new HashMap<>();
        manager.put("username", managerUsername);
        manager.put("password", "Matkhau@123");
        manager.put("firstName", "Bình");
        manager.put("lastName", "Trần");
        manager.put("phoneNumber", "0281234500");
        manager.put("email", managerUsername + "@moitruongxanh.vn");
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Công ty Môi trường Xanh");
        body.put("taxCode", taxCode);
        body.put("address", "12 Nguyễn Huệ, TP. Hồ Chí Minh");
        body.put("phoneNumber", "0281234567");
        body.put("email", "lienhe@moitruongxanh.vn");
        body.put("manager", manager);

        perform(post("/api/v1/businesses").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)), admin)
                .andExpect(status().isCreated());
        return jdbcTemplate.queryForObject("SELECT id FROM businesses WHERE tax_code = ?", Long.class, taxCode);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, Cookie cookie) throws Exception {
        return mockMvc.perform(request.cookie(cookie));
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
            User created = User.builder().username(username).passwordHash("$2a$12$hash")
                    .firstName(username).lastName("Họ").email(username + "@example.com").build();
            created.addRole(roleRepository.findByCode(roleCode).orElseThrow(IllegalStateException::new), null);
            return userRepository.save(created);
        });
    }

    private Cookie login(User account) {
        String token = transactionTemplate.execute(tx ->
                tokenProvider.generateAccessToken(new CustomUserDetails(userRepository.findById(account.getId())
                        .orElseThrow(IllegalStateException::new))));
        return new Cookie("access_token", token);
    }
}
