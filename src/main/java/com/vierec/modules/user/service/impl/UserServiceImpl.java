package com.vierec.modules.user.service.impl;

import com.vierec.common.dto.PageResponse;
import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.modules.auth.dto.RegisterRequest;
import com.vierec.modules.role.entity.Role;
import com.vierec.modules.role.entity.RoleCode;
import com.vierec.modules.role.repository.RoleRepository;
import com.vierec.modules.user.dto.AssignRolesRequest;
import com.vierec.modules.user.dto.CreateUserRequest;
import com.vierec.modules.user.dto.UpdateUserRequest;
import com.vierec.modules.user.dto.UserResponse;
import com.vierec.modules.user.entity.User;
import com.vierec.modules.user.entity.UserStatus;
import com.vierec.modules.user.mapper.UserMapper;
import com.vierec.modules.user.repository.UserRepository;
import com.vierec.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        Role defaultRole = roleRepository.findByCode(RoleCode.DEFAULT_FOR_REGISTRATION)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND,
                        "Role " + RoleCode.DEFAULT_FOR_REGISTRATION + " is missing from table roles"));

        User saved = createUser(request, Collections.singletonList(defaultRole), UserStatus.ACTIVE, null);
        log.info("Registered user id={} username={}", saved.getId(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request, String actorUsername) {
        User actor = findActor(actorUsername);
        List<Role> roles = findRoles(request.getRoles());
        if (!isSuperAdmin(actor) && hasSuperAdmin(roles)) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_ROLE_FORBIDDEN);
        }
        UserStatus status = request.getStatus() == null ? UserStatus.ACTIVE : request.getStatus();

        User saved = createUser(request, roles, status, actor.getId());
        log.info("User id={} created user id={} username={} roles={}",
                actor.getId(), saved.getId(), saved.getUsername(), request.getRoles());
        return userMapper.toResponse(saved);
    }

    /** Shared by self-registration and admin creation: uniqueness checks, then the insert. */
    private User createUser(RegisterRequest request, List<Role> roles, UserStatus status, Long assignedBy) {
        String email = normalizeEmail(request.getEmail());

        // Checked against soft-deleted rows too: they still hold the unique keys in the table.
        if (userRepository.countByUsernameIncludingDeleted(request.getUsername().trim()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.countByEmailIncludingDeleted(email) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.countByPhoneNumberIncludingDeleted(request.getPhoneNumber()) > 0) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (userRepository.countByCccdIncludingDeleted(request.getCccd()) > 0) {
            throw new BusinessException(ErrorCode.CCCD_ALREADY_EXISTS);
        }

        User user = userMapper.toEntity(request);
        user.setUsername(request.getUsername().trim());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setAddress(request.getAddress().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(status);
        for (Role role : roles) {
            user.addRole(role, assignedBy);
        }
        return userRepository.save(user);
    }

    @Override
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findEntityById(id));
    }

    @Override
    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    public PageResponse<UserResponse> search(String keyword, UserStatus status, Pageable pageable) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<User> page = userRepository.search(normalized, status, pageable);
        return PageResponse.of(page, userMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findEntityById(id);

        if (request.getEmail() != null) {
            request.setEmail(normalizeEmail(request.getEmail()));
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.countByEmailIncludingDeleted(request.getEmail()) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }
        if (request.getPhoneNumber() != null
                && !request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.countByPhoneNumberIncludingDeleted(request.getPhoneNumber()) > 0) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (request.getCccd() != null
                && !Objects.equals(request.getCccd(), user.getCccd())
                && userRepository.countByCccdIncludingDeleted(request.getCccd()) > 0) {
            throw new BusinessException(ErrorCode.CCCD_ALREADY_EXISTS);
        }

        userMapper.updateEntity(request, user);

        User saved = userRepository.save(user);
        log.info("Updated user id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, AssignRolesRequest request, String actorUsername) {
        User actor = findActor(actorUsername);
        User user = findEntityById(id);
        if (actor.getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.OWN_ROLES_CHANGE_FORBIDDEN);
        }
        List<Role> roles = findRoles(request.getRoles());
        if (!isSuperAdmin(actor) && (hasSuperAdmin(roles) || isSuperAdmin(user))) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_ROLE_FORBIDDEN);
        }

        // Roles the user keeps are left alone, so their assigned_at / assigned_by stay as they were.
        user.getUserRoles().removeIf(userRole -> !roles.contains(userRole.getRole()));
        Set<Role> current = user.getRoles();
        for (Role role : roles) {
            if (!current.contains(role)) {
                user.addRole(role, actor.getId());
            }
        }

        User saved = userRepository.saveAndFlush(user);
        log.info("User id={} set roles of user id={} to {}", actor.getId(), id, request.getRoles());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findEntityById(id);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("Soft-deleted user id={}", id);
    }

    private User findActor(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /** Every code must exist; codes are matched exactly (they are upper case in {@code roles}). */
    private List<Role> findRoles(Set<String> codes) {
        Set<String> trimmed = codes.stream().map(String::trim).collect(Collectors.toSet());
        List<Role> roles = roleRepository.findByCodeIn(trimmed);
        if (roles.size() != trimmed.size()) {
            Set<String> found = roles.stream().map(Role::getCode).collect(Collectors.toSet());
            trimmed.removeAll(found);
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + String.join(", ", trimmed));
        }
        return roles;
    }

    private static boolean hasSuperAdmin(List<Role> roles) {
        return roles.stream().anyMatch(role -> RoleCode.SUPER_ADMIN.equals(role.getCode()));
    }

    /** Read from the DB, not from the JWT, so a role removed in the last 30 minutes no longer counts. */
    private static boolean isSuperAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> RoleCode.SUPER_ADMIN.equals(role.getCode()));
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /** The email column is case-insensitive (utf8mb4_unicode_ci); store one canonical form. */
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
