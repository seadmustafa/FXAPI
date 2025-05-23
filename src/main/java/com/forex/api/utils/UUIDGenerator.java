package com.forex.api.utils;


import java.util.UUID;

public final class UUIDGenerator {

    private UUIDGenerator() {
    }

    public static String generateTransactionId() {
        return UUID.randomUUID().toString();
    }
}
