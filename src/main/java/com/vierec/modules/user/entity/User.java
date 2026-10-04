package com.vierec.modules.user.entity;

import com.vierec.domain.BaseEntity;
import com.vierec.modules.business.entity.Business;
import com.vierec.modules.role.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Where;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Account row from {@code users}. Soft-deleted rows ({@code deleted_at} set) are invisible to every
 * JPA read through {@link Where}, but still hold their unique username / cccd / phone / email.
 */
@Entity
@Table(name = "users")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"passwordHash", "userRoles"})
public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    /** BCrypt hash - never the plain value. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /** Căn cước công dân. The column is CHAR(12), so the type must be spelled out for schema validation. */
    @Column(name = "cccd", columnDefinition = "char(12)", unique = true)
    private String cccd;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number", length = 15, unique = true)
    private String phoneNumber;

    @Column(name = "email", unique = true)
    private String email;

    /** The column is a MySQL ENUM; the definition keeps {@code ddl-auto: validate} happy. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('ACTIVE','INACTIVE','LOCKED')")
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    /** Wrong passwords in a row; reset by a successful login, an admin unlock or a password reset. */
    @Column(name = "failed_login_count", nullable = false)
    @Builder.Default
    private int failedLoginCount = 0;

    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;

    /** Created with a default password (business import): the user must choose their own after logging in. */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Business the user learns for (TRAINEE) or manages (BUSINESS); null for an independent learner. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    private Business business;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    public void addRole(Role role, Long assignedBy) {
        userRoles.add(new UserRole(this, role, assignedBy));
    }

    public Set<Role> getRoles() {
        return userRoles.stream().map(UserRole::getRole).collect(Collectors.toSet());
    }

    public boolean hasRole(String code) {
        return userRoles.stream().anyMatch(userRole -> code.equals(userRole.getRole().getCode()));
    }

    public boolean isActive() {
        return UserStatus.ACTIVE == this.status;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
