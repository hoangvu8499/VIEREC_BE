package com.vierec.modules.certificate.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.certificate.dto.CertificateResponse;
import com.vierec.modules.certificate.dto.CertificateVerificationResponse;
import com.vierec.modules.certificate.dto.IssueCertificateRequest;
import com.vierec.modules.certificate.service.CertificateService;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

@RestController
@RequestMapping(AppConstants.API_V1)
@RequiredArgsConstructor
@Validated
@Tag(name = "Certificates", description = "Issuing, listing and verifying course certificates")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping(value = "/courses/{courseId}/enrollments/{enrollmentId}/certificate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AdminOnly
    @Operation(summary = "Issue the certificate of a COMPLETED enrollment with its PDF",
            description = "Name, date of birth and CCCD of the trainee are copied at issue time.")
    public ResponseEntity<ApiResponse<CertificateResponse>> issue(@PathVariable @Positive Long courseId,
                                                                  @PathVariable @Positive Long enrollmentId,
                                                                  @Valid @ModelAttribute
                                                                  IssueCertificateRequest request,
                                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                certificateService.issue(courseId, enrollmentId, request.getFile(), authentication.getName()),
                "Certificate issued"));
    }

    @GetMapping("/auth/me/certificates")
    @Operation(summary = "Certificates of the logged-in user, newest first")
    public ApiResponse<PageResponse<CertificateResponse>> mine(
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size,
            Authentication authentication) {
        return ApiResponse.success(certificateService.listMine(authentication.getName(), page, size));
    }

    @GetMapping("/auth/me/certificates/{code}")
    @Operation(summary = "One certificate of the logged-in user by its code")
    public ApiResponse<CertificateResponse> mineByCode(
            @PathVariable @Size(max = 30, message = "{certificate.code.size}") String code,
            Authentication authentication) {
        return ApiResponse.success(certificateService.getMine(authentication.getName(), code));
    }

    @GetMapping("/certificates/{code}")
    @SecurityRequirements
    @Operation(summary = "Verify a certificate by its code (no login required)",
            description = "Returns no date of birth and no file; the CCCD is masked.")
    public ApiResponse<CertificateVerificationResponse> verify(
            @PathVariable @Size(max = 30, message = "{certificate.code.size}") String code) {
        return ApiResponse.success(certificateService.verify(code));
    }
}
