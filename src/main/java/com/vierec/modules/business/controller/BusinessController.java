package com.vierec.modules.business.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.business.dto.BusinessManagerRequest;
import com.vierec.modules.business.dto.BusinessMemberDetailResponse;
import com.vierec.modules.business.dto.BusinessMemberResponse;
import com.vierec.modules.business.dto.BusinessRequest;
import com.vierec.modules.business.dto.BusinessResponse;
import com.vierec.modules.business.dto.CreateBusinessRequest;
import com.vierec.modules.business.entity.BusinessStatus;
import com.vierec.modules.business.service.BusinessService;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.util.List;

/** Corporate customers, managed by admins. */
@RestController
@RequestMapping(AppConstants.API_V1 + "/businesses")
@RequiredArgsConstructor
@Validated
@Tag(name = "Businesses", description = "Corporate customers, their manager accounts and their learners (admin)")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
@AdminOnly
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    @Operation(summary = "Search businesses by name or tax code, newest first")
    public ApiResponse<PageResponse<BusinessResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BusinessStatus status,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(businessService.search(keyword, status, page, size));
    }

    @PostMapping
    @Operation(summary = "Create a business with its first manager account (role BUSINESS)",
            description = "409 TAX_CODE_ALREADY_EXISTS (VRC-409-801); a taken username / email / phone of the "
                    + "manager cancels the whole creation.")
    public ResponseEntity<ApiResponse<BusinessResponse>> create(@Valid @RequestBody CreateBusinessRequest request,
                                                                @Parameter(hidden = true)
                                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                businessService.create(request, authentication.getName()), "Business created"));
    }

    @GetMapping("/{id}")
    public ApiResponse<BusinessResponse> get(@PathVariable @Positive Long id) {
        return ApiResponse.success(businessService.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Change the details or the status of a business",
            description = "INACTIVE blocks the managers' business features (403 BUSINESS_INACTIVE); the learners "
                    + "keep learning.")
    public ApiResponse<BusinessResponse> update(@PathVariable @Positive Long id,
                                                @Valid @RequestBody BusinessRequest request) {
        return ApiResponse.success(businessService.update(id, request), "Business updated");
    }

    @GetMapping("/{id}/managers")
    public ApiResponse<List<UserResponse>> managers(@PathVariable @Positive Long id) {
        return ApiResponse.success(businessService.managers(id));
    }

    @PostMapping("/{id}/managers")
    @Operation(summary = "Add a manager account to a business")
    public ResponseEntity<ApiResponse<UserResponse>> addManager(@PathVariable @Positive Long id,
                                                                @Valid @RequestBody BusinessManagerRequest request,
                                                                @Parameter(hidden = true)
                                                                Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                businessService.addManager(id, request, authentication.getName()), "Manager created"));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Learners of a business with a summary of their courses")
    public ApiResponse<PageResponse<BusinessMemberResponse>> members(
            @PathVariable @Positive Long id,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(businessService.members(id, keyword, page, size));
    }

    @GetMapping("/{id}/members/{userId}")
    @Operation(summary = "Courses of a learner of the business: progress, exam result, certificate")
    public ApiResponse<BusinessMemberDetailResponse> member(@PathVariable @Positive Long id,
                                                            @PathVariable @Positive Long userId) {
        return ApiResponse.success(businessService.member(id, userId));
    }
}
