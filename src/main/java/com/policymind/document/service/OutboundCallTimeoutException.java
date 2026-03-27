package com.policymind.document.service;

public class OutboundCallTimeoutException extends RuntimeException {

    public OutboundCallTimeoutException(String serviceName, Throwable cause) {
        super("Outbound call timed out for service '" + serviceName + "'.", cause);
    }
}
