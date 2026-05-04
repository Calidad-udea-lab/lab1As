package com.udea.lab1As.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InvalidTransactionExceptionTest {

    @Test
    void constructor_shouldPreserveMessage() {
        var exception = new InvalidTransactionException("Transaction amount must be greater than zero");

        assertEquals("Transaction amount must be greater than zero", exception.getMessage());
    }
}
