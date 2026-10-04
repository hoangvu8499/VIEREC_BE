package com.vierec.modules.certificate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Full certificate, for its owner and admins. Name, date of birth and CCCD are the values at issue time. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CertificateResponse")
public class CertificateResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "3")
    private Long id;

    @Schema(example = "VRC-2026-7K3QX9PA")
    private String code;

    @Schema(example = "15")
    private Long enrollmentId;

    @Schema(example = "7")
    private Long courseId;

    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String courseName;

    @Schema(description = "Owner of the certificate", example = "12")
    private Long userId;

    @Schema(example = "Nguyễn Văn An")
    private String fullName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(example = "1995-01-01", type = "string", format = "date")
    private LocalDate dateOfBirth;

    @Schema(example = "099000000001")
    private String cccd;

    private LocalDateTime issuedAt;

    @Schema(description = "PDF, downloadable by the owner, the managers of their business and admins; add "
            + "?download=true to save it instead of opening it", example = "/api/v1/files/42")
    private String fileUrl;
}
