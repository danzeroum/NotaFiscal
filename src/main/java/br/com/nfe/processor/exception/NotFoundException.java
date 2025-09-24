package br.com.nfe.processor.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ProblemException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "https://nfe.processor/problems/not-found", message);
    }
}
