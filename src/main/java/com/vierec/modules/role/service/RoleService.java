package com.vierec.modules.role.service;

import com.vierec.modules.role.dto.RoleResponse;

import java.util.List;

public interface RoleService {

    /** Every row of {@code roles}, by id. */
    List<RoleResponse> findAll();
}
