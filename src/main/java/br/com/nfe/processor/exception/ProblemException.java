package br.com.nfe.processor.exception;

import org.springframework.http.HttpStatus;

public class ProblemException extends RuntimeException {

    private final HttpStatus status;
    private final String type;

    public ProblemException(HttpStatus status, String type, String message) {
        super(message);
        this.status = status;
        this.type = type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }
}
