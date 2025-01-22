package com.Tak.AI.actions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.utils.InvalidMoveException;

/**
 * The Action class represents a generic action in the Tak game.
 * It serves as an abstract base class for specific action types like Placement and Move.
 */
public abstract class Action implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Executes the action on the given board.
     *
     * @param board The game board where the action will be executed.
     * @throws InvalidMoveException If the action is invalid or cannot be performed.
     */
    public abstract void execute(Board board) throws InvalidMoveException;

    /**
     * Undoes the action on the given board.
     *
     * @param board The game board where the action will be undone.
     * @throws InvalidMoveException If the action cannot be undone.
     */
    public abstract void undo(Board board) throws InvalidMoveException;

    /**
     * Provides a string representation of the action.
     *
     * @return A string describing the action.
     */
    @Override
    public abstract String toString();

    /**
     * Gets the player color associated with this action.
     *
     * @return The player color performing the action.
     */
    public abstract Player.Color getPlayerColor();

    /**
     * Parses an action string and creates an Action object.
     *
     * @param actionStr    The action string.
     * @param playerColor  The color of the player performing the action.
     * @return The corresponding Action object.
     * @throws InvalidMoveException If the action string is invalid or cannot be parsed.
     */
    public static Action fromString(String actionStr, Player.Color playerColor) throws InvalidMoveException {
        String[] parts = actionStr.trim().split(" ");
        String actionType = parts[0];
        switch (actionType) {
            case "PLACE_FLAT_STONE":
                if (parts.length != 3) {
                    throw new InvalidMoveException("Invalid PLACE_FLAT_STONE action format.");
                }
                int xFs = Integer.parseInt(parts[1]);
                int yFs = Integer.parseInt(parts[2]);
                return new Placement(xFs, yFs, Piece.PieceType.FLAT_STONE, playerColor);
            case "PLACE_CAPSTONE":
                if (parts.length != 3) {
                    throw new InvalidMoveException("Invalid PLACE_CAPSTONE action format.");
                }
                int xCs = Integer.parseInt(parts[1]);
                int yCs = Integer.parseInt(parts[2]);
                return new Placement(xCs, yCs, Piece.PieceType.CAPSTONE, playerColor);
            case "MOVE":
                if (parts.length < 5) {
                    throw new InvalidMoveException("Invalid MOVE action format.");
                }
                int fromX = Integer.parseInt(parts[1]);
                int fromY = Integer.parseInt(parts[2]);
                Direction direction;
                try {
                    direction = Direction.valueOf(parts[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new InvalidMoveException("Invalid direction in MOVE action.");
                }
                int numPieces = Integer.parseInt(parts[4]);
                List<Integer> dropCounts = new ArrayList<>();
                for (int i = 5; i < parts.length; i++) {
                    dropCounts.add(Integer.parseInt(parts[i]));
                }
                int sum = dropCounts.stream().mapToInt(Integer::intValue).sum();
                if (sum != numPieces) {
                    throw new InvalidMoveException("Sum of drop counts does not equal number of pieces to move.");
                }
                return new Move(fromX, fromY, direction, numPieces, dropCounts, playerColor);
            default:
                throw new InvalidMoveException("Unknown action type: " + actionType);
        }
        
    }
    
}
