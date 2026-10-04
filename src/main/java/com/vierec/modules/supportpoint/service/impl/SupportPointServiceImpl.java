package com.vierec.modules.supportpoint.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.supportpoint.dto.NearbySupportPointsResponse;
import com.vierec.modules.supportpoint.dto.SupportPointResponse;
import com.vierec.modules.supportpoint.mapper.SupportPointMapper;
import com.vierec.modules.supportpoint.repository.SupportPointRepository;
import com.vierec.modules.supportpoint.service.GeocodedPlace;
import com.vierec.modules.supportpoint.service.GeocodingService;
import com.vierec.modules.supportpoint.service.SupportPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportPointServiceImpl implements SupportPointService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    /** Kilometres per degree of latitude (and of longitude at the equator). */
    private static final double KM_PER_DEGREE = 111.32;

    private final SupportPointRepository supportPointRepository;
    private final GeocodingService geocodingService;
    private final SupportPointMapper supportPointMapper;

    @Override
    public NearbySupportPointsResponse nearby(String address, Double latitude, Double longitude, int radiusKm) {
        NearbySupportPointsResponse.Origin origin = origin(address, latitude, longitude);
        double lat = origin.getLatitude();
        double lng = origin.getLongitude();

        // Box around the origin first (uses the index), then the exact distance.
        double latDelta = radiusKm / KM_PER_DEGREE;
        double lngDelta = radiusKm / (KM_PER_DEGREE * Math.max(Math.cos(Math.toRadians(lat)), 0.01));
        List<SupportPointResponse> results = supportPointRepository.findInBox(
                        BigDecimal.valueOf(lat - latDelta), BigDecimal.valueOf(lat + latDelta),
                        BigDecimal.valueOf(lng - lngDelta), BigDecimal.valueOf(lng + lngDelta)).stream()
                .map(point -> supportPointMapper.toResponse(point, roundKm(haversineKm(lat, lng,
                        point.getLatitude().doubleValue(), point.getLongitude().doubleValue()))))
                .filter(point -> point.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(SupportPointResponse::getDistanceKm))
                .collect(Collectors.toList());

        return NearbySupportPointsResponse.builder()
                .origin(origin)
                .radiusKm(radiusKm)
                .results(results)
                .build();
    }

    private NearbySupportPointsResponse.Origin origin(String address, Double latitude, Double longitude) {
        if (StringUtils.hasText(address)) {
            GeocodedPlace place = geocodingService.geocode(address)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ADDRESS_NOT_FOUND));
            return new NearbySupportPointsResponse.Origin(place.getLatitude(), place.getLongitude(),
                    place.getLabel());
        }
        if (latitude == null || longitude == null) {
            throw new BusinessException(ErrorCode.SUPPORT_POINT_LOCATION_REQUIRED);
        }
        return new NearbySupportPointsResponse.Origin(latitude, longitude, null);
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double roundKm(double km) {
        return Math.round(km * 1000) / 1000.0;
    }
}
