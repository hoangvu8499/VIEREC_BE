package com.vierec.modules.business.dto;

import com.vierec.common.validation.RequiredFile;
import com.vierec.modules.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * Multipart form of {@code POST /api/v1/my-business/members/import}: the learner sheet plus the one password
 * every account in it starts with. The password is not a column of the file, so it never travels row by row.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "defaultPassword")
@Schema(name = "BusinessMemberImportRequest")
public class BusinessMemberImportRequest {

    @RequiredFile(message = "{memberImport.file.required}")
    @Schema(description = "Excel file (.xlsx or .xls) laid out like"
            + " GET /api/v1/my-business/members/import-template",
            type = "string", format = "binary")
    private MultipartFile file;

    /** Same rules as a self-registered password: every imported account gets this one. */
    @NotBlank(message = "{memberImport.defaultPassword.required}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    @Pattern(regexp = RegisterRequest.PASSWORD_REGEX, message = "{user.password.special}")
    @Schema(example = "Vierec@2026", requiredMode = Schema.RequiredMode.REQUIRED,
            description = "Default password chosen by the business for every learner in the file")
    private String defaultPassword;
}
