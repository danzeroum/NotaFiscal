package br.com.nfe.processor.exception;

import br.com.nfe.processor.infrastructure.config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URL = "https://nfe.processor/problems";
    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @ExceptionHandler(ProblemException.class)
    public ResponseEntity<ProblemDetail> handleProblemException(
            ProblemException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                ex.getStatus(), ex.getMessage());
        detail.setType(URI.create(ex.getType() != null ? ex.getType() : defaultType(ex.getStatus())));
        detail.setTitle(ex.getStatus().getReasonPhrase());
        decorate(detail, request);
        logStructured(ex, detail);

        return ResponseEntity
                .status(ex.getStatus())
                .contentType(PROBLEM_JSON)
                .body(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Request validation failed");

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, message);
        detail.setType(URI.create(PROBLEM_BASE_URL + "/validation-error"));
        detail.setTitle("Erro de validação");
        decorate(detail, request);
        logStructured(ex, detail);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(PROBLEM_JSON)
                .body(detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
        detail.setType(URI.create(PROBLEM_BASE_URL + "/internal-error"));
        detail.setTitle("Erro interno");
        decorate(detail, request);
        logStructured(ex, detail);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(PROBLEM_JSON)
                .body(detail);
    }

    private void decorate(ProblemDetail detail, HttpServletRequest request) {
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("traceId", ensureTraceId());
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("method", request.getMethod());
    }

    private String ensureTraceId() {
        return Optional.ofNullable(MDC.get(TraceIdFilter.TRACE_ID_KEY))
                .filter(trace -> !trace.isBlank())
                .orElseGet(() -> {
                    String traceId = UUID.randomUUID().toString();
                    MDC.put(TraceIdFilter.TRACE_ID_KEY, traceId);
                    return traceId;
                });
    }

    private String defaultType(HttpStatus status) {
        return PROBLEM_BASE_URL + "/" + status.value();
    }

    private void logStructured(Exception ex, ProblemDetail detail) {
        logger.error("""
            {
                "timestamp": "{}",
                "level": "ERROR",
                "service": "nfe-processor",
                "traceId": "{}",
                "type": "{}",
                "status": {},
                "message": "{}",
                "exception": "{}",
                "stackTrace": "{}"
            }
            """,
                Instant.now(),
                detail.getProperties().get("traceId"),
                detail.getType(),
                detail.getStatus(),
                detail.getDetail(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }
}
