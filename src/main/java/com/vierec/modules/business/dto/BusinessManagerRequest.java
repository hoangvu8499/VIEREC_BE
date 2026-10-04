package com.vierec.modules.business.dto;

import com.vierec.modules.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/** Login account of a business manager: the registration rules without CCCD, date of birth and address. */
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
@Schema(name = "BusinessManagerRequest")
public class BusinessManagerRequest {

    @NotBlank(message = "{user.username.required}")
    @Size(min = 3, max = 50, message = "{user.username.size}")
    @Pattern(regexp = RegisterRequest.USERNAME_REGEX, message = "{user.username.pattern}")
    @Schema(example = "mtxanh.admin")
    private String username;

    @NotBlank(message = "{user.password.required}")
    @Size(min = 8, max = 72, message = "{user.password.size}")
    @Pattern(regexp = RegisterRequest.PASSWORD_REGEX, message = "{user.password.special}")
    @Schema(example = "Matkhau@123")
    private String password;

    @NotBlank(message = "{user.firstName.required}")
    @Size(max = 50, message = "{user.firstName.size}")
    @Schema(example = "Thị Bình")
    private String firstName;

    @NotBlank(message = "{user.lastName.required}")
    @Size(max = 50, message = "{user.lastName.size}")
    @Schema(example = "Trần")
    private String lastName;

    @NotBlank(message = "{user.phone.required}")
    @Pattern(regexp = RegisterRequest.PHONE_REGEX, message = "{user.phone.invalid}")
    @Schema(example = "0912345678")
    private String phoneNumber;

    @NotBlank(message = "{user.email.required}")
    @Email(regexp = RegisterRequest.EMAIL_REGEX, message = "{user.email.invalid}")
    @Size(max = 255, message = "{user.email.size}")
    @Schema(example = "binh.tran@moitruongxanh.vn")
    private String email;

    public RegisterRequest toRegisterRequest() {
        return RegisterRequest.builder()
                .username(username)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .email(email)
                .build();
    }
}
