package Logic;

public class Piece {

    // Enum to define the types of pieces (flat stone, standing stone, capstone)
    public enum PieceType {
        FLAT_STONE,  // A regular flat stone that can be stacked and is part of a road
        STANDING_STONE,  // A standing stone (wall) that blocks roads
        CAPSTONE  // A capstone that can flatten standing stones and count toward roads
    }

    // The type of piece (flat stone, standing stone, or capstone)
    private PieceType pieceType;

    // The player that owns this piece
    private Player owner;

    // Constructor to initialize the piece with its type and the owning player
    public Piece(PieceType pieceType, Player owner) {
        this.pieceType = pieceType;
        this.owner = owner;
    }

    // Getter function to retrieve the type of the piece (flat stone, standing stone, capstone)
    public PieceType getPieceType() {
        return this.pieceType;
    }

    // Getter function to retrieve the owner of the piece
    public Player getOwner() {
        return this.owner;
    }

    // Function to check if this piece can be stacked (flat stones and standing stones can stack)
    public boolean isStackable() {
        // Only flat stones and standing stones can be stacked, capstones cannot be stacked upon
        return this.pieceType == PieceType.FLAT_STONE || this.pieceType == PieceType.STANDING_STONE;
    }

    // Function to check if this piece is a capstone
    public boolean isCapstone() {
        return this.pieceType == PieceType.CAPSTONE;
    }

    // Function to check if this piece can be part of a road (flat stones and capstones can)
    public boolean canBePartOfRoad() {
        return this.pieceType == PieceType.FLAT_STONE || this.pieceType == PieceType.CAPSTONE;
    }
}
