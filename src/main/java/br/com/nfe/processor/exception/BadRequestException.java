package br.com.nfe.processor.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ProblemException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "https://nfe.processor/problems/bad-request", message);
    }
}
