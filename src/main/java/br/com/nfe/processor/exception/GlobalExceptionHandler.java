package br.com.nfe.processor.exception;

import br.com.nfe.processor.config.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
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

    @ExceptionHandler(ProblemException.class)
    public ProblemDetail handleProblemException(ProblemException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        detail.setType(ex.getType());
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
        detail.setType("https://nfe.processor/problems/validation-error");
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(MultipartException ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Arquivo inválido ou ausente");
        detail.setType("https://nfe.processor/problems/multipart-error");
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest request) {
        ProblemDetail detail = ex.getBody();
        decorate(detail, request);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro inesperado");
        detail.setType("https://nfe.processor/problems/internal-error");
        decorate(detail, request);
        return detail;
    }

    private void decorate(ProblemDetail detail, HttpServletRequest request) {
        detail.setInstance(request.getRequestURI());
        detail.setProperty(TraceIdFilter.TRACE_ID_KEY, MDC.get(TraceIdFilter.TRACE_ID_KEY));
    }
}
