package br.com.nfe.processor.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends ProblemException {
    public UnprocessableEntityException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "https://nfe.processor/problems/unprocessable", message);
    }
}
