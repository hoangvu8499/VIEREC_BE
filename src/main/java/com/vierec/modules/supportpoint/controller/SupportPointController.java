package com.vierec.modules.supportpoint.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.supportpoint.dto.NearbySupportPointsResponse;
import com.vierec.modules.supportpoint.service.SupportPointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@RestController
@RequestMapping(AppConstants.API_V1)
@RequiredArgsConstructor
@Validated
@Tag(name = "Support points", description = "Units that handle environmental incidents, searched by location")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class SupportPointController {

    private final SupportPointService supportPointService;

    @GetMapping("/support-points/nearby")
    @Operation(summary = "Support points around an address or coordinates, nearest first (login required)",
            description = "Give address (resolved with OpenStreetMap Nominatim) or latitude + longitude. "
                    + "VRC-404-601 when the address is not found, VRC-503-601 when geocoding is unavailable.")
    public ApiResponse<NearbySupportPointsResponse> nearby(
            @Parameter(description = "Free-text address, e.g. Ngũ Hành Sơn, Đà Nẵng")
            @RequestParam(required = false) @Size(max = 255, message = "{supportPoint.address.size}") String address,
            @RequestParam(required = false) @DecimalMin(value = "-90", message = "{supportPoint.latitude.range}")
            @DecimalMax(value = "90", message = "{supportPoint.latitude.range}") Double latitude,
            @RequestParam(required = false) @DecimalMin(value = "-180", message = "{supportPoint.longitude.range}")
            @DecimalMax(value = "180", message = "{supportPoint.longitude.range}") Double longitude,
            @Parameter(description = "Search radius in km") @RequestParam(defaultValue = "3")
            @Min(value = 1, message = "{supportPoint.radius.range}")
            @Max(value = 50, message = "{supportPoint.radius.range}") int radiusKm) {
        return ApiResponse.success(supportPointService.nearby(address, latitude, longitude, radiusKm));
    }
}
