package com.Tak.Logic.utils;

/**
 * Exception thrown when a move is invalid according to game rules.
 */
public class InvalidMoveException extends Exception {
    public InvalidMoveException(String message) {
        super(message);
    }
}
