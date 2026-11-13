package com.vierec.modules.supportpoint.dto;

import com.vierec.common.validation.RequiredFile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/** Multipart form of {@code POST /api/v1/support-points/import}. */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "SupportPointImportRequest")
public class SupportPointImportRequest {

    @RequiredFile(message = "{supportPointImport.file.required}")
    @Schema(description = "Excel file (.xlsx or .xls) laid out like GET /api/v1/support-points/import-template",
            type = "string", format = "binary")
    private MultipartFile file;
}
