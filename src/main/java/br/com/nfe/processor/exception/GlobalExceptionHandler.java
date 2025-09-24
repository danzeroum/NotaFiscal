package br.com.nfe.processor.exception;

import br.com.nfe.processor.config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URL = "https://nfe.processor/problems";

    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setType(ex.getType() != null ? ex.getType() : defaultType(ex.getStatus()));
        detail.setTitle(ex.getStatus().getReasonPhrase());
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Request validation failed");
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setType(PROBLEM_BASE_URL + "/validation-error");
        detail.setTitle("Erro de validação");
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(MultipartException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Arquivo inválido ou ausente");
        detail.setType(PROBLEM_BASE_URL + "/multipart-error");
        detail.setTitle("Requisição inválida");
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest request) {
        ProblemDetail detail = ex.getBody();
        if (detail.getType() == null) {
            detail.setType(defaultType(HttpStatus.valueOf(detail.getStatus())));
        }
        if (detail.getTitle() == null) {
            detail.setTitle(HttpStatus.valueOf(detail.getStatus()).getReasonPhrase());
        }
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor. Por favor, tente novamente.");
        detail.setType(PROBLEM_BASE_URL + "/internal-error");
        detail.setTitle("Erro interno");
        decorate(detail, request);
        return detail;
    }

    private void decorate(ProblemDetail detail, HttpServletRequest request) {
        detail.setInstance(request.getRequestURI());
        detail.setProperty(TraceIdFilter.TRACE_ID_KEY, ensureTraceId());
        detail.setProperty("timestamp", Instant.now());
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
}
