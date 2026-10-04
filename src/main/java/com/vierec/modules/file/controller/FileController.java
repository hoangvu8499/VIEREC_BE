package com.vierec.modules.file.controller;

import com.vierec.common.constant.AppConstants;
import com.vierec.modules.file.entity.StoredFile;
import com.vierec.modules.file.service.FileAccessGuard;
import com.vierec.modules.file.service.FileDownload;
import com.vierec.modules.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(AppConstants.API_V1 + "/files")
@RequiredArgsConstructor
@Validated
@Tag(name = "Files", description = "Download uploaded files")
@SecurityRequirement(name = AppConstants.AUTH_COOKIE_SCHEME)
public class FileController {

    /** A file id never gets other content (a new upload is a new id), so the browser can keep it; private: guarded. */
    private static final CacheControl CACHE = CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate();

    private final FileStorageService fileStorageService;
    private final List<FileAccessGuard> accessGuards;

    /**
     * Streams the file inline (a PDF opens in the browser, a video plays in {@code <video>}), or as an attachment
     * with {@code download=true}.
     * Spring answers {@code Range} requests on a {@link Resource} body, so videos can be seeked.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Download or stream an uploaded file",
            description = "A certificate PDF: its owner, the managers of their business and admins only.")
    public ResponseEntity<Resource> download(@PathVariable @Positive Long id,
                                             @Parameter(description = "true: save the file instead of opening it")
                                             @RequestParam(name = "download", defaultValue = "false") boolean attachment,
                                             Authentication authentication) {
        for (FileAccessGuard guard : accessGuards) {
            guard.checkRead(id, authentication);
        }
        FileDownload download = fileStorageService.load(id);
        StoredFile file = download.getFile();
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .cacheControl(CACHE)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.getResource());
    }
}
