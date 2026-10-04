package com.vierec.modules.file.service.impl;

import com.vierec.common.exception.BusinessException;
import com.vierec.common.exception.ErrorCode;
import com.vierec.common.exception.ResourceNotFoundException;
import com.vierec.config.properties.UploadProperties;
import com.vierec.modules.file.entity.StoredFile;
import com.vierec.modules.file.repository.StoredFileRepository;
import com.vierec.modules.file.service.FileCategory;
import com.vierec.modules.file.service.FileDownload;
import com.vierec.modules.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class FileStorageServiceImpl implements FileStorageService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final String PDF = "pdf";
    private static final String PDF_TYPE = "application/pdf";

    private final StoredFileRepository storedFileRepository;
    private final UploadProperties properties;
    private final Path root;

    public FileStorageServiceImpl(StoredFileRepository storedFileRepository, UploadProperties properties) {
        this.storedFileRepository = storedFileRepository;
        this.properties = properties;
        this.root = Paths.get(properties.getDir()).toAbsolutePath().normalize();
    }

    @Override
    public void validate(MultipartFile file, FileCategory category) {
        contentTypeOf(file, category);
    }

    @Override
    @Transactional
    public StoredFile store(MultipartFile file, FileCategory category, String folder) {
        String contentType = contentTypeOf(file, category);
        String name = originalName(file);
        LocalDate today = LocalDate.now();
        // Stored name is random: the uploader's file name never reaches the file system.
        String relative = String.format("%s/%d/%02d/%s.%s", folder, today.getYear(), today.getMonthValue(),
                UUID.randomUUID().toString().replace("-", ""), extensionOf(name));
        Path target = resolve(relative);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException ex) {
            log.error("Could not write upload to {}", target, ex);
            deleteQuietly(target);
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
        deleteOnRollback(target);

        StoredFile stored = new StoredFile();
        stored.setUrl(relative);
        stored.setOriginalName(name);
        stored.setContentType(contentType);
        stored.setSizeBytes(file.getSize());
        StoredFile saved = storedFileRepository.save(stored);
        log.info("Stored file id={} path={} size={}", saved.getId(), relative, saved.getSizeBytes());
        return saved;
    }

    @Override
    public FileDownload load(Long id) {
        StoredFile file = storedFileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND));
        Path path = resolve(file.getUrl());
        if (!Files.isRegularFile(path)) {
            log.error("File id={} is in the DB but missing on disk: {}", id, path);
            throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
        }
        return new FileDownload(file, new PathResource(path));
    }

    // ------------------------------------------------------------------ helpers

    /** Checks extension and size; returns the MIME type configured for the extension. */
    private String contentTypeOf(MultipartFile file, FileCategory category) {
        Map<String, String> allowed = category == FileCategory.CERTIFICATE
                ? Collections.singletonMap(PDF, PDF_TYPE) : properties.getDocumentTypes();
        DataSize maxSize = properties.getMaxDocumentSize();
        String kind = category.name().toLowerCase(Locale.ROOT);

        String name = originalName(file);
        String contentType = allowed.get(extensionOf(name));
        if (contentType == null) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, String.format(
                    "File '%s' is not an allowed %s type. Allowed: %s",
                    name, kind, String.join(", ", allowed.keySet())));
        }
        if (file.getSize() > maxSize.toBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, String.format(
                    "File '%s' is larger than the %s limit of %d MB", name, kind, maxSize.toMegabytes()));
        }
        return contentType;
    }

    /** File name only (some browsers send the full client path), capped to the column length. */
    private static String originalName(MultipartFile file) {
        String raw = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String name = StringUtils.getFilename(StringUtils.cleanPath(raw));
        if (!StringUtils.hasText(name)) {
            return "file";
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(name.length() - MAX_NAME_LENGTH) : name;
    }

    private static String extensionOf(String name) {
        String extension = StringUtils.getFilenameExtension(name);
        return extension == null ? "" : extension.toLowerCase(Locale.ROOT);
    }

    /** Resolves a stored relative path and refuses anything that would escape the upload root. */
    private Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new ResourceNotFoundException(ErrorCode.FILE_NOT_FOUND);
        }
        return path;
    }

    private static void deleteOnRollback(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    deleteQuietly(target);
                }
            }
        });
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Could not delete {}", path, ex);
        }
    }
}
