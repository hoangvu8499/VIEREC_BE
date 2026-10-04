package com.vierec.modules.business.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.business.dto.BusinessEnrollRequest;
import com.vierec.modules.business.dto.BusinessEnrollResponse;
import com.vierec.modules.business.dto.BusinessMemberDetailResponse;
import com.vierec.modules.business.dto.BusinessMemberResponse;
import com.vierec.modules.business.dto.BusinessResponse;
import com.vierec.modules.business.service.BusinessService;
import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.security.access.BusinessManagerOnly;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

/**
 * The logged-in business manager's own business. Every endpoint answers 403 NOT_A_BUSINESS_MANAGER
 * (VRC-403-801) for an account without a business and 403 BUSINESS_INACTIVE (VRC-403-802) once it is deactivated.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/my-business")
@RequiredArgsConstructor
@Validated
@Tag(name = "My business", description = "Learners of the logged-in manager's business")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
@BusinessManagerOnly
public class MyBusinessController {

    private final BusinessService businessService;

    @GetMapping
    public ApiResponse<BusinessResponse> mine(@Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(businessService.mine(authentication.getName()));
    }

    @GetMapping("/members")
    @Operation(summary = "Learners of my business with a summary of their courses, newest first")
    public ApiResponse<PageResponse<BusinessMemberResponse>> members(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size,
            @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(businessService.myMembers(authentication.getName(), keyword, page, size));
    }

    @GetMapping("/certificates")
    @Operation(summary = "Certificates of the learners of my business, latest first",
            description = "keyword matches the certificate code, the learner's name or the course name.")
    public ApiResponse<PageResponse<CertificateResponse>> certificates(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size,
            @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(businessService.myCertificates(authentication.getName(), keyword, page, size));
    }

    @GetMapping("/certificates/{code}")
    @Operation(summary = "One certificate of a learner of my business")
    public ApiResponse<CertificateResponse> certificate(
            @PathVariable @Size(max = 30, message = "{certificate.code.size}") String code,
            @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(businessService.myCertificate(authentication.getName(), code));
    }

    @PostMapping("/members")
    @Operation(summary = "Create a learner account (role TRAINEE) in my business",
            description = "Same fields and rules as the self-registration.")
    public ResponseEntity<ApiResponse<BusinessMemberResponse>> createMember(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                businessService.createMember(authentication.getName(), request), "Learner created"));
    }

    @GetMapping("/members/{userId}")
    @Operation(summary = "Courses of one of my learners: video progress, exam result, certificate",
            description = "404 BUSINESS_MEMBER_NOT_FOUND (VRC-404-802) for a user outside my business.")
    public ApiResponse<BusinessMemberDetailResponse> member(@PathVariable @Positive Long userId,
                                                            @Parameter(hidden = true)
                                                            Authentication authentication) {
        return ApiResponse.success(businessService.myMember(authentication.getName(), userId));
    }

    @PostMapping("/enrollments")
    @Operation(summary = "Enroll some of my learners in a course (PENDING until an admin approves the transfer)",
            description = "Learners already waiting for, learning or having completed the course are skipped. "
                    + "400 BUSINESS_ENROLL_NOT_MEMBERS (VRC-400-801) when a user is not a learner of my business.")
    public ApiResponse<BusinessEnrollResponse> enroll(@Valid @RequestBody BusinessEnrollRequest request,
                                                      @Parameter(hidden = true) Authentication authentication) {
        return ApiResponse.success(businessService.enroll(authentication.getName(), request), "Enrollments created");
    }
}
