package com.vierec.modules.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Partial update: every field is optional, {@code null} means "leave unchanged".
 * A field that is sent must follow the same format rules as registration.
 * Roles are changed through {@code PUT /api/v1/users/{id}/roles}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateUserRequest", description = "Fields to update; omit a field to leave it unchanged")
public class UpdateUserRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    // A sent value must not be blank: the regexes and @Size(min = 1) reject "" and whitespace.
    @Size(min = 1, max = 50, message = "{user.firstName.size}")
    @Pattern(regexp = "^.*\\S.*$", message = "{user.firstName.required}")
    private String firstName;

    @Size(min = 1, max = 50, message = "{user.lastName.size}")
    @Pattern(regexp = "^.*\\S.*$", message = "{user.lastName.required}")
    private String lastName;

    @Pattern(regexp = RegisterRequest.CCCD_REGEX, message = "{user.cccd.invalid}")
    private String cccd;

    @Past(message = "{user.dateOfBirth.past}")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Size(min = 1, max = 255, message = "{user.address.size}")
    @Pattern(regexp = "^.*\\S.*$", message = "{user.address.required}")
    private String address;

    @Pattern(regexp = RegisterRequest.PHONE_REGEX, message = "{user.phone.invalid}")
    private String phoneNumber;

    @Size(min = 1, max = 255, message = "{user.email.size}")
    @Email(regexp = RegisterRequest.EMAIL_REGEX, message = "{user.email.invalid}")
    private String email;

    private UserStatus status;
}
