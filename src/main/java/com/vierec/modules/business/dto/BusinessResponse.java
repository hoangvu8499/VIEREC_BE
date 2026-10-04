package com.vierec.modules.business.dto;

import com.vierec.modules.business.entity.BusinessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "BusinessResponse")
public class BusinessResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Công ty TNHH Môi trường Xanh")
    private String name;

    @Schema(example = "0101234567")
    private String taxCode;

    private String address;

    private String phoneNumber;

    private String email;

    private BusinessStatus status;

    @Schema(description = "Accounts with the BUSINESS role", example = "1")
    private long managerCount;

    @Schema(description = "Learners of the business", example = "25")
    private long memberCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
