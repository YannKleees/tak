package com.Tak.Logic.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

/**
 * Represents a stack of pieces at a specific board position.
 */
public class PieceStack {
    private List<Piece> pieces;

    public PieceStack() {
        this.pieces = new ArrayList<>();
    }

    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    public Piece removeTopPiece() {
        if (pieces.isEmpty()) return null;
        Piece top = pieces.remove(pieces.size() - 1);
        return top;
    }

    public Piece getTopPiece() {
        if (pieces.isEmpty()) return null;
        return pieces.get(pieces.size() - 1);
    }

    public int size() {
        return pieces.size();
    }

    public List<Piece> getPieces() {
        return new ArrayList<>(pieces);
    }

    /**
     * Clears all pieces from the stack.
     */
    public void clear() {
        pieces.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PieceStack)) return false;
        PieceStack other = (PieceStack) obj;
        return this.pieces.equals(other.pieces);
    }

    @Override
    public int hashCode() {
        return pieces.hashCode();
    }

    public boolean isEmpty() {
        return pieces.isEmpty();
    }

    /**
     * Removes a specified number of pieces from the top of the stack.
     *
     * @param amount The number of pieces to remove.
     * @return A list of removed pieces.
     * @throws InvalidMoveException If there aren't enough pieces to remove.
     */
    public List<Piece> removePieces(int amount) throws InvalidMoveException {
        if (pieces.size() < amount) {
            throw new InvalidMoveException("Not enough pieces to remove from the stack.");
        }
        List<Piece> removed = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            removed.add(removeTopPiece());
        }
        return removed;
    }

    /**
     * Retrieves the owner of the top piece in the stack.
     *
     * @return The Player who owns the top piece, or null if the stack is empty.
     */
    public Player getOwner() {
        Piece topPiece = getTopPiece();
        if (topPiece != null) {
            return topPiece.getOwner();
        }
        return null;
    }
    public Player getControlledBy() {
        if (!this.isEmpty()) {
            return this.getTopPiece().getOwner();
        }
        return null;
    }
    /**
     * Creates a deep copy of the current PieceStack.
     *
     * @param playerMap Map of original players to their copies.
     * @return A new PieceStack instance with copied pieces.
     */
    public PieceStack copy(Map<Player, Player> playerMap) {
        PieceStack newStack = new PieceStack();
        for (Piece piece : this.pieces) {
            Player ownerCopy = playerMap.get(piece.getOwner());
            Piece copiedPiece = new Piece(piece.getPieceType(), ownerCopy);
            newStack.addPiece(copiedPiece);
        }
        return newStack;
    }
}
