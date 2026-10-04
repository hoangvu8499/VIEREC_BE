package com.vierec.modules.certificate.entity;

import com.vierec.modules.course.entity.CourseEnrollment;
import com.vierec.modules.file.entity.StoredFile;
import com.vierec.modules.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Row of {@code certificates}: proof that one enrollment was completed.
 *
 * <p>Course name and the trainee's name, date of birth and CCCD are copied at issue time, so a later profile or
 * course change does not alter an issued certificate.</p>
 */
@Entity
@Table(name = "certificates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_certificates_code", columnNames = "code"),
                @UniqueConstraint(name = "uk_certificates_enrollment", columnNames = "enrollment_id")},
        indexes = @Index(name = "idx_certificates_file", columnList = "file_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Certificate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Public lookup code, random so that codes cannot be enumerated. */
    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false, updatable = false)
    private CourseEnrollment enrollment;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "full_name", nullable = false, length = 101)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "cccd", columnDefinition = "char(12)")
    private String cccd;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private StoredFile file;

    /** Admin who issued it; set by the service. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by", updatable = false)
    private User issuedBy;

    @CreatedDate
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;
}
