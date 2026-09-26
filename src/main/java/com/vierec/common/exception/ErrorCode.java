package com.vierec.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Single source of truth for business error codes exposed over the API.
 *
 * <p>Codes are stable strings so that front-ends can branch on them without parsing messages.</p>
 */
@Getter
public enum ErrorCode {

    // ---------- Generic ----------
    INTERNAL_ERROR("VRC-500-001", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_FAILED("VRC-400-001", "Validation failed", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("VRC-400-002", "Bad request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("VRC-404-001", "Resource not found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("VRC-405-001", "HTTP method not supported", HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE("VRC-415-001", "Unsupported media type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    CONFLICT("VRC-409-001", "Resource conflict", HttpStatus.CONFLICT),

    // ---------- Auth ----------
    UNAUTHENTICATED("VRC-401-001", "Authentication required", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("VRC-401-002", "Invalid username or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("VRC-401-003", "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("VRC-401-004", "Token is invalid", HttpStatus.UNAUTHORIZED),
    ACCOUNT_DISABLED("VRC-403-001", "Account is disabled", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("VRC-403-002", "You do not have permission to perform this action", HttpStatus.FORBIDDEN),

    // ---------- User ----------
    USER_NOT_FOUND("VRC-404-101", "User not found", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS("VRC-409-101", "Username is already taken", HttpStatus.CONFLICT),
    EMAIL_ALREADY_EXISTS("VRC-409-102", "Email is already registered", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS("VRC-409-103", "Phone number is already registered", HttpStatus.CONFLICT),
    CCCD_ALREADY_EXISTS("VRC-409-104", "CCCD is already registered", HttpStatus.CONFLICT),
    ROLE_NOT_FOUND("VRC-404-102", "Role not found", HttpStatus.NOT_FOUND),
    SUPER_ADMIN_ROLE_FORBIDDEN("VRC-403-101", "Only a SUPER_ADMIN can grant, remove or change the SUPER_ADMIN role",
            HttpStatus.FORBIDDEN),
    OWN_ROLES_CHANGE_FORBIDDEN("VRC-403-102", "You cannot change your own roles", HttpStatus.FORBIDDEN),

    // ---------- Course / lesson ----------
    COURSE_NOT_FOUND("VRC-404-201", "Course not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_NOT_FOUND("VRC-404-202", "Instructor not found", HttpStatus.NOT_FOUND),
    LESSON_NOT_FOUND("VRC-404-203", "Lesson not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_NOT_ACTIVE("VRC-400-201", "Instructor account is not active", HttpStatus.BAD_REQUEST),
    LESSON_SORT_ORDER_EXISTS("VRC-409-201", "Another lesson of this course already uses this sort order",
            HttpStatus.CONFLICT),

    // ---------- File ----------
    FILE_NOT_FOUND("VRC-404-301", "File not found", HttpStatus.NOT_FOUND),
    FILE_TYPE_NOT_ALLOWED("VRC-400-301", "File type is not allowed", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("VRC-413-301", "File is too large", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_STORAGE_FAILED("VRC-500-301", "Could not store the file", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
