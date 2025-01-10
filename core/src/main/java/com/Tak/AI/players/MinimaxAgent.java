package com.Tak.AI.players;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.search.MiniMaxAlgorithm;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * MinimaxAgent represents an AI player using the Minimax algorithm for decision-making.
 * It also handles special forced-move logic for the first two moves in Tak.
 */
public class MinimaxAgent extends Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private IEvaluationFunction evaluationFunction;
    private MiniMaxAlgorithm minimaxAlgorithm;
    private int maxDepth;

    /**
     * Full constructor allowing a custom evaluation function and move ordering setting.
     */
    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth,
                        boolean useMoveOrdering,
                        IEvaluationFunction evaluator) {
        super(color, flatStones, capstones);
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
     * Constructor with default 'useMoveOrdering = true' and a custom evaluator.
     */
    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth,
                        IEvaluationFunction evaluator) {
        this(color, flatStones, standingStones, capstones, maxDepth, true, evaluator);
    }

    /**
     * Constructor allowing toggling move ordering, but using a default (heuristic) EvaluationFunction.
     */
    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth,
                        boolean useMoveOrdering) {
        this(color, flatStones, standingStones, capstones, maxDepth, useMoveOrdering, new EvaluationFunction());
    }

    /**
     * Constructor with a default heuristic evaluator and move ordering = true.
     */
    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth) {
        this(color, flatStones, standingStones, capstones, maxDepth, true, new EvaluationFunction());
    }

    /**
     * Executes the AI player's move.
     * Implements special forced logic for the first two moves of the game:
     *   - Move #0 if this agent is BLUE => force the worst move for GREEN
     *   - Move #1 if this agent is GREEN => force the worst move for BLUE
     * Otherwise, do normal Minimax.
     */
    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        Board board = game.getBoard();
        int currentCount = game.getMoveCount();
        Color myColor = this.getColor();

        // Check if we are in the first 2 moves
        if (currentCount == 0 && myColor == Color.BLUE) {
            // It's the very first move of the game, current agent is BLUE
            // => Force the worst possible move for GREEN
            Player forcedColor = this.getOpponent(); // should be GREEN
            Action worstMove = findWorstMove(board, forcedColor, currentCount);
            if (worstMove != null) {
                worstMove.execute(board);
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
            } else {
                throw new InvalidMoveException("No valid forced moves available (first move).");
            }

        } else if (currentCount == 1 && myColor == Color.GREEN) {
            // It's the second move of the game, current agent is GREEN
            // => Force the worst possible move for BLUE
            Player forcedColor = this.getOpponent(); // should be BLUE
            Action worstMove = findWorstMove(board, forcedColor, currentCount);
            if (worstMove != null) {
                worstMove.execute(board);
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
            } else {
                throw new InvalidMoveException("No valid forced moves available (second move).");
            }

        } else {
            // Normal Minimax logic for subsequent moves
            Action bestMove = minimaxAlgorithm.findBestMove(board, this, currentCount);
            if (bestMove != null) {
                bestMove.execute(board);
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
            } else {
                throw new InvalidMoveException("No valid moves available from Minimax.");
            }
        }
    }

    /**
     * Helper method to pick the "worst" move for a specific color (forcedColor).
     * This enumerates forcedColor's possible actions, executes each on a copy,
     * and picks the one with the lowest evaluation (from forcedColor's perspective).
     */
    private Action findWorstMove(Board board, Player forcedColor, int currentMoveCount) {
        // Enumerate forcedColor's possible actions
        List<String> possibleActionStrings = ActionGenerator.generatePossibleActions(board, forcedColor, currentMoveCount);
        List<Action> possibleActions = minimaxAlgorithm.parseActionStrings(possibleActionStrings, forcedColor);

        Action worstAction = null;
        double worstValue = Double.POSITIVE_INFINITY;

        // Evaluate from forcedColor's perspective.
        for (Action action : possibleActions) {
            Board boardCopy = board.copy();
            try {
                action.execute(boardCopy);
                double moveValue = minimaxAlgorithm.getEvalFunction().evaluate(boardCopy, forcedColor);

                if (moveValue < worstValue || worstAction == null) {
                    worstValue = moveValue;
                    worstAction = action;
                }
            } catch (InvalidMoveException e) {
                // skip invalid
            }
        }
        return worstAction;
    }

    /**
     * Creates a copy of this agent, preserving color, piece counts, depth, etc.
     */
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
