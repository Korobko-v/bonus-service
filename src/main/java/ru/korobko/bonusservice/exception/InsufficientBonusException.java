package ru.korobko.bonusservice.exception;

public class InsufficientBonusException extends RuntimeException {
    public InsufficientBonusException(String message) {
        super(message);
    }
}