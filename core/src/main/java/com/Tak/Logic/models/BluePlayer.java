// File: core/src/main/java/com/Tak/Logic/models/BluePlayer.java
package com.Tak.Logic.models;

import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;

public class BluePlayer extends Player {
    public BluePlayer(int stones, int capstones) {
        super(Color.BLUE, stones, capstones);
    }

    @Override
    public Player copy() {
        BluePlayer copy = new BluePlayer(
            this.getRemainingPieces(Piece.PieceType.FLAT_STONE),
            this.getRemainingPieces(Piece.PieceType.CAPSTONE)
        );
        copy.setScore(this.getScore());
        return copy;
    }

    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        throw new UnsupportedOperationException("BluePlayer's makeMove logic is not yet implemented.");
    }

    @Override
    public int getTotalPiecesLeft() {
        return this.getRemainingPieces(Piece.PieceType.FLAT_STONE)
            + this.getRemainingPieces(Piece.PieceType.CAPSTONE);
    }
}
