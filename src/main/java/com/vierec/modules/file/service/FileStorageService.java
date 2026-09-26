package com.vierec.modules.file.service;

import com.vierec.modules.file.entity.StoredFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploads on the server disk ({@code app.upload.dir}) and records them in {@code files}.
 */
public interface FileStorageService {

    /** Public path clients use to download a stored file. */
    String DOWNLOAD_PATH = "/api/v1/files/";

    /** Throws {@code FILE_TYPE_NOT_ALLOWED} / {@code FILE_TOO_LARGE} without writing anything. */
    void validate(MultipartFile file, FileCategory category);

    /**
     * Validates, writes the file under {@code folder} and inserts its {@code files} row. Must run inside the
     * caller's transaction: if that transaction rolls back, the written file is deleted again.
     */
    StoredFile store(MultipartFile file, FileCategory category, String folder);

    FileDownload load(Long id);

    static String downloadUrl(Long fileId) {
        return DOWNLOAD_PATH + fileId;
    }
}
