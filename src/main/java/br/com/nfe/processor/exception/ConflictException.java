package br.com.nfe.processor.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ProblemException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "https://nfe.processor/problems/conflict", message);
    }
}
