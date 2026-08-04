package com.example.onlineexam.exception;

public class TimeLimitExceededException extends RuntimeException {
    public TimeLimitExceededException(String message) {
        super(message);
    }
}
