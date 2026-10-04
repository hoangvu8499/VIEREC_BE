package com.vierec.modules.supportpoint;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.config.properties.GeocodingProperties;
import com.vierec.modules.supportpoint.service.GeocodedPlace;
import com.vierec.modules.supportpoint.service.impl.NominatimGeocodingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class NominatimGeocodingServiceTest {

    private MockRestServiceServer server;
    private NominatimGeocodingService service;

    @BeforeEach
    void setUp() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        GeocodingProperties properties = new GeocodingProperties();
        service = new NominatimGeocodingService(new RestTemplateBuilder(customizer), properties);
        server = customizer.getServer();
    }

    @Test
    void asksNominatimOnceForAnAddressInVietnam() {
        server.expect(once(), requestTo(startsWith("https://nominatim.openstreetmap.org/search?")))
                .andExpect(queryParam("countrycodes", "vn"))
                .andExpect(queryParam("limit", "1"))
                .andExpect(header(HttpHeaders.USER_AGENT, "VIEREC-Academy/1.0"))
                .andRespond(withSuccess("[{\"lat\":\"15.9967\",\"lon\":\"108.2462\","
                        + "\"display_name\":\"Ngũ Hành Sơn, Đà Nẵng, Việt Nam\",\"importance\":0.5}]",
                        MediaType.APPLICATION_JSON));

        Optional<GeocodedPlace> place = service.geocode("  Ngũ Hành Sơn, Đà Nẵng ");
        // Same address, other spacing and case: answered from the cache.
        Optional<GeocodedPlace> again = service.geocode("ngũ hành sơn, đà nẵng");

        assertThat(place).isPresent();
        assertThat(place.get().getLatitude()).isEqualTo(15.9967);
        assertThat(place.get().getLongitude()).isEqualTo(108.2462);
        assertThat(place.get().getLabel()).isEqualTo("Ngũ Hành Sơn, Đà Nẵng, Việt Nam");
        assertThat(again).isEqualTo(place);
        server.verify();
    }

    @Test
    void anEmptyAnswerMeansNotFound() {
        server.expect(once(), requestTo(startsWith("https://nominatim.openstreetmap.org/search?")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(service.geocode("không có nơi này")).isEmpty();
    }

    @Test
    void aFailingProviderIsReportedAsUnavailable() {
        server.expect(once(), requestTo(startsWith("https://nominatim.openstreetmap.org/search?")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.geocode("Đà Nẵng"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GEOCODING_UNAVAILABLE);
    }
}
