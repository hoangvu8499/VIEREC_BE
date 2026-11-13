package com.vierec.modules.supportpoint.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.supportpoint.dto.NearbySupportPointsResponse;
import com.vierec.modules.supportpoint.dto.SupportPointDetailResponse;
import com.vierec.modules.supportpoint.dto.SupportPointImportResponse;
import com.vierec.modules.supportpoint.dto.SupportPointRequest;
import org.springframework.web.multipart.MultipartFile;

public interface SupportPointService {

    /**
     * Support points within {@code radiusKm} of a location, nearest first. The location is the address when it is
     * given, otherwise the coordinates.
     */
    NearbySupportPointsResponse nearby(String address, Double latitude, Double longitude, int radiusKm);

    // ---------------------------------------------------------------- admin

    /** Live support points sorted by province, district and name; keyword on name, area, address or phone. */
    PageResponse<SupportPointDetailResponse> search(String keyword, int page, int size);

    SupportPointDetailResponse get(Long id);

    /**
     * Without coordinates the place is looked up from the address, district and province: 404
     * {@code ADDRESS_NOT_FOUND} when nothing matches, 503 {@code GEOCODING_UNAVAILABLE} when the lookup fails.
     * Only one of the two coordinates: 400 {@code SUPPORT_POINT_LOCATION_REQUIRED}.
     */
    SupportPointDetailResponse create(SupportPointRequest request);

    /** Same coordinate rules as {@link #create}. */
    SupportPointDetailResponse update(Long id, SupportPointRequest request);

    /** Soft delete: the point disappears from the list and from the nearby search. */
    void delete(Long id);

    /**
     * Adds one support point per row of an Excel file. All or nothing: a single unusable row saves nothing and
     * answers 400 {@code SUPPORT_POINT_IMPORT_INVALID_ROWS} listing each problem as {@code rows[<Excel row>].<field>}.
     */
    SupportPointImportResponse importPoints(MultipartFile file);
}
