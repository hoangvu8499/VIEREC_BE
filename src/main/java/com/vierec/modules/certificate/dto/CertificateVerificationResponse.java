package com.vierec.modules.certificate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Public view of a certificate: no date of birth, no file, CCCD masked. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CertificateVerificationResponse")
public class CertificateVerificationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "VRC-2026-7K3QX9PA")
    private String code;

    @Schema(example = "Phòng cháy chữa cháy cơ bản")
    private String courseName;

    @Schema(example = "Nguyễn Văn An")
    private String fullName;

    private LocalDateTime issuedAt;

    @Schema(description = "First 4 and last 2 digits; null when no CCCD was recorded", example = "0990******01")
    private String cccdMasked;
}
