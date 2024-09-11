package Logic;

import java.util.List;
import java.util.ArrayList;

public class Player {

    // The player's name or identifier
    private String name;

    // List of pieces owned by the player
    private List<Piece> pieces;

    // Constructor to initialize the player with a name
    public Player(String name) {
        this.name = name;
        this.pieces = new ArrayList<>();
    }

    // Getter function to retrieve the player's name
    public String getName() {
        return this.name;
    }

    // Function to add a piece to the player's collection of pieces
    public void addPiece(Piece piece) {
        this.pieces.add(piece);
    }

    // Function to remove a piece from the player's collection of pieces
    public void removePiece(Piece piece) {
        this.pieces.remove(piece);
    }

    // Function to get the player's list of pieces
    public List<Piece> getPieces() {
        return this.pieces;
    }

    // Function to check if the player has any pieces left
    public boolean hasPiecesLeft() {
        return !this.pieces.isEmpty();
    }

    // Function to count the number of pieces the player has
    public int countPieces() {
        return this.pieces.size();
    }

    // Function to reset the player's pieces (for starting a new game)
    public void resetPieces() {
        this.pieces.clear();
    }
}
