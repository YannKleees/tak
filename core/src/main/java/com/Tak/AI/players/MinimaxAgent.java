package com.Tak.AI.players;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.AI.search.MiniMaxAlgorithm2;
import com.Tak.AI.search.MinimaxAlgorithm;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.Logger;

import java.io.Serializable;
import java.util.Objects;

/**
 * MinimaxAgent represents an AI player using the Minimax algorithm for decision-making.
 */
public class MinimaxAgent extends Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private MiniMaxAlgorithm2 minimaxAlgorithm;
    private EvaluationFunction evaluationFunction;
    private int maxDepth;

    /**
     * Constructs a MinimaxAgent with specified parameters.
     *
     * @param color          The color of the player.
     * @param flatStones     Number of flat stones.
     * @param standingStones Number of standing stones.
     * @param capstones      Number of capstones.
     * @param maxDepth       The maximum depth for the Minimax search.
     */
    public MinimaxAgent(Color color, int flatStones, int standingStones,
                        int capstones, int maxDepth) {
        super(color, flatStones, capstones);
        this.evaluationFunction = new EvaluationFunction();
        this.maxDepth = maxDepth;
        this.minimaxAlgorithm = new MiniMaxAlgorithm2(evaluationFunction, maxDepth, this);
    }

    /**
     * Executes the AI player's move using the Minimax algorithm.
     *
     * @param game The current game instance.
     * @throws InvalidMoveException If the move is invalid.
     * @throws GameOverException    If the game is over.
     */
    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        // If it's the second overall move (moveCount == 1) and this AI is GREEN,
        // forcibly place a BLUE stone, ignoring normal Minimax for just this one move.
        if (game.getMoveCount() == 1 && this.getColor() == Player.Color.GREEN) {
            // 1) Figure out which of game.getPlayer1() or game.getPlayer2() is actually the BLUE player.
            Player bluePlayer = (game.getPlayer1().getColor() == Player.Color.BLUE)
                ? game.getPlayer1() : game.getPlayer2();

            // 2) Choose the forced coordinates for your AI's first placement.
            //    (Here we just pick (2, 2) as an example.)
            int forcedX = 2;
            int forcedY = 2;

            // 3) Call placePiece(...) with the forcedX/forcedY, a FLAT_STONE,
            //    and force the owner to be that bluePlayer.
            game.placePiece(forcedX, forcedY, Piece.PieceType.FLAT_STONE, bluePlayer);

            // Return early to skip the normal Minimax logic just for this move.
            return;
        }

        // =====================================================
        // Otherwise, use normal Minimax logic for all other moves
        // =====================================================
        Board board = game.getBoard();
        Action bestMove = minimaxAlgorithm.findBestMove(board, this, game.getMoveCount());
        if (bestMove != null) {
            // Just execute the board changes
            bestMove.execute(board);
            game.incrementMoveCount();
            game.checkWinConditions();
            game.switchPlayer(); // Add this line

            Logger.debug("MinimaxAgent", this.getColor() + " executed move: " + bestMove.toString());
            // Do not call game.switchPlayer(), incrementMoveCount(), etc.
            // Let TakGame handle those in placePiece / moveStack.
        } else {
            throw new InvalidMoveException("No valid moves available for the AI.");
        }
    }



    /**
     * Creates a copy of this MinimaxAgent.
     *
     * @return A copy of this MinimaxAgent.
     */
    @Override
    public Player copy() {
        return new MinimaxAgent(this.getColor(),
                                this.getRemainingPieces(PieceType.FLAT_STONE),
                                this.getRemainingPieces(PieceType.STANDING_STONE),
                                this.getRemainingPieces(PieceType.CAPSTONE),
                                this.maxDepth);
    }

    /**
     * Compares MinimaxAgents based on Player properties.
     *
     * @param obj The object to compare.
     * @return true if the agents are equal, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof MinimaxAgent;
    }

    /**
     * Computes the hash code for this MinimaxAgent.
     *
     * @return The hash code of this MinimaxAgent.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), "MinimaxAgent");
    }

    /**
     * Retrieves the maximum search depth.
     *
     * @return The maximum depth.
     */
    public int getMaxDepth() {
        return this.maxDepth;
    }

    @Override
    public int getTotalPiecesLeft() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTotalPiecesLeft'");
    }
}
