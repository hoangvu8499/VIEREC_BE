package com.vierec.modules.supportpoint.service;

import java.util.Optional;

/** Turns a free-text address into coordinates. */
public interface GeocodingService {

    /**
     * @return empty when no place matches the address
     * @throws com.vierec.common.exception.BusinessException {@code GEOCODING_UNAVAILABLE} when the provider fails
     */
    Optional<GeocodedPlace> geocode(String address);
}
