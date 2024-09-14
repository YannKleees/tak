package Logic;

import java.util.List;

/**
 * The Move class represents a move made by a player.
 * It includes information about the move's starting and ending positions,
 * the direction, number of pieces being moved, and drop counts.
 */
public class Move {

    private int startX;                 // Starting X coordinate.
    private int startY;                 // Starting Y coordinate.
    private Direction direction;        // Direction of movement.
    private int numberOfPieces;         // Number of pieces being moved.
    private List<Integer> dropCounts;   // Number of pieces to drop at each space.

    /**
     * Constructor to initialize a move.
     *
     * @param startX         The starting X coordinate.
     * @param startY         The starting Y coordinate.
     * @param direction      The direction of movement.
     * @param numberOfPieces The number of pieces being moved.
     * @param dropCounts     The list of drop counts.
     */
    public Move(int startX, int startY, Direction direction, int numberOfPieces, List<Integer> dropCounts) {
        this.startX = startX;
        this.startY = startY;
        this.direction = direction;
        this.numberOfPieces = numberOfPieces;
        this.dropCounts = dropCounts;
    }

    /**
     * Gets the starting X coordinate.
     *
     * @return The starting X coordinate.
     */
    public int getStartX() {
        return this.startX;
    }

    /**
     * Gets the starting Y coordinate.
     *
     * @return The starting Y coordinate.
     */
    public int getStartY() {
        return this.startY;
    }

    /**
     * Gets the direction of the move.
     *
     * @return The direction of the move.
     */
    public Direction getDirection() {
        return this.direction;
    }

    /**
     * Gets the number of pieces being moved.
     *
     * @return The number of pieces being moved.
     */
    public int getNumberOfPieces() {
        return this.numberOfPieces;
    }

    /**
     * Gets the list of drop counts.
     *
     * @return The list of drop counts.
     */
    public List<Integer> getDropCounts() {
        return this.dropCounts;
    }
}
