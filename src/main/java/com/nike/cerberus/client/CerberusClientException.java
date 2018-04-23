package com.nike.cerberus.client;

public class CerberusClientException extends RuntimeException {
    /**
     * Constructs the exception with a message and underlying exception.
     *
     * @param message Message
     * @param t       Underlying exception
     */
    public CerberusClientException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Constructs the exception with a message.
     *
     * @param message Message
     */
    public CerberusClientException(String message) {
        super(message);
    }
}
