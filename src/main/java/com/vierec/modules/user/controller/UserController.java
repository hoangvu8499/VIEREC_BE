package com.vierec.modules.user.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.common.dto.ApiResponse;
import com.vierec.common.dto.PageResponse;
import com.vierec.modules.user.dto.AssignRolesRequest;
import com.vierec.modules.user.dto.CreateUserRequest;
import com.vierec.modules.user.dto.UpdateUserRequest;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.service.UserService;
import com.vierec.security.access.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Positive;

@RestController
@RequestMapping(AppConstants.API_V1 + "/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "User administration endpoints")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class UserController {

    private final UserService userService;

    @PostMapping
    @AdminOnly
    @Operation(summary = "Create a user with roles",
            description = "Same fields and rules as registration, plus roles (required) and status (default ACTIVE). "
                    + "Only a SUPER_ADMIN may create a SUPER_ADMIN.")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.create(request, authentication.getName()), "User created"));
    }

    @GetMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Get a user by id")
    public ApiResponse<UserResponse> getById(@PathVariable @Positive Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping
    @AdminOnly
    @Operation(summary = "Search users with paging and sorting")
    public ApiResponse<PageResponse<UserResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.success(userService.search(keyword, status, pageable));
    }

    @PutMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Update a user")
    public ApiResponse<UserResponse> update(@PathVariable @Positive Long id,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.update(id, request), "User updated");
    }

    @PutMapping("/{id}/roles")
    @AdminOnly
    @Operation(summary = "Replace the roles of a user",
            description = "Only a SUPER_ADMIN may grant or remove SUPER_ADMIN or change a SUPER_ADMIN's roles. "
                    + "Nobody may change their own roles. Takes effect at the user's next login or token refresh "
                    + "(at most 30 minutes).")
    public ApiResponse<UserResponse> assignRoles(@PathVariable @Positive Long id,
                                                 @Valid @RequestBody AssignRolesRequest request,
                                                 Authentication authentication) {
        return ApiResponse.success(userService.assignRoles(id, request, authentication.getName()), "Roles updated");
    }

    @DeleteMapping("/{id}")
    @AdminOnly
    @Operation(summary = "Soft-delete a user")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
