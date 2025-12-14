package cz.osu.opr3_backend.web.error;

import cz.osu.opr3_backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
                details.put(err.getField(), err.getDefaultMessage())
        );
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String message = "Data integrity violation";

        // Zkusime rozpoznat nas unikátní constraint na SKU
        String mostSpecific = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : null;
        if (mostSpecific != null && mostSpecific.contains("ux_product_sku")) {
            message = "Product with this SKU already exists";
        }

        return build(HttpStatus.CONFLICT, message, req.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return ApiError.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                req.getRequestURI()
        );
    }

    @ExceptionHandler(OutOfStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleOutOfStock(OutOfStockException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        details.put("productId", ex.getProductId());
        details.put("requested", ex.getRequested());
        details.put("available", ex.getAvailable());

        return ApiError.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                req.getRequestURI(),
                details
        );
    }

    @ExceptionHandler(OrderStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleOrderState(OrderStateException ex, HttpServletRequest req) {
        return ApiError.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                req.getRequestURI()
        );
    }

    @ExceptionHandler(BuildValidationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleBuildValidation(BuildValidationException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        details.put("reasons", ex.getReasons());

        return ApiError.of(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                req.getRequestURI(),
                details
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiError handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return ApiError.of(HttpStatus.UNAUTHORIZED, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException ex, HttpServletRequest req) {
        return ApiError.of(HttpStatus.CONFLICT, ex.getMessage(), req.getRequestURI(), null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleOptimistic(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return ApiError.of(HttpStatus.CONFLICT, "Data changed by another request. Please retry.", req.getRequestURI(), null);
    }


    private ResponseEntity<ApiError> build(HttpStatus status, String message, String path, Map<String, Object> details) {
        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                details
        );
        return ResponseEntity.status(status).body(error);
    }
}
