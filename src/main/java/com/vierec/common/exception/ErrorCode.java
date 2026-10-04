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
    ACCOUNT_LOCKED("VRC-403-003", "The account is locked; an admin must unlock it", HttpStatus.FORBIDDEN),
    LOGIN_TEMPORARILY_BLOCKED("VRC-429-001", "Too many wrong passwords; try again later",
            HttpStatus.TOO_MANY_REQUESTS),
    RESET_CODE_INVALID("VRC-400-003", "The code is wrong or has expired", HttpStatus.BAD_REQUEST),
    RESET_CODE_TOO_SOON("VRC-429-002", "A code was sent less than a minute ago", HttpStatus.TOO_MANY_REQUESTS),

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
    IDENTITY_LOCKED("VRC-409-105", "Identity fields are locked after a certificate is issued", HttpStatus.CONFLICT),

    // ---------- Course / lesson ----------
    COURSE_NOT_FOUND("VRC-404-201", "Course not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_NOT_FOUND("VRC-404-202", "Instructor not found", HttpStatus.NOT_FOUND),
    LESSON_NOT_FOUND("VRC-404-203", "Lesson not found", HttpStatus.NOT_FOUND),
    INSTRUCTOR_NOT_ACTIVE("VRC-400-201", "Instructor account is not active", HttpStatus.BAD_REQUEST),
    LESSON_SORT_ORDER_EXISTS("VRC-409-201", "Another lesson of this course already uses this sort order",
            HttpStatus.CONFLICT),
    ENROLLMENT_NOT_FOUND("VRC-404-204", "Enrollment not found", HttpStatus.NOT_FOUND),
    ALREADY_ENROLLED("VRC-409-202", "Already enrolled in this course", HttpStatus.CONFLICT),
    ENROLLMENT_COMPLETED("VRC-409-203", "A completed enrollment cannot be cancelled", HttpStatus.CONFLICT),
    PROGRESS_NOT_ALLOWED("VRC-403-201", "Only approved learners of this course can save their progress",
            HttpStatus.FORBIDDEN),

    // ---------- File ----------
    FILE_NOT_FOUND("VRC-404-301", "File not found", HttpStatus.NOT_FOUND),
    FILE_TYPE_NOT_ALLOWED("VRC-400-301", "File type is not allowed", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("VRC-413-301", "File is too large", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_STORAGE_FAILED("VRC-500-301", "Could not store the file", HttpStatus.INTERNAL_SERVER_ERROR),

    // ---------- Certificate ----------
    CERTIFICATE_NOT_FOUND("VRC-404-401", "Certificate not found", HttpStatus.NOT_FOUND),
    ENROLLMENT_NOT_COMPLETED("VRC-400-401", "The enrollment is not COMPLETED yet", HttpStatus.BAD_REQUEST),
    CERTIFICATE_ALREADY_ISSUED("VRC-409-401", "A certificate was already issued for this enrollment",
            HttpStatus.CONFLICT),
    CERTIFIED_ENROLLMENT_LOCKED("VRC-409-402", "A certificate was issued for this enrollment; its status is locked",
            HttpStatus.CONFLICT),

    // ---------- Payment ----------
    PAYMENT_NOT_FOUND("VRC-404-501", "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_REFUNDABLE("VRC-409-501", "Only a PAID payment can be refunded", HttpStatus.CONFLICT),

    // ---------- Support point ----------
    SUPPORT_POINT_LOCATION_REQUIRED("VRC-400-601", "Give an address, or both latitude and longitude",
            HttpStatus.BAD_REQUEST),
    ADDRESS_NOT_FOUND("VRC-404-601", "No place matches this address", HttpStatus.NOT_FOUND),
    GEOCODING_UNAVAILABLE("VRC-503-601", "The address lookup service is unavailable, try again later",
            HttpStatus.SERVICE_UNAVAILABLE),

    // ---------- Exam ----------
    EXAM_NOT_FOUND("VRC-404-701", "This course has no exam yet", HttpStatus.NOT_FOUND),
    EXAM_QUESTION_NOT_FOUND("VRC-404-702", "Exam question not found", HttpStatus.NOT_FOUND),
    EXAM_IMPORT_INVALID_ROWS("VRC-400-701", "Some rows of the file are invalid; nothing was imported",
            HttpStatus.BAD_REQUEST),
    EXAM_IMPORT_UNREADABLE("VRC-400-702", "The file is not a readable Excel workbook", HttpStatus.BAD_REQUEST),
    EXAM_IMPORT_EMPTY("VRC-400-703", "The file has no questions", HttpStatus.BAD_REQUEST),
    EXAM_IMPORT_TOO_MANY_ROWS("VRC-400-704", "The file has too many questions", HttpStatus.BAD_REQUEST),
    EXAM_NOT_ALLOWED("VRC-403-701", "Only approved learners of this course can take its exam", HttpStatus.FORBIDDEN),
    EXAM_ATTEMPT_NOT_FOUND("VRC-404-703", "You have not started this exam", HttpStatus.NOT_FOUND),
    EXAM_HAS_NO_QUESTIONS("VRC-409-701", "This exam has no questions yet", HttpStatus.CONFLICT),
    EXAM_ALREADY_TAKEN("VRC-409-702", "This exam can be taken only once and was already submitted",
            HttpStatus.CONFLICT),
    EXAM_PROGRESS_NOT_ENOUGH("VRC-409-703", "Watch at least 80% of the course videos before taking the exam",
            HttpStatus.CONFLICT),

    // ---------- Business ----------
    BUSINESS_NOT_FOUND("VRC-404-801", "Business not found", HttpStatus.NOT_FOUND),
    BUSINESS_MEMBER_NOT_FOUND("VRC-404-802", "This learner does not belong to the business", HttpStatus.NOT_FOUND),
    TAX_CODE_ALREADY_EXISTS("VRC-409-801", "Another business already uses this tax code", HttpStatus.CONFLICT),
    NOT_A_BUSINESS_MANAGER("VRC-403-801", "This account does not manage a business", HttpStatus.FORBIDDEN),
    BUSINESS_INACTIVE("VRC-403-802", "The business is inactive", HttpStatus.FORBIDDEN),
    BUSINESS_ENROLL_NOT_MEMBERS("VRC-400-801", "Some learners do not belong to the business",
            HttpStatus.BAD_REQUEST);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
