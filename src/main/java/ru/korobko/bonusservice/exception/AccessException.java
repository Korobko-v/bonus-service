package ru.korobko.bonusservice.exception;

public class AccessException extends RuntimeException{
    public AccessException(String message) {
        super(message);
    }
}
