package com.vierec.modules.file.service;

import com.vierec.modules.file.entity.StoredFile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

/** A stored file ready to be streamed: its row plus the bytes on disk. */
@Getter
@AllArgsConstructor
public class FileDownload {

    private final StoredFile file;
    private final Resource resource;
}
