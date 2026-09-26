package com.vierec.modules.role.service.impl;

import com.vierec.modules.role.dto.RoleResponse;
import com.vierec.modules.role.mapper.RoleMapper;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public List<RoleResponse> findAll() {
        return roleMapper.toResponses(roleRepository.findAll(Sort.by("id")));
    }
}
