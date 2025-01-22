package com.Tak.AI.players;

import com.Tak.AI.actions.Action;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;

import java.util.List;
import java.util.Random;

/**
 * The RandomAIPlayer class represents an AI player that selects moves at random.
 * It uses the ActionGenerator to generate all possible valid actions and selects one randomly.
 */
public class RandomAIPlayer extends Player {

    /**
     * Constructs a RandomAIPlayer with specified piece counts.
     *
     * @param color           The color of the player (BLACK or WHITE).
     * @param flatStones      Number of flat stones.
     * @param standingStones  Number of standing stones.
     * @param capstones       Number of capstones.
     */
    public RandomAIPlayer(Color color, int flatStones, int standingStones, int capstones) {
        super(color, flatStones, capstones);
    }

    /**
     * Makes a move by generating all possible actions and selecting one at random.
     *
     * @param game The current TakGame instance.
     * @throws InvalidMoveException If an invalid move is attempted.
     * @throws GameOverException    If the game has already ended.
     */
    @Override
    public void makeMove(TakGame game) throws GameOverException {
        Board board = game.getBoard();
        int moveCount = board.getMoveCount();
        List<String> possibleActions = ActionGenerator.generatePossibleActions(board, this, moveCount);
    
        if (possibleActions.isEmpty()) {
            throw new GameOverException("No valid moves available.");
        }
    
        Random random = new Random();
    
        while (!possibleActions.isEmpty()) {
            String actionStr = possibleActions.remove(random.nextInt(possibleActions.size()));
            try {
                Action action = Action.fromString(actionStr, this.getColor());
                action.execute(board);
                board.incrementMoveCount();
                game.switchPlayer();
                return; // Successful move
            } catch (InvalidMoveException e) {
                // Invalid move, try another
            }
        }
    
        // If no valid moves are left
        throw new GameOverException("No valid moves available.");
    }
    

    /**
     * Creates a copy of this RandomAIPlayer.
     *
     * @return A new RandomAIPlayer instance with the same properties.
     */
    @Override
    public Player copy() {
        int flatStones = getRemainingPieces(Piece.PieceType.FLAT_STONE);
        int standingStones = getRemainingPieces(Piece.PieceType.STANDING_STONE);
        int capstones = getRemainingPieces(Piece.PieceType.CAPSTONE);
        RandomAIPlayer copy = new RandomAIPlayer(this.getColor(), flatStones, standingStones, capstones);
        // Opponent will be set appropriately by the Board's copy method
        return copy;
    }

    /**
     * Gets the total number of pieces left for the player.
     *
     * @return The total pieces left.
     */
    @Override
    public int getTotalPiecesLeft() {
        return getRemainingPieces(Piece.PieceType.FLAT_STONE)
                + getRemainingPieces(Piece.PieceType.STANDING_STONE)
                + getRemainingPieces(Piece.PieceType.CAPSTONE);
    }
}
