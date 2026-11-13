package com.vierec.modules.supportpoint.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.supportpoint.dto.SupportPointDetailResponse;
import com.vierec.modules.supportpoint.dto.SupportPointImportRequest;
import com.vierec.modules.supportpoint.dto.SupportPointImportResponse;
import com.vierec.modules.supportpoint.dto.SupportPointRequest;
import com.vierec.modules.supportpoint.service.SupportPointService;
import com.vierec.modules.supportpoint.service.SupportPointWorkbook;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;

/**
 * Admin management of the support points. The nearby search for every logged-in user stays in
 * {@link SupportPointController}.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/support-points")
@RequiredArgsConstructor
@Validated
@Tag(name = "Support points (admin)", description = "Create, edit, delete and import support points")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
@AdminOnly
public class SupportPointAdminController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final SupportPointService supportPointService;
    private final SupportPointWorkbook workbook;

    @GetMapping
    @Operation(summary = "Support points sorted by province, district and name",
            description = "keyword matches the name, province, district, address or phone number.")
    public ApiResponse<PageResponse<SupportPointDetailResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(supportPointService.search(keyword, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One support point", description = "404 SUPPORT_POINT_NOT_FOUND (VRC-404-602).")
    public ApiResponse<SupportPointDetailResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(supportPointService.get(id));
    }

    @PostMapping
    @Operation(summary = "Create a support point",
            description = "Without latitude and longitude the place is looked up from address, district and "
                    + "province: 404 ADDRESS_NOT_FOUND (VRC-404-601) when nothing matches, 503 "
                    + "GEOCODING_UNAVAILABLE (VRC-503-601) when the lookup fails. Only one coordinate: 400 "
                    + "SUPPORT_POINT_LOCATION_REQUIRED (VRC-400-601).")
    public ResponseEntity<ApiResponse<SupportPointDetailResponse>> create(
            @Valid @RequestBody SupportPointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                supportPointService.create(request), "Support point created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a support point", description = "Same coordinate rules as the creation.")
    public ApiResponse<SupportPointDetailResponse> update(@PathVariable @Positive Long id,
                                                          @Valid @RequestBody SupportPointRequest request) {
        return ApiResponse.success(supportPointService.update(id, request), "Support point updated");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a support point (soft delete)")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        supportPointService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add support points from an Excel file (multipart/form-data)",
            description = "At most " + SupportPointWorkbook.MAX_POINTS + " rows; every row needs both "
                    + "coordinates. All or nothing: a single unusable row saves nothing and answers 400 "
                    + "SUPPORT_POINT_IMPORT_INVALID_ROWS (VRC-400-602) listing every problem in errors as "
                    + "rows[<Excel row>].<field>.")
    public ResponseEntity<ApiResponse<SupportPointImportResponse>> importPoints(
            @Valid @ModelAttribute SupportPointImportRequest request) {
        SupportPointImportResponse result = supportPointService.importPoints(request.getFile());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result,
                "Imported " + result.getImportedCount() + " support points"));
    }

    @GetMapping("/import-template")
    @Operation(summary = "Download the Excel template for the support point import")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("mau-diem-ho-tro-su-co.xlsx", StandardCharsets.UTF_8).build().toString())
                .body(workbook.template());
    }
}
