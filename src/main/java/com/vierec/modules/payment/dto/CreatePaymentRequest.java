package com.vierec.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Admin records a payment. Payer, course and amount cannot change afterwards; a wrong record is marked FAILED and
 * recorded again.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "CreatePaymentRequest")
public class CreatePaymentRequest extends PaymentDetails {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "{payment.userId.required}")
    @Positive(message = "{payment.userId.positive}")
    @Schema(description = "Payer", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "{payment.courseId.required}")
    @Positive(message = "{payment.courseId.positive}")
    @Schema(example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long courseId;

    @NotNull(message = "{payment.amount.required}")
    @DecimalMin(value = "0.01", message = "{payment.amount.min}")
    @Digits(integer = 13, fraction = 2, message = "{payment.amount.digits}")
    @Schema(description = "VND", example = "1500000", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
