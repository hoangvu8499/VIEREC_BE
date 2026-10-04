package com.vierec.common.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.vierec.common.constant.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Translates every exception that escapes a controller into the single {@link ErrorResponse} shape.
 *
 * <p>Nothing else in the codebase should build error payloads by hand.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ------------------------------------------------------------------ application errors

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("Application error [{}] on {} {}", errorCode.getCode(),
                    request.getMethod(), request.getRequestURI(), ex);
        } else {
            log.warn("Application error [{}] on {} {}: {}", errorCode.getCode(),
                    request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        return build(errorCode, ex.getMessage(), request, ex.getViolations());
    }

    // ------------------------------------------------------------------ validation

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                 HttpHeaders headers,
                                                                 HttpStatus status,
                                                                 WebRequest request) {
        return validationFailed(ex.getBindingResult(), request);
    }

    /** {@code @Valid @ModelAttribute} (multipart forms) fails with a plain {@link BindException}. */
    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex,
                                                         HttpHeaders headers,
                                                         HttpStatus status,
                                                         WebRequest request) {
        return validationFailed(ex.getBindingResult(), request);
    }

    /** Unreadable JSON; a wrong enum or number value becomes a field violation instead of a Jackson message. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Unreadable request body: {}", ex.getMostSpecificCause().getMessage());
        if (!(ex.getCause() instanceof InvalidFormatException)) {
            return new ResponseEntity<>(body(ErrorCode.BAD_REQUEST, "Request body is missing or is not valid JSON",
                    currentRequest(request), null), ErrorCode.BAD_REQUEST.getHttpStatus());
        }
        InvalidFormatException cause = (InvalidFormatException) ex.getCause();
        String allowed = cause.getTargetType() != null && cause.getTargetType().isEnum()
                ? ". Allowed: " + Arrays.toString(cause.getTargetType().getEnumConstants())
                : "";
        List<ErrorResponse.FieldViolation> violations = Collections.singletonList(
                ErrorResponse.FieldViolation.builder()
                        .field(fieldPath(cause))
                        .rejectedValue(cause.getValue())
                        .message("Invalid value" + allowed)
                        .build());
        return new ResponseEntity<>(body(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                currentRequest(request), violations), ErrorCode.VALIDATION_FAILED.getHttpStatus());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                  HttpServletRequest request) {
        List<ErrorResponse.FieldViolation> violations = ex.getConstraintViolations().stream()
                .map(this::toViolation)
                .collect(Collectors.toList());
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                request, violations);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("Bad request on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.BAD_REQUEST, ex.getMessage(), request, null);
    }

    /** Upload bigger than {@code spring.servlet.multipart.max-file-size} / {@code max-request-size}. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                                             HttpServletRequest request) {
        log.warn("Upload too large on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return build(ErrorCode.FILE_TOO_LARGE, ErrorCode.FILE_TOO_LARGE.getDefaultMessage(), request, null);
    }

    // ------------------------------------------------------------------ security

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getDefaultMessage(),
                request, null);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCOUNT_DISABLED, ErrorCode.ACCOUNT_DISABLED.getDefaultMessage(),
                request, null);
    }

    /** Locked by wrong passwords (or by an admin): only an admin can set it back to ACTIVE. */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex, HttpServletRequest request) {
        return build(ErrorCode.ACCOUNT_LOCKED, ErrorCode.ACCOUNT_LOCKED.getDefaultMessage(), request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                             HttpServletRequest request) {
        return build(ErrorCode.UNAUTHENTICATED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        log.warn("Access denied on {} {}", request.getMethod(), request.getRequestURI());
        return build(ErrorCode.ACCESS_DENIED, ErrorCode.ACCESS_DENIED.getDefaultMessage(), request, null);
    }

    // ------------------------------------------------------------------ persistence / routing

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Data integrity violation on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.CONFLICT, "The request conflicts with existing data", request, null);
    }

    /**
     * Every Spring MVC exception handled by the base class (404 no handler, 405, 415, unreadable body,
     * missing parameter, ...) ends up here; mapping them in this class too would make Spring fail on startup
     * with an "Ambiguous @ExceptionHandler" error.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             Object body,
                                                             HttpHeaders headers,
                                                             HttpStatus status,
                                                             WebRequest request) {
        HttpServletRequest servletRequest = currentRequest(request);
        ErrorCode errorCode = toErrorCode(status);
        String message = ex instanceof NoHandlerFoundException && servletRequest != null
                ? "No endpoint " + servletRequest.getMethod() + " " + servletRequest.getRequestURI()
                : ex.getMessage();
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("Spring MVC error [{}]", status, ex);
        } else {
            log.warn("Spring MVC error [{}]: {}", status, ex.getMessage());
        }
        return new ResponseEntity<>(body(errorCode, message, servletRequest, null), headers,
                errorCode.getHttpStatus());
    }

    // ------------------------------------------------------------------ catch-all

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        // Never leak internals to the caller; the traceId links the response to the log entry.
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), request, null);
    }

    // ------------------------------------------------------------------ helpers

    private ResponseEntity<Object> validationFailed(BindingResult bindingResult, WebRequest request) {
        List<ErrorResponse.FieldViolation> violations = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            violations.add(ErrorResponse.FieldViolation.builder()
                    .field(fieldError.getField())
                    .rejectedValue(maskIfSensitive(fieldError.getField(), fieldError.getRejectedValue()))
                    // A type mismatch ("abc" for a number) carries a long Spring message; keep it short.
                    .message(fieldError.isBindingFailure() ? "Invalid value" : fieldError.getDefaultMessage())
                    .build());
        }
        bindingResult.getGlobalErrors().forEach(globalError ->
                violations.add(ErrorResponse.FieldViolation.builder()
                        .field(globalError.getObjectName())
                        .message(globalError.getDefaultMessage())
                        .build()));

        log.warn("Validation failed: {}", violations.size());
        return new ResponseEntity<>(
                body(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.getDefaultMessage(),
                        currentRequest(request), violations),
                ErrorCode.VALIDATION_FAILED.getHttpStatus());
    }

    private static String fieldPath(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private ErrorCode toErrorCode(HttpStatus status) {
        if (status.is5xxServerError()) {
            return ErrorCode.INTERNAL_ERROR;
        }
        switch (status) {
            case NOT_FOUND:
                return ErrorCode.RESOURCE_NOT_FOUND;
            case METHOD_NOT_ALLOWED:
                return ErrorCode.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE:
                return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default:
                return ErrorCode.BAD_REQUEST;
        }
    }

    private ErrorResponse.FieldViolation toViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString();
        return ErrorResponse.FieldViolation.builder()
                .field(path)
                .rejectedValue(maskIfSensitive(path, violation.getInvalidValue()))
                .message(violation.getMessage())
                .build();
    }

    /**
     * Never echo secrets back: a rejected password would otherwise appear in the response and in client logs.
     * An uploaded file is reported by its name only (serializing it would read the whole upload).
     */
    private static Object maskIfSensitive(String field, Object value) {
        if (value instanceof MultipartFile) {
            return ((MultipartFile) value).getOriginalFilename();
        }
        if (value != null && field != null && field.toLowerCase(Locale.ROOT).contains("password")) {
            return "******";
        }
        return value;
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode errorCode,
                                                String message,
                                                HttpServletRequest request,
                                                List<ErrorResponse.FieldViolation> violations) {
        return new ResponseEntity<>(body(errorCode, message, request, violations), errorCode.getHttpStatus());
    }

    private ErrorResponse body(ErrorCode errorCode,
                              String message,
                              HttpServletRequest request,
                              List<ErrorResponse.FieldViolation> violations) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getDefaultMessage())
                .status(errorCode.getHttpStatus().value())
                .path(request != null ? request.getRequestURI() : null)
                .method(request != null ? request.getMethod() : null)
                .traceId(MDC.get(AppConstants.TRACE_ID))
                .errors(violations)
                .build();
    }

    private HttpServletRequest currentRequest(WebRequest request) {
        if (request instanceof org.springframework.web.context.request.ServletWebRequest) {
            return ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        }
        return null;
    }
}
