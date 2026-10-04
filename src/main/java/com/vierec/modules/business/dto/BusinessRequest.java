package com.vierec.modules.business.dto;

import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.business.entity.BusinessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Business details, for {@code PUT /businesses/{id}} (and the first part of the creation). */
@Getter
@Setter
@NoArgsConstructor
@Schema(name = "BusinessRequest")
public class BusinessRequest {

    /** Vietnamese tax code: 10 digits, or 10 digits, a dash and 3 digits for a branch. */
    public static final String TAX_CODE_REGEX = "^[0-9]{10}(-[0-9]{3})?$";

    @NotBlank(message = "{business.name.required}")
    @Size(max = 200, message = "{business.name.size}")
    @Schema(example = "Công ty TNHH Môi trường Xanh")
    private String name;

    @NotBlank(message = "{business.taxCode.required}")
    @Pattern(regexp = TAX_CODE_REGEX, message = "{business.taxCode.invalid}")
    @Schema(example = "0101234567")
    private String taxCode;

    @NotBlank(message = "{user.address.required}")
    @Size(max = 255, message = "{user.address.size}")
    @Schema(example = "12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh")
    private String address;

    @NotBlank(message = "{user.phone.required}")
    @Pattern(regexp = RegisterRequest.PHONE_REGEX, message = "{user.phone.invalid}")
    @Schema(example = "0281234567")
    private String phoneNumber;

    @NotBlank(message = "{user.email.required}")
    @Email(regexp = RegisterRequest.EMAIL_REGEX, message = "{user.email.invalid}")
    @Size(max = 255, message = "{user.email.size}")
    @Schema(example = "lienhe@moitruongxanh.vn")
    private String email;

    @Schema(description = "Defaults to ACTIVE on creation; INACTIVE blocks the managers' business features")
    private BusinessStatus status;
}
