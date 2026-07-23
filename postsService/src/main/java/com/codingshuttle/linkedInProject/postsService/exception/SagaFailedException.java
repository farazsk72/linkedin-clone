package com.codingshuttle.linkedInProject.postsService.exception;

/**
 * Thrown when a saga could not complete and its committed steps were rolled
 * back by compensation. Distinct from BadRequest/NotFound so it maps to a 5xx -
 * the request was valid, the system just could not fulfil it.
 */
public class SagaFailedException extends RuntimeException {
    public SagaFailedException(String message) {
        super(message);
    }
}
