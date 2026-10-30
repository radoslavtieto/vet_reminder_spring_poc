package com.vetsync.pms.application;

public class PmsInvalidRequestException extends RuntimeException {

    public PmsInvalidRequestException(String message) {
        super(message);
    }
}
