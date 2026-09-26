package com.vierec.modules.file.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Row of the shared {@code files} table. Not tied to one owner: other tables link to it
 * (e.g. {@code lesson_files}).
 *
 * <p>Named {@code StoredFile} to avoid clashing with {@link java.io.File}. Does not extend {@code BaseEntity}:
 * the table has {@code created_at} but no {@code updated_at}.</p>
 */
@Entity
@Table(name = "files")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class StoredFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Where the file lives. For uploads this is the path relative to {@code app.upload.dir}
     * (e.g. {@code lessons/2026/09/<uuid>.mp4}); clients download it through {@code GET /api/v1/files/{id}}.
     */
    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    /** File name as sent by the uploader; only used for display and the download file name. */
    @Column(name = "original_name", nullable = false)
    private String originalName;

    /** MIME type chosen by the server from the file extension, not the one the client claimed. */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
