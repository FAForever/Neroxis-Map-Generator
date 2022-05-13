package com.faforever.neroxis.generator;

public class GraphComputationException extends RuntimeException {
    public GraphComputationException(String message) {
        this(message, null);
    }

    public GraphComputationException(String message, Throwable cause) {
        super(message, cause);
    }
}
