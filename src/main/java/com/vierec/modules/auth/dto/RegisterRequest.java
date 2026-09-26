package com.vierec.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Self-registration payload. Every field is mandatory, even those nullable in the {@code users} table.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
@Schema(name = "RegisterRequest", description = "Thông tin đăng ký tài khoản")
public class RegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Letters, digits, dot, underscore, hyphen. */
    public static final String USERNAME_REGEX = "^[a-zA-Z0-9._-]+$";
    /** At least one character that is neither a letter nor a digit. */
    public static final String PASSWORD_REGEX = "^(?=.*[^A-Za-z0-9]).+$";
    /** Local part, "@", then a domain containing at least one dot. */
    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$";
    public static final String CCCD_REGEX = "^[0-9]{12}$";
    public static final String PHONE_REGEX = "^[0-9]{10}$";

    @NotBlank(message = "{user.username.required}")
    @Size(min = 3, max = 50, message = "{user.username.size}")
    @Pattern(regexp = USERNAME_REGEX, message = "{user.username.pattern}")
    @Schema(example = "nguyenvana", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    // BCrypt only uses the first 72 bytes, so longer passwords are rejected instead of silently truncated.
    @NotBlank(message = "{user.password.required}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    @Pattern(regexp = PASSWORD_REGEX, message = "{user.password.special}")
    @Schema(example = "Matkhau@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "{user.firstName.required}")
    @Size(max = 50, message = "{user.firstName.size}")
    @Schema(example = "Văn A", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(message = "{user.lastName.required}")
    @Size(max = 50, message = "{user.lastName.size}")
    @Schema(example = "Nguyễn", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @NotBlank(message = "{user.cccd.required}")
    @Pattern(regexp = CCCD_REGEX, message = "{user.cccd.invalid}")
    @Schema(example = "001099012345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cccd;

    @NotNull(message = "{user.dateOfBirth.required}")
    @Past(message = "{user.dateOfBirth.past}")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(example = "1999-04-08", type = "string", format = "date", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateOfBirth;

    @NotBlank(message = "{user.address.required}")
    @Size(max = 255, message = "{user.address.size}")
    @Schema(example = "12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @NotBlank(message = "{user.phone.required}")
    @Pattern(regexp = PHONE_REGEX, message = "{user.phone.invalid}")
    @Schema(example = "0901234567", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;

    @NotBlank(message = "{user.email.required}")
    @Email(regexp = EMAIL_REGEX, message = "{user.email.invalid}")
    @Size(max = 255, message = "{user.email.size}")
    @Schema(example = "nguyenvana@vierec.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}
