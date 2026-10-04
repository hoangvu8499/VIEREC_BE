package com.vierec.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bound from {@code app.upload.*}: where uploaded files are stored on the server and what is accepted.
 *
 * <p>The per-type limits must stay below {@code spring.servlet.multipart.max-file-size}, which is enforced
 * first by the servlet container.</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** Root folder for uploads; relative paths resolve against the working directory of the app. */
    @NotBlank
    private String dir = "./uploads";

    @NotNull
    private DataSize maxDocumentSize = DataSize.ofMegabytes(50);

    /** Allowed document extensions (lower case) and the MIME type stored for each. */
    @NotEmpty
    private Map<String, String> documentTypes = new LinkedHashMap<>();
}
