package com.vierec.modules.role.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.modules.role.dto.RoleResponse;
import com.vierec.modules.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(AppConstants.API_V1 + "/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Roles that can be assigned to users")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "List every role")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.success(roleService.findAll());
    }
}
