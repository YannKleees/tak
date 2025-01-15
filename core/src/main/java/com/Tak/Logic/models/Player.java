// File: core/src/main/java/com/Tak/Logic/models/Player.java
package com.Tak.Logic.models;

import java.util.Objects;
import java.io.Serializable;

import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;

/**
 * The Player class represents a player in the Tak game.
 * It serves as an abstract base class for different player types.
 */
public abstract class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Color { BLUE, GREEN }

    private Color color;
    private Player opponent;
    private int stones;        // Combined count for flat and standing stones
    private int capstones;
    protected int score;

    /**
     * Constructor to initialize a player.
     *
     * @param color    The color of the player (e.g., BLUE or GREEN).
     * @param stones   Total number of stones (flat + standing).
     * @param capstones Number of capstones.
     */
    public Player(Color color, int stones, int capstones) {
        this.color = color;
        this.stones = stones;
        this.capstones = capstones;
        this.score = 0;
    }

    // ----------------------------------------------------------------
    // Basic getters/setters and piece tracking
    // ----------------------------------------------------------------

    public Color getColor() {
        return this.color;
    }

    public void setOpponent(Player opponent) {
        this.opponent = opponent;
    }

    public Player getOpponent() {
        return this.opponent;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return this.score;
    }

    public void decrementPiece(Piece.PieceType pieceType) {
        switch (pieceType) {
            case FLAT_STONE:
            case STANDING_STONE:
                if (stones > 0) {
                    stones--;
                }
                break;
            case CAPSTONE:
                if (capstones > 0) {
                    capstones--;
                }
                break;
            default:
                break;
        }
    }

    public void incrementPiece(Piece.PieceType pieceType) {
        switch (pieceType) {
            case FLAT_STONE:
            case STANDING_STONE:
                stones++;
                break;
            case CAPSTONE:
                capstones++;
                break;
            default:
                break;
        }
    }

    public boolean hasPieces(Piece.PieceType pieceType) {
        return hasPiecesLeft(pieceType);
    }

    public boolean hasPiecesLeft(Piece.PieceType pieceType) {
        switch (pieceType) {
            case FLAT_STONE:
            case STANDING_STONE:
                return stones > 0;
            case CAPSTONE:
                return capstones > 0;
            default:
                return false;
        }
    }

    public int getRemainingPieces(Piece.PieceType pieceType) {
        switch (pieceType) {
            case FLAT_STONE:
            case STANDING_STONE:
                return stones;
            case CAPSTONE:
                return capstones;
            default:
                return 0;
        }
    }

    public void resetPieces(int stones, int capstones) {
        this.stones = stones;
        this.capstones = capstones;
        System.out.println(this.color + " pieces have been reset.");
    }

    public void resetScore() {
        this.score = 0;
        System.out.println(this.color + " score has been reset.");
    }

    public void incrementScore(int points) {
        this.score += points;
        System.out.println(this.color + " score increased by " + points + " to " + this.score + ".");
    }

    // ----------------------------------------------------------------
    // Abstract methods that subclasses must implement
    // ----------------------------------------------------------------

    public abstract Player copy();

    public abstract void makeMove(TakGame game) throws InvalidMoveException, GameOverException;

    public abstract int getTotalPiecesLeft();

    // ----------------------------------------------------------------
    // "Can Make A Move" logic (for 'no moves left' detection)
    // ----------------------------------------------------------------

    public boolean canMakeAMove(Board board) {
        // 1) Check if we can place a piece
        if (canPlaceAnyPiece(board)) {
            return true;
        }
        // 2) Check if we can move a stack
        if (canMoveAStack(board)) {
            return true;
        }
        return false;
    }

    protected boolean canPlaceAnyPiece(Board board) {
        // If the player has any stone or capstone left...
        boolean hasAnyPieceLeft = (stones > 0 || capstones > 0);
        if (!hasAnyPieceLeft) {
            return false;
        }

        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                if (board.isCellEmpty(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean canMoveAStack(Board board) {
        // Simplified: if there's any stack whose top belongs to us, we say "we can move"
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Piece top = board.getPieceAt(x, y);
                if (top != null && top.getOwner().equals(this)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ----------------------------------------------------------------
    // Equals & hashCode
    // ----------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Player)) return false;
        Player other = (Player) obj;
        return this.color == other.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }

    /**
     * Updates this player's state from another (for copying).
     */
    public void updateFrom(Player other) {
        this.stones = other.getRemainingPieces(Piece.PieceType.FLAT_STONE);
        this.capstones = other.getRemainingPieces(Piece.PieceType.CAPSTONE);
        this.opponent = other.getOpponent();
        this.score = other.getScore();
    }
}
