package com.vierec.modules.payment.service;

import com.vierec.common.dto.PageResponse;
import com.vierec.modules.payment.dto.CreatePaymentRequest;
import com.vierec.modules.payment.dto.PaymentDetails;
import com.vierec.modules.payment.dto.PaymentResponse;
import com.vierec.modules.payment.entity.PaymentStatus;

public interface PaymentService {

    /** Admin records a payment. The course name is copied so the history survives renames and deletes. */
    PaymentResponse create(CreatePaymentRequest request, String actorUsername);

    /** Newest first; every filter is optional. */
    PageResponse<PaymentResponse> search(Long userId, Long courseId, PaymentStatus status, int page, int size);

    /** Payments of the caller, newest first. */
    PageResponse<PaymentResponse> listMine(String username, PaymentStatus status, int page, int size);

    /** Admins see every payment, other users only their own (others are reported as not found). */
    PaymentResponse get(Long id, String username, boolean admin);

    /** Changes method, status, reference, note and paid time. REFUNDED is only allowed from PAID. */
    PaymentResponse update(Long id, PaymentDetails request);
}
