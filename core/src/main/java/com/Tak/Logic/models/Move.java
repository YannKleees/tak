package com.Tak.Logic.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.Tak.Logic.validators.MoveExecutor;
import com.Tak.Logic.validators.MoveValidator;
import com.Tak.AI.actions.Action;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

/**
 * The Move class represents a movement action in Tak.
 * It includes information about the move's starting position,
 * direction, number of pieces being moved, and drop counts.
 * The class provides methods to execute and undo the move on a game board.
 */
public class Move extends Action {

    private int startX;                 // Starting X coordinate.
    private int startY;                 // Starting Y coordinate.
    private Direction direction;        // Direction of movement.
    private int numberOfPieces;         // Number of pieces being moved.
    private List<Integer> dropCounts;   // Number of pieces to drop at each space.

    // State to store for undo functionality
    private List<Piece> movedPieces;    // Pieces that were moved during the execution
    private List<Integer> positionsX;   // X positions of the move path
    private List<Integer> positionsY;   // Y positions of the move path

    private Player player;              // Player who made the move

    public Move(int startX, int startY, Direction direction, int numberOfPieces, List<Integer> dropCounts) {
        this.startX = startX;
        this.startY = startY;
        this.direction = direction;
        this.numberOfPieces = numberOfPieces;
        this.dropCounts = dropCounts;

        // Initialize lists for undo functionality
        this.movedPieces = new ArrayList<>();
        this.positionsX = new ArrayList<>();
        this.positionsY = new ArrayList<>();
    }

    /**
     * Sets the player who made the move.
     *
     * @param player The player.
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Executes the move on the given game board.
     *
     * @param board The game board on which to execute the move.
     * @throws InvalidMoveException If the move is invalid according to game rules.
     */
    @Override
    public void execute(Board board) throws InvalidMoveException {
        if (player == null) {
            throw new InvalidMoveException("Player not set for this move.");
        }
        MoveExecutor executor = new MoveExecutor(board, player);
        executor.executeMove(startX, startY, this);
        //Logger.log("Move", "Executed move: " + this.toString());
    }

    /**
     * Undoes the move on the given game board.
     *
     * @param board The game board on which to undo the move.
     */
    @Override
    public void undo(Board board) {
        // Reverse the move by moving pieces back to their original positions
        // Iterate in reverse order
        for (int i = dropCounts.size() - 1; i >= 0; i--) {
            int x = positionsX.get(i + 1); // Destination positions start from index 1
            int y = positionsY.get(i + 1);
            int piecesToRemove = dropCounts.get(i);

            PieceStack destinationStack = board.getBoardStack(x, y);
            if (destinationStack == null || destinationStack.size() < piecesToRemove) {
                //Logger.log("Move", "Cannot undo move: insufficient pieces at (" + x + ", " + y + ").");
                return;
            }

            // Remove the pieces that were dropped
            List<Piece> removedPieces;
            try {
                removedPieces = destinationStack.removePieces(piecesToRemove);
            } catch (InvalidMoveException e) {
                //Logger.log("Move", "Error during undo: " + e.getMessage());
                return;
            }

            // Add them back to the source stack
            int sourceX = positionsX.get(0);
            int sourceY = positionsY.get(0);
            PieceStack sourceStack = board.getBoardStack(sourceX, sourceY);
            if (sourceStack == null) {
                //Logger.log("Move", "Cannot undo move: source stack at (" + sourceX + ", " + sourceY + ") does not exist.");
                return;
            }

            for (Piece piece : removedPieces) {
                sourceStack.addPiece(piece);
            }

            //Logger.log("Move", "Undid move: Moved " + piecesToRemove + " piece(s) back to (" + sourceX + ", " + sourceY + ").");
        }
    }

    /**
     * Gets the player who made the move.
     *
     * @return The player who made the move, or null if not set.
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Provides a string representation of the move.
     *
     * @return A string detailing the move.
     */
    @Override
    public String toString() {
        return "Move: " + numberOfPieces + " piece(s) from (" + startX + ", " + startY + ") to " + direction + " with drop counts " + dropCounts;
    }

    // Getters for properties

    public int getStartX() {
        return this.startX;
    }

    public int getStartY() {
        return this.startY;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public int getNumberOfPieces() {
        return this.numberOfPieces;
    }

    public List<Integer> getDropCounts() {
        return this.dropCounts;
    }

    public List<Piece> getMovedPieces() {
        return this.movedPieces;
    }

    public List<Integer> getPositionsX() {
        return this.positionsX;
    }

    public List<Integer> getPositionsY() {
        return this.positionsY;
    }

    @Override
    public Color getPlayerColor() {
        return player.getColor();
    }
}
