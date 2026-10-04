package com.vierec.modules.payment.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.payment.dto.CreatePaymentRequest;
import com.vierec.modules.payment.dto.PaymentDetails;
import com.vierec.modules.payment.dto.PaymentResponse;
import com.vierec.modules.payment.entity.PaymentStatus;
import com.vierec.modules.payment.service.PaymentService;
import com.vierec.security.SecurityUtils;
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

@RestController
@RequestMapping(AppConstants.API_V1)
@RequiredArgsConstructor
@Validated
@Tag(name = "Payments", description = "Payment history of course registrations (recorded by admins)")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payments")
    @AdminOnly
    @Operation(summary = "Record a payment of a user for a course",
            description = "History only: does not enroll the user. Payer, course and amount cannot change later.")
    public ResponseEntity<ApiResponse<PaymentResponse>> create(@Valid @RequestBody CreatePaymentRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(paymentService.create(request, authentication.getName()),
                        "Payment recorded"));
    }

    @GetMapping("/payments")
    @AdminOnly
    @Operation(summary = "Search payments, newest first")
    public ApiResponse<PageResponse<PaymentResponse>> search(
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long courseId,
            @RequestParam(required = false) PaymentStatus status,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size) {
        return ApiResponse.success(paymentService.search(userId, courseId, status, page, size));
    }

    @GetMapping("/auth/me/payments")
    @Operation(summary = "Payment history of the logged-in user, newest first")
    public ApiResponse<PageResponse<PaymentResponse>> mine(
            @RequestParam(required = false) PaymentStatus status,
            @Parameter(description = "0-based page number") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "{page.min}") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "{size.range}")
            @Max(value = AppConstants.MAX_PAGE_SIZE, message = "{size.range}") int size,
            Authentication authentication) {
        return ApiResponse.success(paymentService.listMine(authentication.getName(), status, page, size));
    }

    @GetMapping("/payments/{id}")
    @Operation(summary = "One payment", description = "Admins see every payment, other users only their own.")
    public ApiResponse<PaymentResponse> get(@PathVariable @Positive Long id, Authentication authentication) {
        return ApiResponse.success(paymentService.get(id, authentication.getName(),
                SecurityUtils.isAdmin(authentication)));
    }

    @PutMapping("/payments/{id}")
    @AdminOnly
    @Operation(summary = "Update method, status, transaction reference, note and paid time",
            description = "transactionRef and note are replaced as sent (omitted = cleared). "
                    + "REFUNDED is only allowed from PAID (VRC-409-501).")
    public ApiResponse<PaymentResponse> update(@PathVariable @Positive Long id,
                                               @Valid @RequestBody PaymentDetails request) {
        return ApiResponse.success(paymentService.update(id, request), "Payment updated");
    }
}
