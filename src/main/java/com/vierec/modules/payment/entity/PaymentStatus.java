package com.vierec.modules.payment.entity;

/** Values of the {@code payments.status} ENUM column. */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    /** Only reachable from {@link #PAID}. */
    REFUNDED
}
