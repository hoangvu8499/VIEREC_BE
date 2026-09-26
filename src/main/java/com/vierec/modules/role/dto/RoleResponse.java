package com.vierec.modules.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RoleResponse")
public class RoleResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(example = "2")
    private Integer id;

    @Schema(description = "Value to send in the roles of a user", example = "ADMIN")
    private String code;

    @Schema(example = "Admin")
    private String name;

    @Schema(example = "Quản trị viên")
    private String description;
}
