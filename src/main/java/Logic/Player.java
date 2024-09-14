package Logic;

import java.util.HashMap;
import java.util.Map;

/**
 * The Player class represents a player in the game.
 * It tracks the player's color and the number of remaining pieces.
 */
public class Player {

    /**
     * Enum to represent player colors.
     */
    public enum Color {
        BLACK,
        WHITE
    }

    private Color color; // The player's color.
    private Map<Piece.PieceType, Integer> remainingPieces; // Remaining pieces of each type.

    /**
     * Constructor to initialize the player with a color and piece counts.
     *
     * @param color          The player's color.
     * @param flatStones     The number of flat stones.
     * @param standingStones The number of standing stones.
     * @param capstones      The number of capstones.
     */
    public Player(Color color, int flatStones, int standingStones, int capstones) {
        this.color = color;
        this.remainingPieces = new HashMap<>();
        remainingPieces.put(Piece.PieceType.FLAT_STONE, flatStones);
        remainingPieces.put(Piece.PieceType.STANDING_STONE, standingStones);
        remainingPieces.put(Piece.PieceType.CAPSTONE, capstones);
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
     * Checks if the player has any remaining pieces of a specific type.
     *
     * @param pieceType The type of piece.
     * @return true if the player has remaining pieces of the given type, false otherwise.
     */
    public boolean hasPiecesLeft(Piece.PieceType pieceType) {
        return remainingPieces.getOrDefault(pieceType, 0) > 0;
    }

    /**
     * Decrements the count of a specific piece type after placement.
     *
     * @param pieceType The type of piece placed.
     */
    public void decrementPiece(Piece.PieceType pieceType) {
        int count = remainingPieces.getOrDefault(pieceType, 0);
        if (count > 0) {
            remainingPieces.put(pieceType, count - 1);
        }
    }

    /**
     * Gets the number of remaining pieces of a specific type.
     *
     * @param pieceType The type of piece.
     * @return The number of remaining pieces.
     */
    public int getRemainingPieces(Piece.PieceType pieceType) {
        return remainingPieces.getOrDefault(pieceType, 0);
    }

    /**
     * Resets the player's pieces (for starting a new game).
     *
     * @param flatStones     The number of flat stones.
     * @param standingStones The number of standing stones.
     * @param capstones      The number of capstones.
     */
    public void resetPieces(int flatStones, int standingStones, int capstones) {
        remainingPieces.put(Piece.PieceType.FLAT_STONE, flatStones);
        remainingPieces.put(Piece.PieceType.STANDING_STONE, standingStones);
        remainingPieces.put(Piece.PieceType.CAPSTONE, capstones);
    }
}
