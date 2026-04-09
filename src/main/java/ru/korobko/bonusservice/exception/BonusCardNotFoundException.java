package ru.korobko.bonusservice.exception;

public class BonusCardNotFoundException extends RuntimeException {
    public BonusCardNotFoundException(String message) {
        super(message);
    }
}