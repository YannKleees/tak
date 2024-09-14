package Logic;

/**
 * The Piece class represents a game piece in Tak.
 * It holds information about the piece type and its owner.
 */
public class Piece {

    /**
     * Enum to define the types of pieces in Tak.
     */
    public enum PieceType {
        FLAT_STONE,     // A flat stone that can be stacked and is part of a road.
        STANDING_STONE, // A standing stone (wall) that blocks roads.
        CAPSTONE        // A capstone that can flatten standing stones and count toward roads.
    }

    private PieceType pieceType; // The type of the piece.
    private Player owner;        // The player who owns the piece.

    /**
     * Constructor to initialize the piece with its type and owner.
     *
     * @param pieceType The type of the piece.
     * @param owner     The owner of the piece.
     */
    public Piece(PieceType pieceType, Player owner) {
        this.pieceType = pieceType;
        this.owner = owner;
    }

    /**
     * Gets the type of the piece.
     *
     * @return The piece type.
     */
    public PieceType getPieceType() {
        return this.pieceType;
    }

    /**
     * Sets the type of the piece.
     * This may be needed when a capstone flattens a standing stone.
     *
     * @param pieceType The new piece type.
     */
    public void setPieceType(PieceType pieceType) {
        this.pieceType = pieceType;
    }

    /**
     * Gets the owner of the piece.
     *
     * @return The player who owns the piece.
     */
    public Player getOwner() {
        return this.owner;
    }

    /**
     * Checks if the piece is a capstone.
     *
     * @return true if the piece is a capstone, false otherwise.
     */
    public boolean isCapstone() {
        return this.pieceType == PieceType.CAPSTONE;
    }

    /**
     * Checks if the piece can be part of a road.
     *
     * @return true if the piece can be part of a road, false otherwise.
     */
    public boolean canBePartOfRoad() {
        return this.pieceType == PieceType.FLAT_STONE || this.pieceType == PieceType.CAPSTONE;
    }
}
