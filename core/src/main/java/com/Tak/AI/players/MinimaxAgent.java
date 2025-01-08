package com.Tak.AI.players;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.AI.search.MiniMaxAlgorithm;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;
import com.Tak.AI.utils.ActionGenerator;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * MinimaxAgent represents an AI player using the Minimax algorithm for decision-making.
 */
public class MinimaxAgent extends Player implements Serializable {
    private static final long serialVersionUID = 1L;

    // Use the interface type here instead of the concrete class
    private IEvaluationFunction evaluationFunction;
    private MiniMaxAlgorithm minimaxAlgorithm;
    private int maxDepth;

    /**
     * Constructs a MinimaxAgent with specified parameters and move ordering option.
     *
     * @param color            The color of the player.
     * @param flatStones       Number of flat stones.
     * @param standingStones   Number of standing stones.
     * @param capstones        Number of capstones.
     * @param maxDepth         The maximum depth for the Minimax search.
     * @param useMoveOrdering  Flag to enable or disable move ordering.
     * @param evaluator        The evaluation function to use (e.g., NeuralNetworkEvaluator).
     */
    public MinimaxAgent(Color color, int flatStones, int standingStones,
                        int capstones, int maxDepth, boolean useMoveOrdering,
                        IEvaluationFunction evaluator) {
        super(color, flatStones, capstones);

        // Initialize with the provided evaluation function
        this.evaluationFunction = evaluator;
        this.maxDepth = maxDepth;
        this.minimaxAlgorithm = new MiniMaxAlgorithm(
            this.evaluationFunction,
            maxDepth,
            this,
            useMoveOrdering
        );
    }

    /**
     * Alternative constructor with move ordering enabled by default.
     *
     * @param color            The color of the player.
     * @param flatStones       Number of flat stones.
     * @param standingStones   Number of standing stones.
     * @param capstones        Number of capstones.
     * @param maxDepth         The maximum depth for the Minimax search.
     * @param evaluator        The evaluation function to use.
     */
    public MinimaxAgent(Color color, int flatStones, int standingStones,
                        int capstones, int maxDepth,
                        IEvaluationFunction evaluator) {
        this(color, flatStones, standingStones, capstones, maxDepth, true, evaluator);
    }

    /**
     * Original constructor retained for backward compatibility.
     * Uses a default EvaluationFunction (heuristic-based).
     *
     * @param color           The color of the player.
     * @param flatStones      Number of flat stones.
     * @param standingStones  Number of standing stones.
     * @param capstones       Number of capstones.
     * @param maxDepth        The maximum depth for the Minimax search.
     * @param useMoveOrdering Flag to enable or disable move ordering.
     */
    public MinimaxAgent(Color color, int flatStones, int standingStones,
                        int capstones, int maxDepth, boolean useMoveOrdering) {
        this(color, flatStones, standingStones, capstones, maxDepth, useMoveOrdering,
             new EvaluationFunction());
    }

    /**
     * Alternative constructor with move ordering enabled by default.
     * Uses a default EvaluationFunction (heuristic-based).
     *
     * @param color           The color of the player.
     * @param flatStones      Number of flat stones.
     * @param standingStones  Number of standing stones.
     * @param capstones       Number of capstones.
     * @param maxDepth        The maximum depth for the Minimax search.
     */
    public MinimaxAgent(Color color, int flatStones, int standingStones,
                        int capstones, int maxDepth) {
        this(color, flatStones, standingStones, capstones, maxDepth, true,
             new EvaluationFunction());
    }

    /**
     * Executes the AI player's move.
     */
    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        Board board = game.getBoard();

        // Example: if it's the AI's very first move (or if you want first two moves, adjust condition)
        if (game.getMoveCount() < 2) {
            // Force the worst possible move
            Action worstMove = findWorstMove(board, this, game.getMoveCount());
            if (worstMove != null) {
                worstMove.execute(board);
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
            } else {
                throw new InvalidMoveException("No valid moves available for the AI's first move.");
            }
        } else {
            // Normal Minimax logic for subsequent moves
        Action bestMove = minimaxAlgorithm.findBestMove(board, this, game.getMoveCount());
        if (bestMove != null) {
            bestMove.execute(board);
            game.incrementMoveCount();
            game.checkWinConditions();
            game.switchPlayer();
        } else {
            throw new InvalidMoveException("No valid moves available.");
        }
        }
    }

    /**
     * Helper method to pick the "worst" move (lowest-evaluated) from all possible actions.
     */
    private Action findWorstMove(Board board, Player player, int currentMoveCount) {

        Player opponent = player.getOpponent();
        // Generate possible action strings
        List<String> possibleActionStrings = ActionGenerator.generatePossibleActions(board, opponent, currentMoveCount);
        // Convert them to Action objects
        List<Action> possibleActions = minimaxAlgorithm.parseActionStrings(possibleActionStrings, opponent);

        Action worstAction = null;
        double worstValue = Double.POSITIVE_INFINITY;

        for (Action action : possibleActions) {
            Board boardCopy = board.copy();
            try {
                action.execute(boardCopy);
                // Evaluate the resulting board
                double moveValue = minimaxAlgorithm.getEvalFunction().evaluate(boardCopy, this);

                if (moveValue < worstValue || worstAction == null) {
                    worstValue = moveValue;
                    worstAction = action;
                }
            } catch (InvalidMoveException e) {
                // Skip invalid
            }
        }
        return worstAction;
    }

    @Override
    public Player copy() {
        return new MinimaxAgent(
                this.getColor(),
                this.getRemainingPieces(PieceType.FLAT_STONE),
                this.getRemainingPieces(PieceType.STANDING_STONE),
                this.getRemainingPieces(PieceType.CAPSTONE),
                this.maxDepth,
                this.minimaxAlgorithm.isUseMoveOrdering(),
                this.evaluationFunction
        );
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof MinimaxAgent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), "MinimaxAgent");
    }

    @Override
    public int getTotalPiecesLeft() {
        return getRemainingPieces(PieceType.FLAT_STONE)
             + getRemainingPieces(PieceType.STANDING_STONE)
             + getRemainingPieces(PieceType.CAPSTONE);
    }

    public int getMaxDepth() {
        return this.maxDepth;
    }

    /**
     * Retrieves the number of nodes evaluated during the last move.
     */
    public int getNodesEvaluated() {
        return this.minimaxAlgorithm.getNodesEvaluated();
    }

    /**
     * Retrieves the time taken (in milliseconds) during the last move.
     */
    public long getTimeTakenMillis() {
        return this.minimaxAlgorithm.getTimeTakenMillis();
    }

    /**
     * Retrieves the number of alpha-beta pruning events during the last move.
     */
    public int getPruneCount() {
        return this.minimaxAlgorithm.getPruneCount();
    }
}
