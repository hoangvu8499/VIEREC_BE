package com.vierec.modules.user.mapper;

import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.user.dto.UpdateProfileRequest;
import com.vierec.modules.user.dto.UpdateUserRequest;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.modules.user.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entity/DTO conversions. Implementation is generated at compile time by MapStruct.
 *
 * <p>Roles, password and status are never copied from a request: the service sets them explicitly.</p>
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(toRoleCodes(user.getRoles()))")
    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "businessName", source = "business.name")
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    User toEntity(RegisterRequest request);

    /** Copies only the non-null fields of the request onto the managed entity. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    /** Like {@link #updateEntity} for the user's own profile: the status is never touched. */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateProfile(UpdateProfileRequest request, @MappingTarget User user);

    default Set<String> toRoleCodes(Set<Role> roles) {
        return roles.stream().map(Role::getCode).collect(Collectors.toSet());
    }
}
