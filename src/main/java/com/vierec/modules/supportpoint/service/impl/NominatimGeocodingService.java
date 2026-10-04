package com.vierec.modules.supportpoint.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.config.properties.GeocodingProperties;
import com.vierec.modules.supportpoint.service.GeocodedPlace;
import com.vierec.modules.supportpoint.service.GeocodingService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Geocoding with OpenStreetMap Nominatim (free, no key). Its usage policy asks for a real User-Agent and at most
 * one request per second, so answers are cached in memory: the same address is looked up only once.
 */
@Slf4j
@Service
public class NominatimGeocodingService implements GeocodingService {

    private static final int CACHE_SIZE = 500;

    private final RestTemplate restTemplate;
    private final GeocodingProperties properties;
    private final Map<String, Optional<GeocodedPlace>> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, Optional<GeocodedPlace>>(16, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Optional<GeocodedPlace>> eldest) {
                    return size() > CACHE_SIZE;
                }
            });

    public NominatimGeocodingService(RestTemplateBuilder builder, GeocodingProperties properties) {
        this.restTemplate = builder.setConnectTimeout(properties.getTimeout())
                .setReadTimeout(properties.getTimeout())
                .build();
        this.properties = properties;
    }

    @Override
    public Optional<GeocodedPlace> geocode(String address) {
        String key = address.trim().toLowerCase(Locale.ROOT);
        Optional<GeocodedPlace> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<GeocodedPlace> place = lookUp(address.trim());
        cache.put(key, place);
        return place;
    }

    private Optional<GeocodedPlace> lookUp(String address) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .queryParam("countrycodes", properties.getCountryCodes())
                .queryParam("accept-language", "vi")
                .encode()
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, properties.getUserAgent());
        try {
            NominatimPlace[] places = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers),
                    NominatimPlace[].class).getBody();
            if (places == null || places.length == 0) {
                return Optional.empty();
            }
            NominatimPlace first = places[0];
            return Optional.of(new GeocodedPlace(Double.parseDouble(first.getLat()),
                    Double.parseDouble(first.getLon()), first.getDisplayName()));
        } catch (RestClientException | NumberFormatException ex) {
            log.warn("Geocoding failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.GEOCODING_UNAVAILABLE);
        }
    }

    /** One element of the Nominatim search answer. */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class NominatimPlace {

        private String lat;
        private String lon;

        @JsonProperty("display_name")
        private String displayName;
    }
}
