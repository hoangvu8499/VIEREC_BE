package com.vierec.modules.supportpoint.service;

import com.vierec.modules.supportpoint.dto.NearbySupportPointsResponse;

public interface SupportPointService {

    /**
     * Support points within {@code radiusKm} of a location, nearest first. The location is the address when it is
     * given, otherwise the coordinates.
     */
    NearbySupportPointsResponse nearby(String address, Double latitude, Double longitude, int radiusKm);
}
