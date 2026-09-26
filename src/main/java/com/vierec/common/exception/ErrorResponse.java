package com.vierec.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * RFC-7807-inspired error body. One shape for every failure the API can return.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private boolean success = false;

    private String code;
    private String message;
    private int status;
    private String path;
    private String method;
    private String traceId;

    /** Field-level violations, present only for validation failures. */
    private List<FieldViolation> errors;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldViolation implements Serializable {

        private static final long serialVersionUID = 1L;

        private String field;
        private Object rejectedValue;
        private String message;
    }
}
