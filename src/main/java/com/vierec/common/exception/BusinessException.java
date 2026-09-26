package com.vierec.common.exception;

/**
 * Thrown when a request is well-formed but violates a business rule.
 */
public class BusinessException extends AppException {

    private static final long serialVersionUID = 1L;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
