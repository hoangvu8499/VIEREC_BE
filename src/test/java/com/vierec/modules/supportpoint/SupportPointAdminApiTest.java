package com.vierec.modules.supportpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.supportpoint.service.GeocodedPlace;
import com.vierec.modules.supportpoint.service.GeocodingService;
import com.vierec.modules.supportpoint.service.SupportPointWorkbook;
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
import org.springframework.boot.test.mock.mockito.MockBean;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin management of {@code support_points}: list, create, edit, delete, Excel import. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportPointAdminApiTest {

    private static final String POINTS = "/api/v1/support-points";
    private static final String[] HEADERS = {"STT", "Tên đơn vị", "Tỉnh / Thành phố", "Quận / Huyện",
        "Địa chỉ chi tiết", "Số điện thoại", "Vĩ độ", "Kinh độ"};

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

    @MockBean
    private GeocodingService geocodingService;

    private Cookie admin;
    private Cookie trainee;

    @BeforeEach
    void setUp() {
        admin = login(user("quantri", RoleCode.ADMIN));
        trainee = login(user("hocvien", RoleCode.TRAINEE));
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"support_points", "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    // ------------------------------------------------------------------ CRUD

    @Test
    void createsAPointWithItsCoordinatesWithoutLookingUpTheAddress() throws Exception {
        perform(post(POINTS).contentType(MediaType.APPLICATION_JSON)
                .content(json(body("  Đội PCCC Ngũ Hành Sơn  ", 15.99422412345, 108.237739))), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("Đội PCCC Ngũ Hành Sơn"))
                .andExpect(jsonPath("$.data.province").value("Đà Nẵng"))
                // DECIMAL(10,7): rounded to 7 decimals.
                .andExpect(jsonPath("$.data.latitude").value(15.9942241))
                .andExpect(jsonPath("$.data.createdAt").exists());
        verifyNoInteractions(geocodingService);
        assertThat(count()).isEqualTo(1);
    }

    @Test
    void withoutCoordinatesTheAddressIsLookedUp() throws Exception {
        when(geocodingService.geocode(anyString()))
                .thenReturn(Optional.of(new GeocodedPlace(16.0544, 108.2022, "Hải Châu, Đà Nẵng")));

        perform(post(POINTS).contentType(MediaType.APPLICATION_JSON)
                .content(json(body("Đội PCCC Hải Châu", null, null))), admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.latitude").value(16.0544))
                .andExpect(jsonPath("$.data.longitude").value(108.2022));
        verify(geocodingService).geocode("12 Lê Văn Hiến, Ngũ Hành Sơn, Đà Nẵng");
    }

    @Test
    void anAddressThatCannotBeFoundAsksForCoordinates() throws Exception {
        when(geocodingService.geocode(anyString())).thenReturn(Optional.empty());

        perform(post(POINTS).contentType(MediaType.APPLICATION_JSON)
                .content(json(body("Đội PCCC", null, null))), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-601"));
        assertThat(count()).isZero();
    }

    @Test
    void onlyOneCoordinateIsRejected() throws Exception {
        perform(post(POINTS).contentType(MediaType.APPLICATION_JSON)
                .content(json(body("Đội PCCC", 15.99, null))), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-601"));
        verifyNoInteractions(geocodingService);
    }

    @Test
    void missingAndInvalidFieldsAreValidated() throws Exception {
        Map<String, Object> body = body("", 95.0, 108.0);
        body.put("phoneNumber", "abc");
        perform(post(POINTS).contentType(MediaType.APPLICATION_JSON).content(json(body)), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "name", "phoneNumber", "latitude")));
    }

    @Test
    void listsSortedByAreaAndSearches() throws Exception {
        // Names without a leading "Đ": H2 sorts it after "Z", MySQL (utf8mb4_unicode_ci) like "D".
        point("Đội Sơn Trà", "Khánh Hoà", "Sơn Trà", null);
        point("Đội Điện Bàn", "Quảng Nam", "Điện Bàn", null);
        point("Đội Cẩm Lệ", "Khánh Hoà", "Cẩm Lệ", null);
        point("Đã xoá", "Khánh Hoà", "Cẩm Lệ", LocalDateTime.now());

        perform(get(POINTS), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[*].name", contains("Đội Cẩm Lệ", "Đội Sơn Trà",
                        "Đội Điện Bàn")));
        perform(get(POINTS).param("keyword", "quảng"), admin)
                .andExpect(jsonPath("$.data.content[*].name", contains("Đội Điện Bàn")));
    }

    @Test
    void editsAndSoftDeletesAPoint() throws Exception {
        long id = point("Đội cũ", "Đà Nẵng", "Sơn Trà", null);

        perform(put(POINTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(json(body("Đội mới", 16.1, 108.3))), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Đội mới"))
                .andExpect(jsonPath("$.data.district").value("Ngũ Hành Sơn"))
                .andExpect(jsonPath("$.data.latitude").value(16.1));

        perform(delete(POINTS + "/" + id), admin).andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM support_points WHERE id = ?",
                LocalDateTime.class, id)).isNotNull();
        perform(get(POINTS + "/" + id), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-602"));
        perform(delete(POINTS + "/" + id), admin).andExpect(status().isNotFound());
    }

    @Test
    void onlyAdminsManagePointsButEveryoneStillSearchesNearby() throws Exception {
        point("Đội Sơn Trà", "Đà Nẵng", "Sơn Trà", null);

        perform(get(POINTS), trainee).andExpect(status().isForbidden());
        perform(post(POINTS).contentType(MediaType.APPLICATION_JSON)
                .content(json(body("Đội PCCC", 15.99, 108.23))), trainee)
                .andExpect(status().isForbidden());
        perform(get(POINTS + "/nearby").param("latitude", "16.0").param("longitude", "108.25"), trainee)
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ import

    @Test
    void importsEveryRowWithoutLookingUpAddresses() throws Exception {
        byte[] file = workbook(
                row("Đội PCCC Ngũ Hành Sơn", "Đà Nẵng", "Ngũ Hành Sơn", "12 Lê Văn Hiến", "0236 3822 114",
                        "15.994224", "108.237739"),
                // Decimal comma, as a Vietnamese Excel shows it; no street address.
                row("Đội PCCC Điện Bàn", "Quảng Nam", "Điện Bàn", "", "114", "15,920473", "108,208416"));

        importFile(file, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.importedCount").value(2));

        assertThat(count()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT latitude FROM support_points WHERE district = 'Điện Bàn'",
                BigDecimal.class)).isEqualByComparingTo("15.920473");
        assertThat(jdbcTemplate.queryForObject("SELECT address FROM support_points WHERE district = 'Điện Bàn'",
                String.class)).isNull();
        verifyNoInteractions(geocodingService);
    }

    @Test
    void aSingleBadRowSavesNothingAndNamesItsExcelRow() throws Exception {
        byte[] file = workbook(
                row("Đội 1", "Đà Nẵng", "Sơn Trà", "", "0905123456", "16.1", "108.3"),
                // Row 3: no longitude, latitude out of range.
                row("Đội 2", "Đà Nẵng", "Sơn Trà", "", "0905123457", "95", ""),
                // Row 4: not a number, no district.
                row("Đội 3", "Đà Nẵng", "", "", "0905123458", "mười sáu", "108.3"));

        importFile(file, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-602"))
                .andExpect(jsonPath("$.errors[*].field", contains("rows[3].latitude", "rows[3].longitude",
                        "rows[4].district", "rows[4].latitude")))
                .andExpect(jsonPath("$.errors[1].message").value("Longitude is required"))
                .andExpect(jsonPath("$.errors[3].message").value("Coordinate must be a number like 15.994224"));
        assertThat(count()).isZero();
    }

    @Test
    void aFileOverFiveHundredRowsIsRejected() throws Exception {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i <= SupportPointWorkbook.MAX_POINTS; i++) {
            rows.add(row("Đội " + i, "Đà Nẵng", "Sơn Trà", "", "0905123456", "16.1", "108.3"));
        }
        importFile(workbook(rows.toArray(new String[0][])), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-605"));
        assertThat(count()).isZero();
    }

    @Test
    void anEmptyFileIsRejected() throws Exception {
        importFile(workbook(), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-604"));
    }

    @Test
    void theTemplateIsADownloadableWorkbookForAdminsOnly() throws Exception {
        byte[] body = mockMvc.perform(get(POINTS + "/import-template").cookie(admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body).isNotEmpty();
        perform(get(POINTS + "/import-template"), trainee).andExpect(status().isForbidden());
        importFile(workbook(row("Đội 1", "Đà Nẵng", "Sơn Trà", "", "0905123456", "16.1", "108.3")), trainee)
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> body(String name, Double latitude, Double longitude) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("province", "Đà Nẵng");
        body.put("district", "Ngũ Hành Sơn");
        body.put("address", "12 Lê Văn Hiến");
        body.put("phoneNumber", "0236 3822 114");
        body.put("latitude", latitude);
        body.put("longitude", longitude);
        return body;
    }

    private static String[] row(String name, String province, String district, String address, String phone,
                                String latitude, String longitude) {
        return new String[] {"", name, province, district, address, phone, latitude, longitude};
    }

    private static byte[] workbook(String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SupportPointWorkbook.POINT_SHEET);
            Row header = sheet.createRow(0);
            for (int column = 0; column < HEADERS.length; column++) {
                header.createCell(column).setCellValue(HEADERS[column]);
            }
            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                for (int column = 0; column < rows[index].length; column++) {
                    row.createCell(column).setCellValue(rows[index][column]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private ResultActions importFile(byte[] file, Cookie cookie) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart(POINTS + "/import");
        request.file(new MockMultipartFile("file", "diem-ho-tro.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file));
        return mockMvc.perform(request.cookie(cookie));
    }

    private long point(String name, String province, String district, LocalDateTime deletedAt) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("INSERT INTO support_points (name, province, district, latitude, longitude, "
                + "phone_number, created_at, updated_at, deleted_at) VALUES (?, ?, ?, 16.0, 108.2, '0905123456', "
                + "?, ?, ?)", name, province, district, now, now, deletedAt);
        return jdbcTemplate.queryForObject("SELECT id FROM support_points WHERE name = ?", Long.class, name);
    }

    private long count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM support_points WHERE deleted_at IS NULL",
                Long.class);
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
