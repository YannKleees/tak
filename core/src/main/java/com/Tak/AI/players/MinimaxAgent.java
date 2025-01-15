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
 * It applies symmetrical "forced worst move" logic for the first two moves of the game:
 *   - If moveCount < 2, this agent forces the opponent to make their worst possible move.
 *   - Otherwise, it performs normal Minimax on its own behalf.
 */
public class MinimaxAgent extends Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private IEvaluationFunction evaluationFunction;
    private MiniMaxAlgorithm minimaxAlgorithm;
    private int maxDepth;

    /**
     * Constructor allowing a custom evaluation function and move ordering setting.
     *
     * @param color            The color of this agent (BLUE or GREEN).
     * @param flatStones       Number of flat stones for this agent.
     * @param standingStones   Number of standing stones for this agent.
     * @param capstones        Number of capstones for this agent.
     * @param maxDepth         The maximum Minimax search depth.
     * @param useMoveOrdering  Enable or disable move ordering heuristics.
     * @param evaluator        The evaluation function (heuristic, neural net, etc.).
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
     * Constructor with default 'useMoveOrdering = true' and a custom evaluation function.
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
     * Constructor allowing toggling move ordering, but using the default heuristic-based evaluator.
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
     * Constructor with a default heuristic evaluator and move ordering enabled.
     */
    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth) {
        this(color, flatStones, standingStones, capstones, maxDepth, true, new EvaluationFunction());
    }

    /**
     * Executes this AI player's move.
     * If it's among the first two moves (moveCount < 2), forces the opponent to do a "worst" move.
     * After that, uses normal Minimax to select this agent's own best move.
     */
    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        Board board = game.getBoard();
        int currentCount = game.getMoveCount();

        // For the first two moves, force the *opponent* to perform a "worst" move from their perspective
        if (currentCount < 2) {
            Player forcedColor = this.getOpponent();
            Action worstMove = findWorstMove(board, forcedColor, currentCount);
            if (worstMove != null) {
                worstMove.execute(board);
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
            } else {
                throw new InvalidMoveException("No valid forced moves available in the first two moves.");
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
     * Finds the "worst" move for the specified forcedColor player.
     * We enumerate forcedColor's possible moves, simulate each, and pick the one
     * that yields the lowest evaluation (from forcedColor's own perspective).
     */
    private Action findWorstMove(Board board, Player forcedColor, int currentMoveCount) {
        // Generate all possible moves for forcedColor
        List<String> possibleMoves =
            ActionGenerator.generatePossibleActions(board, forcedColor, currentMoveCount);

        // Parse them into actual Action objects
        List<Action> actions = minimaxAlgorithm.parseActionStrings(possibleMoves, forcedColor);

        Action worstAction = null;
        double worstValue = Double.POSITIVE_INFINITY;

        for (Action action : actions) {
            Board boardCopy = board.copy();
            try {
                action.execute(boardCopy);
                // Evaluate from forcedColor's perspective => the lower the score, the "worse" it is for forcedColor
                double value = minimaxAlgorithm.getEvalFunction().evaluate(boardCopy, forcedColor);
                if (value < worstValue) {
                    worstValue = value;
                    worstAction = action;
                }
            } catch (InvalidMoveException e) {
                // Skip invalid
            }
        }
        return worstAction;
    }

    /**
     * Creates a copy of this MinimaxAgent with the same color, piece counts, max depth, etc.
     */
    @Override
    public Player copy() {
        return new MinimaxAgent(
            this.getColor(),
            this.getRemainingPieces(PieceType.FLAT_STONE),
            this.getRemainingPieces(PieceType.STANDING_STONE),
            this.getRemainingPieces(PieceType.CAPSTONE),
            this.maxDepth,
            minimaxAlgorithm.isUseMoveOrdering(),
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
     * @return the number of nodes evaluated during the last move (for debugging or display).
     */
    public int getNodesEvaluated() {
        return minimaxAlgorithm.getNodesEvaluated();
    }

    /**
     * @return time taken (in milliseconds) during the last move (for debugging or display).
     */
    public long getTimeTakenMillis() {
        return minimaxAlgorithm.getTimeTakenMillis();
    }

    /**
     * @return how many times alpha-beta pruning occurred during the last move (debug).
     */
    public int getPruneCount() {
        return minimaxAlgorithm.getPruneCount();
    }
}
