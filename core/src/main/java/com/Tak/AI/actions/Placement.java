package com.Tak.AI.actions;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

import java.io.Serializable;
import java.util.Objects;

/**
 * The Placement class represents a placement action in the Tak game.
 */
public class Placement extends Action implements Serializable {
    private static final long serialVersionUID = 1L;
    private int x, y;
    private Piece.PieceType pieceType;
    private Player.Color playerColor;

    /**
     * Constructs a Placement action.
     *
     * @param x           The X coordinate where the piece will be placed.
     * @param y           The Y coordinate where the piece will be placed.
     * @param pieceType   The type of piece to place (FLAT_STONE or CAPSTONE).
     * @param playerColor The color of the player performing the placement.
     */
    public Placement(int x, int y, Piece.PieceType pieceType, Player.Color playerColor) {
        this.x = x;
        this.y = y;
        this.pieceType = pieceType;
        this.playerColor = playerColor;
    }

    /**
     * Gets the type of piece being placed.
     *
     * @return The piece type.
     */
    public Piece.PieceType getPieceType() {
        return this.pieceType;
    }

    /**
     * Gets the X coordinate where the piece is placed.
     *
     * @return The X coordinate.
     */
    public int getX() {
        return this.x;
    }

    /**
     * Gets the Y coordinate where the piece is placed.
     *
     * @return The Y coordinate.
     */
    public int getY() {
        return this.y;
    }

    /**
     * Executes the placement action on the given board.
     *
     * @param board The game board where the piece will be placed.
     * @throws InvalidMoveException If the placement is invalid or cannot be performed.
     */
    @Override
    public void execute(Board board) throws InvalidMoveException {
        Player player = board.getPlayerByColor(playerColor);
        if (player == null) {
            throw new InvalidMoveException("Player with color " + playerColor + " not found on the board.");
        }

        Piece piece = new Piece(pieceType, player);
        board.placePiece(x, y, piece);
        player.decrementPiece(pieceType);
    }

    /**
     * Undoes the placement action on the given board.
     *
     * @param board The game board where the placement will be undone.
     * @throws InvalidMoveException If the placement cannot be undone.
     */
    @Override
    public void undo(Board board) throws InvalidMoveException {
        Player player = board.getPlayerByColor(playerColor);
        if (player == null) {
            throw new InvalidMoveException("Player with color " + playerColor + " not found on the board.");
        }

        PieceStack stack = board.getBoardStack(x, y);
        if (stack.isEmpty()) {
            throw new InvalidMoveException("Cannot undo placement: Stack is already empty.");
        }
        Piece topPiece = stack.getTopPiece();
        if (!topPiece.getPieceType().equals(this.pieceType) || !topPiece.getOwner().equals(player)) {
            throw new InvalidMoveException("Cannot undo placement: Top piece does not match.");
        }
        stack.removeTopPiece();
        player.incrementPiece(pieceType);
    }

    /**
     * Provides a string representation of the placement action.
     *
     * @return A string describing the placement.
     */
    @Override
    public String toString() {
        return playerColor + " placed " + pieceType + " at (" + x + ", " + y + ")";
    }

    /**
     * Gets the color of the player performing the placement.
     *
     * @return The player color.
     */
    @Override
    public Player.Color getPlayerColor() {
        return this.playerColor;
    }

    /**
     * Checks if this placement action is equal to another object.
     *
     * @param obj The object to compare.
     * @return true if the actions are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Placement)) return false;
        Placement other = (Placement) obj;
        return this.x == other.x && this.y == other.y && this.pieceType == other.pieceType && this.playerColor == other.playerColor;
    }

    /**
     * Computes the hash code for the placement action.
     *
     * @return The hash code of the placement action.
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y, pieceType, playerColor);
    }
}
