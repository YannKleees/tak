// File: core/src/main/java/com/Tak/Logic/models/Player.java
package com.Tak.Logic.models;

import java.util.Objects;
import java.io.Serializable;

import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

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
    protected int score; // Changed to protected to allow subclasses to access

    /**
     * Constructor to initialize a player.
     *
     * @param color           The color of the player (BLACK or WHITE).
     * @param stones          Total number of stones (flat and standing combined).
     * @param capstones       Number of capstones.
     */
    public Player(Color color, int stones, int capstones) {
        this.color = color;
        this.stones = stones;
        this.capstones = capstones;
        this.score = 0;
    }

    /**
     * Sets the opponent player.
     *
     * @param opponent The opponent player.
     */
    public void setOpponent(Player opponent) {
        this.opponent = opponent;
    }

    /**
     * Gets the opponent player.
     *
     * @return The opponent player.
     */
    public Player getOpponent() {
        return this.opponent;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    /**
     * Decrements the count of a specific piece type.
     *
     * @param pieceType The type of piece to decrement.
     */
    public void decrementPiece(Piece.PieceType pieceType) {
        switch (pieceType) {
            case FLAT_STONE:
            case STANDING_STONE:
                if (stones > 0) {
                    stones--;
                } else {
                    //Logger.log("Player", "No stones left to decrement.");
                }
                break;
            case CAPSTONE:
                if (capstones > 0) {
                    capstones--;
                } else {
                    //Logger.log("Player", "No CAPSTONEs left to decrement.");
                }
                break;
            default:
                //Logger.log("Player", "Unknown PieceType: " + pieceType);
                break;
        }
    }

    /**
     * Increments the count of a specific piece type.
     *
     * @param pieceType The type of piece to increment.
     */
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
                //Logger.log("Player", "Unknown PieceType: " + pieceType);
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
    

    /**
     * Gets the number of remaining pieces of a specific type.
     *
     * @param pieceType The type of piece.
     * @return The number of remaining pieces.
     */
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

    /**
     * Resets the player's pieces to the specified counts.
     *
     * @param stones     Total number of stones (flat and standing combined).
     * @param capstones Number of capstones.
     */
    public void resetPieces(int stones, int capstones) {
        this.stones = stones;
        this.capstones = capstones;
        System.out.println(this.color + " pieces have been reset.");
    }

    /**
     * Resets the player's score.
     */
    public void resetScore() {
        this.score = 0;
        System.out.println(this.color + " score has been reset.");
    }

    /**
     * Increments the player's score.
     *
     * @param points The number of points to add.
     */
    public void incrementScore(int points) {
        this.score += points;
        System.out.println(this.color + " score increased by " + points + " to " + this.score + ".");
    }

    /**
     * Gets the player's color.
     *
     * @return The player's color.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Gets the player's current score.
     *
     * @return The player's score.
     */
    public int getScore() {
        return this.score;
    }
    public void updateFrom(Player other) {
        // Update internal behavior (fields, strategies, etc.)
        this.stones = other.getRemainingPieces(Piece.PieceType.FLAT_STONE);
        this.capstones = other.getRemainingPieces(Piece.PieceType.CAPSTONE);
        this.opponent = other.getOpponent(); // Maintain opponent relationships
        this.score = other.getScore();
    }
    
    /**
     * Abstract method to create a copy of the player.
     *
     * @return A new Player instance with the same properties.
     */
    public abstract Player copy();

    /**
     * Abstract method to execute the player's move.
     *
     * @param game The current TakGame instance.
     * @throws InvalidMoveException If an invalid move is attempted.
     * @throws GameOverException    If the game has already ended.
     */
    public abstract void makeMove(TakGame game) throws InvalidMoveException, GameOverException;

    /**
     * Overrides equals method to compare Players based on unique attributes.
     *
     * @param obj The object to compare.
     * @return true if equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Player)) return false;
        Player other = (Player) obj;
        return this.color == other.color;
    }

    /**
     * Overrides hashCode method consistent with equals.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        return Objects.hash(color);
    }

    public abstract int getTotalPiecesLeft();
}
