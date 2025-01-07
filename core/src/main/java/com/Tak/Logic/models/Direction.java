// File: core/src/main/java/com/Tak/Logic/models/Direction.java
package com.Tak.Logic.models;

/**
 * Enum representing the four orthogonal directions in Tak.
 */
public enum Direction {
    UP(0, 1),
    DOWN(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int deltaX;
    private final int deltaY;

    Direction(int dx, int dy) {
        this.deltaX = dx;
        this.deltaY = dy;
    }

    public int getDeltaX() {
        return deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }
}
