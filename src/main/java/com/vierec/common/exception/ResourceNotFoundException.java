package com.vierec.common.exception;

/**
 * Thrown when an entity addressed by the caller does not exist.
 */
public class ResourceNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                String.format("%s not found with %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
