package com.vierec.common.exception;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Base class for every exception the application raises deliberately.
 *
 * <p>Carrying an {@link ErrorCode} lets {@code GlobalExceptionHandler} derive the HTTP status and
 * response body without a chain of {@code instanceof} checks.</p>
 */
@Getter
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /** Item-level problems returned as {@code errors} (e.g. the invalid rows of an imported file); may be null. */
    private final List<ErrorResponse.FieldViolation> violations;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.violations = null;
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.violations = null;
    }

    public AppException(ErrorCode errorCode, List<ErrorResponse.FieldViolation> violations) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.violations = Collections.unmodifiableList(violations);
    }
}
