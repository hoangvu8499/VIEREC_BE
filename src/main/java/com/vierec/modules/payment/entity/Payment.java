package com.vierec.modules.payment.entity;

import com.vierec.domain.BaseEntity;
import com.vierec.modules.course.entity.Course;
import com.vierec.modules.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of {@code payments}: one payment for a course, recorded by an admin. History only: it neither creates nor
 * blocks an enrollment. Rows are never deleted.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_user", columnList = "user_id"),
        @Index(name = "idx_payments_course", columnList = "course_id")})
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The payer. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    private Course course;

    /** Copied at creation so the history keeps the name the course had when it was paid. */
    @Column(name = "course_name", nullable = false, length = 200, updatable = false)
    private String courseName;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false,
            columnDefinition = "enum('CASH','BANK_TRANSFER','CARD','E_WALLET','OTHER')")
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "enum('PENDING','PAID','FAILED','REFUNDED')")
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "note", length = 500)
    private String note;

    /** Set while the status is PAID or REFUNDED, null otherwise. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Admin who recorded the payment; set by the service. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;
}
