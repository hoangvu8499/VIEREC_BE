package com.vierec.modules.supportpoint;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.supportpoint.service.GeocodedPlace;
import com.vierec.modules.supportpoint.service.GeocodingService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupportPointApiTest {

    private static final String NEARBY = "/api/v1/support-points/nearby";
    /** Near Ngũ Hành Sơn, Đà Nẵng: unit 1 is ~1 km away, unit 2 ~1.7 km, unit 7 ~9.4 km. */
    private static final double ORIGIN_LAT = 15.9967;
    private static final double ORIGIN_LNG = 108.2462;

    @Autowired
    private MockMvc mockMvc;
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

    private Cookie trainee;

    @BeforeEach
    void setUp() {
        role(RoleCode.TRAINEE);
        trainee = login(user("hocvien"));
        point("Đơn vị số 1", "Ngũ Hành Sơn", 15.9942240, 108.2377390, null);
        point("Đơn vị số 2", "Ngũ Hành Sơn", 15.9812630, 108.2461430, null);
        point("Đơn vị số 7", "Điện Bàn", 15.9204730, 108.2084160, null);
        point("Đã ngừng hoạt động", "Ngũ Hành Sơn", ORIGIN_LAT, ORIGIN_LNG, LocalDateTime.now());
    }

    @AfterEach
    void cleanUp() {
        for (String table : new String[] {"support_points", "user_roles", "users"}) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
    }

    @Test
    void findsTheUnitsWithinTheRadiusOfTheCoordinatesNearestFirst() throws Exception {
        nearby(get(NEARBY).param("latitude", String.valueOf(ORIGIN_LAT))
                .param("longitude", String.valueOf(ORIGIN_LNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.radiusKm").value(3))
                .andExpect(jsonPath("$.data.origin.latitude").value(ORIGIN_LAT))
                .andExpect(jsonPath("$.data.origin.label").doesNotExist())
                .andExpect(jsonPath("$.data.results[*].name", contains("Đơn vị số 1", "Đơn vị số 2")))
                .andExpect(jsonPath("$.data.results[0].district").value("Ngũ Hành Sơn"))
                .andExpect(jsonPath("$.data.results[0].province").value("Đà Nẵng"))
                .andExpect(jsonPath("$.data.results[0].phoneNumber").value("0905123456"))
                .andExpect(jsonPath("$.data.results[0].latitude").value(15.994224))
                .andExpect(jsonPath("$.data.results[0].distanceKm", closeTo(0.954, 0.05)));

        nearby(get(NEARBY).param("latitude", String.valueOf(ORIGIN_LAT))
                .param("longitude", String.valueOf(ORIGIN_LNG)).param("radiusKm", "10"))
                .andExpect(jsonPath("$.data.results[*].name",
                        contains("Đơn vị số 1", "Đơn vị số 2", "Đơn vị số 7")));
        verifyNoInteractions(geocodingService);
    }

    @Test
    void anAddressIsResolvedToCoordinatesFirst() throws Exception {
        when(geocodingService.geocode("Ngũ Hành Sơn, Đà Nẵng")).thenReturn(Optional.of(
                new GeocodedPlace(ORIGIN_LAT, ORIGIN_LNG, "Ngũ Hành Sơn, Đà Nẵng, Việt Nam")));

        nearby(get(NEARBY).param("address", "Ngũ Hành Sơn, Đà Nẵng"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.origin.label").value("Ngũ Hành Sơn, Đà Nẵng, Việt Nam"))
                .andExpect(jsonPath("$.data.origin.longitude").value(ORIGIN_LNG))
                .andExpect(jsonPath("$.data.results.length()").value(2));
    }

    @Test
    void explainsAnAddressThatCannotBeUsed() throws Exception {
        when(geocodingService.geocode(anyString())).thenReturn(Optional.<GeocodedPlace>empty());
        nearby(get(NEARBY).param("address", "không có nơi này"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VRC-404-601"));

        when(geocodingService.geocode(anyString())).thenThrow(new BusinessException(ErrorCode.GEOCODING_UNAVAILABLE));
        nearby(get(NEARBY).param("address", "Đà Nẵng"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("VRC-503-601"));
    }

    @Test
    void validatesTheLocationAndRadius() throws Exception {
        nearby(get(NEARBY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-601"));
        nearby(get(NEARBY).param("latitude", "16"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VRC-400-601"));
        nearby(get(NEARBY).param("latitude", "91").param("longitude", "181").param("radiusKm", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("nearby.latitude", "nearby.longitude",
                        "nearby.radiusKm")));
    }

    @Test
    void needsALoggedInUser() throws Exception {
        mockMvc.perform(get(NEARBY).param("latitude", "16").param("longitude", "108"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions nearby(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.cookie(trainee));
    }

    private void point(String name, String district, double lat, double lng, LocalDateTime deletedAt) {
        String province = "Điện Bàn".equals(district) ? "Quảng Nam" : "Đà Nẵng";
        String phone = name.endsWith("1") ? "0905123456" : "0912345678";
        jdbcTemplate.update("INSERT INTO support_points (name, province, district, latitude, longitude, phone_number, "
                        + "created_at, updated_at, deleted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                name, province, district, lat, lng, phone, LocalDateTime.now(), LocalDateTime.now(), deletedAt);
    }

    private void role(String code) {
        if (!roleRepository.findByCode(code).isPresent()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(code);
            roleRepository.save(role);
        }
    }

    private User user(String username) {
        return transactionTemplate.execute(tx -> {
            User user = User.builder().username(username).passwordHash("$2a$12$hash")
                    .firstName(username).lastName("Họ").email(username + "@example.com").build();
            user.addRole(roleRepository.findByCode(RoleCode.TRAINEE).orElseThrow(IllegalStateException::new), null);
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
