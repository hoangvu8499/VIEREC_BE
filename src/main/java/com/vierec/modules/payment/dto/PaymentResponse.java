package com.vierec.modules.payment.dto;

import com.vierec.modules.payment.entity.PaymentMethod;
import com.vierec.modules.payment.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One payment with the payer's contact fields and the course, flat so that it maps onto a table row. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PaymentResponse")
public class PaymentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "12")
    private Long id;

    @Schema(example = "3")
    private Long userId;

    @Schema(example = "nguyenvana")
    private String username;

    @Schema(description = "Payer: last name + first name", example = "Nguyễn Văn An")
    private String payerName;

    @Schema(example = "an@example.com")
    private String payerEmail;

    @Schema(example = "0990000001")
    private String payerPhone;

    @Schema(example = "7")
    private Long courseId;

    @Schema(description = "Course name when the payment was recorded", example = "Phòng cháy chữa cháy cơ bản")
    private String courseName;

    @Schema(description = "VND", example = "1500000.00")
    private BigDecimal amount;

    @Schema(example = "BANK_TRANSFER")
    private PaymentMethod method;

    @Schema(example = "PAID")
    private PaymentStatus status;

    @Schema(example = "FT26269123456")
    private String transactionRef;

    private String note;

    private LocalDateTime paidAt;

    @Schema(description = "Admin who recorded the payment", example = "quantri")
    private String createdByUsername;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
