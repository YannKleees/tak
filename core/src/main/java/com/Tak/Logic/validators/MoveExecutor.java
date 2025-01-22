package com.Tak.Logic.validators;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Move;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes moves on the board.
 */
public class MoveExecutor {
    private Board board;
    private Player currentPlayer;

    public MoveExecutor(Board board, Player currentPlayer) {
        this.board = board;
        this.currentPlayer = currentPlayer;
    }

    /**
     * Executes a move after validation.
     *
     * @param fromX Starting X coordinate.
     * @param fromY Starting Y coordinate.
     * @param move  The move details.
     * @throws InvalidMoveException If the move is invalid.
     */
    public void executeMove(int fromX, int fromY, Move move) throws InvalidMoveException {
        int x = fromX;
        int y = fromY;

        // Retrieve the source stack
        PieceStack sourceStack = board.getBoardStack(x, y);
        if (sourceStack == null) {
            throw new InvalidMoveException("Source position is invalid.");
        }

        // Check if there are enough pieces to move
        if (sourceStack.size() < move.getNumberOfPieces()) {
            throw new InvalidMoveException("Not enough pieces to move from the source stack.");
        }

        // Extract the pieces to move (from top)
        List<Piece> piecesToMove = new ArrayList<>(sourceStack.getPieces().subList(sourceStack.size() - move.getNumberOfPieces(), sourceStack.size()));
        // Remove the pieces from the source stack
        for (int i = 0; i < move.getNumberOfPieces(); i++) {
            sourceStack.removeTopPiece();
        }

        // Store moved pieces and their original positions for undo functionality
        move.getMovedPieces().addAll(piecesToMove);
        move.getPositionsX().add(x);
        move.getPositionsY().add(y);

        // Determine movement direction
        Direction direction = move.getDirection();
        int dx = direction.getDeltaX();
        int dy = direction.getDeltaY();

        // Perform the move by dropping pieces as per dropCounts
        for (int dropCount : move.getDropCounts()) {
            x += dx;
            y += dy;

            // Check if new position is within bounds
            if (!board.isWithinBounds(x, y)) {
                throw new InvalidMoveException("Move goes out of bounds at position (" + x + ", " + y + ").");
            }

            PieceStack destinationStack = board.getBoardStack(x, y);

            // Check stacking rules
            if (!destinationStack.getPieces().isEmpty()) {
                Piece topPiece = destinationStack.getTopPiece();
                Piece movingPiece = piecesToMove.get(0); // Top piece being moved

                if (!canStackOnTop(movingPiece, topPiece)) {
                    throw new InvalidMoveException("Cannot stack " + movingPiece.getPieceType() + " on top of " + topPiece.getPieceType() + " at position (" + x + ", " + y + ").");
                }

                // Handle capstone flattening standing stones
                if (movingPiece.isCapstone() && topPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
                    topPiece.setPieceType(Piece.PieceType.FLAT_STONE);
                    //Logger.log("MoveExecutor", "Capstone flattened a Standing Stone at (" + x + ", " + y + ").");
                }
            }

            // **Pre-Move Stack Height Check**
            if (destinationStack.size() + dropCount > board.getCarryLimit()) {
                throw new InvalidMoveException("Cannot exceed stack height of " + board.getCarryLimit() + " at position (" + x + ", " + y + ").");
            }

            // Drop the specified number of pieces onto the destination stack
            List<Piece> piecesToDrop = new ArrayList<>(piecesToMove.subList(0, dropCount));
            for (int i = 0; i < dropCount; i++) {
                destinationStack.addPiece(piecesToMove.remove(0));
            }

            // Log the drop action
            //Logger.log("MoveExecutor", currentPlayer.getColor() + " dropped " + dropCount + " piece(s) at (" + x + ", " + y + ").");

            // Store the new position for undo functionality
            move.getPositionsX().add(x);
            move.getPositionsY().add(y);

            if (piecesToMove.isEmpty()) {
                break; // All pieces have been dropped
            }
        }

        // After moving, ensure all pieces have been dropped
        if (!piecesToMove.isEmpty()) {
            throw new InvalidMoveException("Not all pieces were dropped during the move.");
        }
    }


    /**
     * Checks if the moving piece can be legally stacked on top of the target piece.
     *
     * @param movingPiece The piece attempting to stack.
     * @param targetPiece The top piece at the target location.
     * @return true if the moving piece can be stacked, false otherwise.
     */
    private boolean canStackOnTop(Piece movingPiece, Piece targetPiece) {
        if (targetPiece.getPieceType() == Piece.PieceType.CAPSTONE) {
            return false; // Cannot stack on top of a capstone
        }
        if (targetPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
            return movingPiece.isCapstone(); // Only capstone can flatten standing stone
        }
        return true; // Flat stones can be stacked on flat stones
    }
}
