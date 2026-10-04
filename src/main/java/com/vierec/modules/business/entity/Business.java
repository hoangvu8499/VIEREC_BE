package com.vierec.modules.business.entity;

import com.vierec.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Corporate customer ({@code businesses}). Its managers (role BUSINESS) and its learners point to it through
 * {@code users.business_id}.
 */
@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
public class Business extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "tax_code", nullable = false, length = 14, unique = true)
    private String taxCode;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('ACTIVE','INACTIVE')")
    private BusinessStatus status = BusinessStatus.ACTIVE;
}
