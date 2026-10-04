package com.vierec.modules.certificate.dto;

import com.vierec.common.validation.RequiredFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** Multipart form of the issue call; the PDF is prepared by the admin. */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "IssueCertificateRequest")
public class IssueCertificateRequest {

    @RequiredFile(message = "{certificate.file.required}")
    @Schema(description = "Certificate PDF", type = "string", format = "binary",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
}
