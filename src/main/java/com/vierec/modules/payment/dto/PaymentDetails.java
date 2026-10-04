package com.vierec.modules.payment.dto;

import com.vierec.modules.payment.entity.PaymentMethod;
import com.vierec.modules.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Fields an admin may set on create and change later ({@code PUT /payments/{id}}). {@code transactionRef} and
 * {@code note} are replaced as sent: omitting them clears them.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "PaymentDetails")
public class PaymentDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "{payment.method.required}")
    @Schema(example = "BANK_TRANSFER", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethod method;

    @Schema(description = "Default PENDING on create; omitted on update keeps the current status. "
            + "REFUNDED only from PAID.", example = "PAID")
    private PaymentStatus status;

    @Size(max = 100, message = "{payment.transactionRef.size}")
    @Schema(example = "FT26269123456")
    private String transactionRef;

    @Size(max = 500, message = "{payment.note.size}")
    @Schema(example = "Chuyển khoản Vietcombank")
    private String note;

    @PastOrPresent(message = "{payment.paidAt.past}")
    @Schema(description = "When the money was received; defaults to now when the status becomes PAID. "
            + "Ignored for PENDING and FAILED.", example = "2026-09-26T10:00:00")
    private LocalDateTime paidAt;
}
