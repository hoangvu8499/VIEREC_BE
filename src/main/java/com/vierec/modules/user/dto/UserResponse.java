package com.vierec.modules.user.dto;

import com.vierec.modules.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UserResponse", description = "User representation returned by the API")
public class UserResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "nguyenvana")
    private String username;

    @Schema(example = "Văn A")
    private String firstName;

    @Schema(example = "Nguyễn")
    private String lastName;

    @Schema(example = "001099012345")
    private String cccd;

    @Schema(example = "1999-04-08")
    private LocalDate dateOfBirth;

    private String address;

    @Schema(example = "0901234567")
    private String phoneNumber;

    @Schema(example = "nguyenvana@vierec.com")
    private String email;

    private UserStatus status;

    @Schema(description = "Wrong passwords in a row; the account is LOCKED at 5", example = "0")
    private int failedLoginCount;

    @Schema(description = "Created with a default password: the user must set their own first")
    private boolean mustChangePassword;

    /** Role codes, e.g. {@code ["TRAINEE"]}. */
    private Set<String> roles;

    @Schema(description = "Business the user learns for or manages; null for an independent learner", example = "1")
    private Long businessId;

    @Schema(example = "Công ty TNHH Môi trường Xanh")
    private String businessName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
