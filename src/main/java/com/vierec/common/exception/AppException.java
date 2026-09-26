package com.vierec.common.exception;

import lombok.Getter;

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

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
