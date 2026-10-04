package com.vierec.modules.payment;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

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
class PaymentApiTest {

    private static final String PAYMENTS = "/api/v1/payments";

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
    private Long traineeId;
    private Long otherTraineeId;
    private Long courseId;

    @BeforeEach
    void setUp() throws Exception {
        role(RoleCode.ADMIN);
        role(RoleCode.TRAINEE);
        admin = login(user("quantri", RoleCode.ADMIN));
        User traineeUser = user("hocvien", RoleCode.TRAINEE);
        traineeId = traineeUser.getId();
        trainee = login(traineeUser);
        otherTraineeId = user("hocvienkhac", RoleCode.TRAINEE).getId();
        courseId = courseId("Phòng cháy chữa cháy", traineeId);
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"payments", "courses", "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void adminRecordsAPaymentWithPayerAndCourse() throws Exception {
        Map<String, Object> body = payment(traineeId, courseId);
        body.put("transactionRef", "  FT123  ");
        body.put("note", "Chuyển khoản");

        create(body, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(traineeId))
                .andExpect(jsonPath("$.data.username").value("hocvien"))
                .andExpect(jsonPath("$.data.payerName").value("Họ hocvien"))
                .andExpect(jsonPath("$.data.payerEmail").value("hocvien@example.com"))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.courseName").value("Phòng cháy chữa cháy"))
                .andExpect(jsonPath("$.data.amount").value(1500000))
                .andExpect(jsonPath("$.data.method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.transactionRef").value("FT123"))
                .andExpect(jsonPath("$.data.createdByUsername").value("quantri"))
                .andExpect(jsonPath("$.data.paidAt").doesNotExist());
    }

    @Test
    void paidPaymentGetsAPaidTime() throws Exception {
        Map<String, Object> body = payment(traineeId, courseId);
        body.put("status", "PAID");
        create(body, admin)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paidAt").isNotEmpty());
    }

    @Test
    void createValidatesTheRequest() throws Exception {
        create(new HashMap<String, Object>(), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-001"))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("userId", "courseId", "amount", "method")));

        Map<String, Object> body = payment(traineeId, courseId);
        body.put("amount", 0);
        body.put("paidAt", "2999-01-01T00:00:00");
        create(body, admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("amount", "paidAt")));

        create(payment(999_999L, courseId), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-101"));
        create(payment(traineeId, 999_999L), admin)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-201"));
    }

    @Test
    void onlyAdminsRecordAndSearchPayments() throws Exception {
        create(payment(traineeId, courseId), trainee).andExpect(status().isForbidden());
        mockMvc.perform(get(PAYMENTS).cookie(trainee)).andExpect(status().isForbidden());
        mockMvc.perform(get(PAYMENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    void adminSearchesByPayerCourseAndStatus() throws Exception {
        long otherCourse = courseId("Sơ cứu", traineeId);
        create(payment(traineeId, courseId), admin).andExpect(status().isCreated());
        create(payment(traineeId, otherCourse), admin).andExpect(status().isCreated());
        Map<String, Object> paid = payment(otherTraineeId, courseId);
        paid.put("status", "PAID");
        create(paid, admin).andExpect(status().isCreated());

        mockMvc.perform(get(PAYMENTS).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
        mockMvc.perform(get(PAYMENTS).param("userId", traineeId.toString()).cookie(admin))
                .andExpect(jsonPath("$.data.content[*].courseName",
                        containsInAnyOrder("Phòng cháy chữa cháy", "Sơ cứu")));
        mockMvc.perform(get(PAYMENTS).param("courseId", courseId.toString()).param("status", "PAID").cookie(admin))
                .andExpect(jsonPath("$.data.content[*].username", containsInAnyOrder("hocvienkhac")));
    }

    @Test
    void userSeesOnlyTheirOwnPayments() throws Exception {
        long mine = paymentId(payment(traineeId, courseId));
        long others = paymentId(payment(otherTraineeId, courseId));

        mockMvc.perform(get("/api/v1/auth/me/payments").cookie(trainee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(mine));
        mockMvc.perform(get(PAYMENTS + "/" + mine).cookie(trainee)).andExpect(status().isOk());
        mockMvc.perform(get(PAYMENTS + "/" + others).cookie(trainee))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-501"));
        mockMvc.perform(get(PAYMENTS + "/" + others).cookie(admin)).andExpect(status().isOk());
    }

    @Test
    void adminUpdatesStatusAndRefundsOnlyPaidPayments() throws Exception {
        long id = paymentId(payment(traineeId, courseId));

        update(id, "REFUNDED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VRC-409-501"));
        update(id, "PAID")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paidAt").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").value(1500000));
        update(id, "REFUNDED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.paidAt").isNotEmpty());
        update(id, "FAILED")
                .andExpect(jsonPath("$.data.paidAt").doesNotExist());

        mockMvc.perform(put(PAYMENTS + "/" + id).cookie(trainee).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CASH\",\"status\":\"PAID\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(PAYMENTS + "/999999").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"CASH\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyKeepsTheCourseNameAfterTheCourseIsRenamedOrDeleted() throws Exception {
        long id = paymentId(payment(traineeId, courseId));
        CourseRequest renamed = CourseRequest.builder().name("Tên mới").description("Mô tả")
                .instructorId(traineeId).price(250_000L).status(CourseStatus.PUBLISHED).build();
        mockMvc.perform(put("/api/v1/courses/" + courseId).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(renamed))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/courses/" + courseId).cookie(admin)).andExpect(status().isNoContent());

        mockMvc.perform(get(PAYMENTS + "/" + id).cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.courseName").value("Phòng cháy chữa cháy"));
        mockMvc.perform(get("/api/v1/auth/me/payments").cookie(trainee))
                .andExpect(jsonPath("$.data.content[0].courseName").value("Phòng cháy chữa cháy"));
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

    private long courseId(String name, Long instructorId) throws Exception {
        CourseRequest request = CourseRequest.builder().name(name).description("Mô tả")
                .instructorId(instructorId).price(250_000L).status(CourseStatus.PUBLISHED).build();
        String body = mockMvc.perform(post("/api/v1/courses").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private static Map<String, Object> payment(Long userId, Long course) {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("courseId", course);
        body.put("amount", 1500000);
        body.put("method", "BANK_TRANSFER");
        return body;
    }

    private ResultActions create(Map<String, Object> body, Cookie caller) throws Exception {
        return mockMvc.perform(post(PAYMENTS).cookie(caller).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private long paymentId(Map<String, Object> body) throws Exception {
        String response = create(body, admin).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private ResultActions update(long id, String status) throws Exception {
        return mockMvc.perform(put(PAYMENTS + "/" + id).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"BANK_TRANSFER\",\"status\":\"" + status + "\"}"));
    }
}
