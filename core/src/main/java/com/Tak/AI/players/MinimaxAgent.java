package com.Tak.AI.players;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.AI.utils.MiniMaxAlgorithm;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece.PieceType;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * MinimaxAgent represents an AI player using the Minimax algorithm for decision-making.
 */
public class MinimaxAgent extends Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private IEvaluationFunction evaluationFunction;
    private MiniMaxAlgorithm minimaxAlgorithm;
    private int maxDepth;

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

    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth,
                        IEvaluationFunction evaluator) {
        this(color, flatStones, standingStones, capstones, maxDepth, true, evaluator);
    }

    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth,
                        boolean useMoveOrdering) {
        this(color, flatStones, standingStones, capstones, maxDepth, useMoveOrdering, new HeuristicEvaluator());
    }

    public MinimaxAgent(Color color,
                        int flatStones,
                        int standingStones,
                        int capstones,
                        int maxDepth) {
        this(color, flatStones, standingStones, capstones, maxDepth, true, new HeuristicEvaluator());
    }

    @Override
    public void makeMove(TakGame game) throws InvalidMoveException, GameOverException {
        Board board = game.getBoard();
        int currentCount = game.getMoveCount();

        // 5% chance to do a random move for added diversity:
        if (Math.random() < 0.05) {
            List<String> possibleMoves = ActionGenerator.generatePossibleActions(board, this, currentCount);
            if (!possibleMoves.isEmpty()) {
                System.out.println("MinimaxAgent: random move for diversity!");
                String chosen = possibleMoves.get(new Random().nextInt(possibleMoves.size()));
                Action randomAction = Action.fromString(chosen, this.getColor());
                randomAction.execute(board);
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
                return;
            }
            // If no possible random moves, just proceed to minimax
        }

        // Standard Minimax if we skip or fail random moves:
        Action bestMove = minimaxAlgorithm.findBestMove(board, this, currentCount);
        if (bestMove != null) {
            bestMove.execute(board);
            game.incrementMoveCount();
            game.checkWinConditions();
            game.switchPlayer();
        } else {
            throw new InvalidMoveException("No valid moves from Minimax.");
        }
    }

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

    public int getNodesEvaluated() {
        return minimaxAlgorithm.getNodesEvaluated();
    }

    public long getTimeTakenMillis() {
        return minimaxAlgorithm.getTimeTakenMillis();
    }

    public int getPruneCount() {
        return minimaxAlgorithm.getPruneCount();
    }

    public IEvaluationFunction getEvaluationFunction() {
        return this.evaluationFunction;
    }
}
