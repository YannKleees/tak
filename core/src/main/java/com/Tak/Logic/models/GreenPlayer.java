// File: core/src/main/java/com/Tak/Logic/models/GreenPlayer.java
package com.Tak.Logic.models;

import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;

public class GreenPlayer extends Player {
    public GreenPlayer(int stones, int capstones) {
        super(Color.GREEN, stones, capstones);
    }

    @Override
    public Player copy() {
        GreenPlayer copy = new GreenPlayer(
            this.getRemainingPieces(Piece.PieceType.FLAT_STONE),
            this.getRemainingPieces(Piece.PieceType.CAPSTONE)
        );
        copy.setScore(this.getScore());
        return copy;
    }

    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        throw new UnsupportedOperationException("GreenPlayer's makeMove logic is not yet implemented.");
    }

    @Override
    public int getTotalPiecesLeft() {
        return this.getRemainingPieces(Piece.PieceType.FLAT_STONE)
            + this.getRemainingPieces(Piece.PieceType.CAPSTONE);
    }
}
