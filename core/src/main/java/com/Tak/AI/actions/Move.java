package com.Tak.AI.actions;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Direction;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Move class represents a movement action where a player moves a stack of pieces in a direction,
 * dropping pieces along the way.
 */
public class Move extends Action implements Serializable {

    private static final long serialVersionUID = 1L;

    private int fromX;
    private int fromY;
    private Direction direction;
    private int numPieces;
    private List<Integer> dropCounts;
    private List<Piece> movedPieces;
    private List<Destination> destinations;
    private Player.Color playerColor;

    /**
     * Constructs a Move.
     *
     * @param fromX        The starting X coordinate.
     * @param fromY        The starting Y coordinate.
     * @param direction    The direction to move.
     * @param numPieces    The number of pieces to move.
     * @param dropCounts   The drop counts for each step.
     * @param playerColor  The color of the player making the move.
     */
    public Move(int fromX, int fromY, Direction direction, int numPieces, List<Integer> dropCounts, Player.Color playerColor) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.direction = direction;
        this.numPieces = numPieces;
        this.dropCounts = new ArrayList<>(dropCounts);
        this.movedPieces = new ArrayList<>();
        this.destinations = new ArrayList<>();
        this.playerColor = playerColor;
    }

    /**
     * Gets the starting X coordinate of the move.
     *
     * @return The starting X coordinate.
     */
    public int getStartX() { return fromX; }

    /**
     * Gets the starting Y coordinate of the move.
     *
     * @return The starting Y coordinate.
     */
    public int getStartY() { return fromY; }

    /**
     * Gets the direction of the move.
     *
     * @return The direction of the move.
     */
    public Direction getDirection() { return direction; }

    /**
     * Gets the drop counts for each step of the move.
     *
     * @return The drop counts for each step.
     */
    public List<Integer> getDropCounts() { return dropCounts; }

    /**
     * Executes the move on the given board.
     *
     * @param board The game board where the move will be executed.
     * @throws InvalidMoveException If the move is invalid or cannot be performed.
     */
    @Override
    public void execute(Board board) throws InvalidMoveException {
        Player player = board.getPlayerByColor(playerColor);
        if (player == null) {
            throw new InvalidMoveException("Player with color " + playerColor + " not found on the board.");
        }
        PieceStack stack = board.getBoardStack(fromX, fromY);
        if (stack.size() < numPieces) {
            throw new InvalidMoveException("Not enough pieces to move.");
        }
        Piece topPiece = stack.getTopPiece();
        if (!topPiece.getOwner().equals(player)) {
            throw new InvalidMoveException("Cannot move stack not owned by the player.");
        }
        movedPieces = new ArrayList<>(stack.getPieces().subList(stack.size() - numPieces, stack.size()));
        for (int i = 0; i < numPieces; i++) {
            stack.removeTopPiece();
        }
        int x = fromX;
        int y = fromY;
        for (int count : dropCounts) {
            switch (direction) {
                case UP:
                    y += 1;
                    break;
                case DOWN:
                    y -= 1;
                    break;
                case LEFT:
                    x -= 1;
                    break;
                case RIGHT:
                    x += 1;
                    break;
                default:
                    undo(board);
                    throw new InvalidMoveException("Unsupported direction: " + direction);
            }
            if (!board.isWithinBounds(x, y)) {
                undo(board);
                throw new InvalidMoveException("Move goes out of bounds.");
            }

            PieceStack destStack = board.getBoardStack(x, y);
            if (!destStack.getPieces().isEmpty()) {
                Piece destTopPiece = destStack.getTopPiece();
                Piece movingTopPiece = movedPieces.get(movedPieces.size() - 1);
                if (!canStackOnTop(movingTopPiece, destTopPiece)) {
                    undo(board);
                    throw new InvalidMoveException("Cannot stack on top of the target stack.");
                }
                if (movingTopPiece.isCapstone() && destTopPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
                    destTopPiece.setPieceType(Piece.PieceType.FLAT_STONE);
                    //Logger.debug("Move", "Capstone flattened a Standing Stone at (" + x + ", " + y + ").");
                }
            }
            List<Piece> piecesToDrop = new ArrayList<>(movedPieces.subList(0, count));
            movedPieces.subList(0, count).clear();
            for (Piece piece : piecesToDrop) {
                destStack.addPiece(piece);
            }
            destinations.add(new Destination(x, y, new ArrayList<>(piecesToDrop)));
            //Logger.debug("Move", player.getColor() + " dropped " + count + " piece(s) at (" + x + ", " + y + ").");

            if (movedPieces.isEmpty()) {
                break;
            }
        }
        if (!movedPieces.isEmpty()) {
            throw new InvalidMoveException("Not all pieces were dropped during the move.");
        }
    }

    /**
     * Undoes the move on the given board.
     *
     * @param board The game board where the move will be undone.
     * @throws InvalidMoveException If the move cannot be undone.
     */
    @Override
    public void undo(Board board) throws InvalidMoveException {
        for (int i = destinations.size() - 1; i >= 0; i--) {
            Destination dest = destinations.get(i);
            PieceStack destStack = board.getBoardStack(dest.getX(), dest.getY());
            for (int j = 0; j < dest.getDroppedPieces().size(); j++) {
                if (destStack.getPieces().isEmpty()) {
                    throw new InvalidMoveException("Cannot undo move: Destination stack is empty.");
                }
                Piece topPiece = destStack.getTopPiece();
                Piece expectedPiece = dest.getDroppedPieces().get(j);
                if (!topPiece.equals(expectedPiece)) {
                    throw new InvalidMoveException("Cannot undo move: Piece mismatch.");
                }
                destStack.removeTopPiece();
            }
            PieceStack originalStack = board.getBoardStack(fromX, fromY);
            for (Piece piece : dest.getDroppedPieces()) {
                originalStack.addPiece(piece);
            }
        }

        PieceStack originalStack = board.getBoardStack(fromX, fromY);
        for (Piece piece : movedPieces) {
            originalStack.addPiece(piece);
        }

        movedPieces.clear();
        destinations.clear();
        //Logger.debug("Move", "Move action undone for player " + playerColor + " at (" + fromX + ", " + fromY + ").");
    }

    /**
     * Checks if the moving piece can be stacked on the target piece.
     *
     * @param movingPiece The piece being moved.
     * @param targetPiece The piece on top of the destination stack.
     * @return true if stacking is allowed, false otherwise.
     */
    private boolean canStackOnTop(Piece movingPiece, Piece targetPiece) {
        if (targetPiece.getPieceType() == Piece.PieceType.CAPSTONE) {
            return false;
        }
        if (targetPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
            return movingPiece.isCapstone();
        }
        return true;
    }

    /**
     * Provides a string representation of the move.
     *
     * @return A string describing the move.
     */
    @Override
    public String toString() {
        return "Move: " + numPieces + " piece(s) from (" + fromX + ", " + fromY + ") to " + direction + " with drop counts " + dropCounts;
    }

    /**
     * Gets the color of the player making the move.
     *
     * @return The player color.
     */
    @Override
    public Player.Color getPlayerColor() {
        return this.playerColor;
    }

    /**
     * Checks if this move is equal to another object.
     *
     * @param obj The object to compare.
     * @return true if the moves are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        return this.fromX == other.fromX &&
                this.fromY == other.fromY &&
                this.direction == other.direction &&
                this.numPieces == other.numPieces &&
                this.dropCounts.equals(other.dropCounts) &&
                this.playerColor == other.playerColor;
    }

    /**
     * Computes the hash code for the move.
     *
     * @return The hash code of the move.
     */
    @Override
    public int hashCode() {
        return Objects.hash(fromX, fromY, direction, numPieces, dropCounts, playerColor);
    }

    /**
     * The Destination class represents a destination point in a move where pieces are dropped.
     */
    private static class Destination implements Serializable {
        private static final long serialVersionUID = 1L;

        private int x;
        private int y;
        private List<Piece> droppedPieces;

        /**
         * Constructs a Destination.
         *
         * @param x             The X coordinate of the destination.
         * @param y             The Y coordinate of the destination.
         * @param droppedPieces The pieces dropped at the destination.
         */
        public Destination(int x, int y, List<Piece> droppedPieces) {
            this.x = x;
            this.y = y;
            this.droppedPieces = droppedPieces;
        }

        /**
         * Gets the X coordinate of the destination.
         *
         * @return The X coordinate.
         */
        public int getX() {
            return x;
        }

        /**
         * Gets the Y coordinate of the destination.
         *
         * @return The Y coordinate.
         */
        public int getY() {
            return y;
        }

        /**
         * Gets the pieces dropped at the destination.
         *
         * @return The dropped pieces.
         */
        public List<Piece> getDroppedPieces() {
            return droppedPieces;
        }

        /**
         * Checks if this destination is equal to another object.
         *
         * @param obj The object to compare.
         * @return true if the destinations are equal, false otherwise.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Destination)) return false;
            Destination other = (Destination) obj;
            return this.x == other.x &&
                    this.y == other.y &&
                    droppedPieces.equals(other.droppedPieces);
        }

        /**
         * Computes the hash code for the destination.
         *
         * @return The hash code of the destination.
         */
        @Override
        public int hashCode() {
            return Objects.hash(x, y, droppedPieces);
        }
    }
}
