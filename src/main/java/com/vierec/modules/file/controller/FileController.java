package com.vierec.modules.file.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.modules.file.entity.StoredFile;
import com.vierec.modules.file.service.FileDownload;
import com.vierec.modules.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping(AppConstants.API_V1 + "/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "Files", description = "Download uploaded files")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Streams the file inline (a PDF opens in the browser, a video plays in {@code <video>}).
     * Spring answers {@code Range} requests on a {@link Resource} body, so videos can be seeked.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Download or stream an uploaded file")
    public ResponseEntity<Resource> download(@PathVariable @Positive Long id) {
        FileDownload download = fileStorageService.load(id);
        StoredFile file = download.getFile();
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.getResource());
    }
}
