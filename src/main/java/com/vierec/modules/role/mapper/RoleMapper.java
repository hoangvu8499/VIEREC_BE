package com.vierec.modules.role.mapper;

import com.vierec.modules.role.dto.RoleResponse;
import com.vierec.modules.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponses(List<Role> roles);
}
