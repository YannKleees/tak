package com.Tak.AI.search;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.AI.utils.MoveOrderingHeuristics;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import com.Tak.Logic.utils.Logger;

/**
 * Implements the Minimax algorithm with Alpha-Beta Pruning and Iterative Deepening for decision-making in the TAK game.
 */
public class MinimaxAlgorithm implements Serializable {
    private static final long serialVersionUID = 1L;

    private EvaluationFunction evalFunction;
    private final int maxDepth;
    private final Player aiPlayer;
    private Map<String, Double> transpositionTable;
    private final long timeLimitMillis = 5000;
    private long startTime;
    private final int maxMovesToConsider = 10;

    /**
     * Constructs a MinimaxAlgorithm with the specified evaluation function, maximum depth, and AI player.
     *
     * @param evalFunction The evaluation function to assess board states.
     * @param maxDepth     The maximum search depth.
     * @param aiPlayer     The AIPlayer instance.
     */
    public MinimaxAlgorithm(EvaluationFunction evalFunction, int maxDepth, Player aiPlayer) {
        this.evalFunction = evalFunction;
        this.maxDepth = maxDepth;
        this.aiPlayer = aiPlayer;
        this.transpositionTable = new HashMap<>();
    }

    /**
     * Finds the best move for the AIPlayer using the Minimax algorithm with Iterative Deepening.
     *
     * @param board           The current game board.
     * @param player          The AIPlayer making the move.
     * @param currentMoveCount The current move count in the game.
     * @return The best Action to take, or null if no valid actions are available.
     */
    public Action findBestMove(Board board, Player player, int currentMoveCount) {
        Action bestAction = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        startTime = System.currentTimeMillis();

        for (int depth = 1; depth <= maxDepth; depth++) {
            try {
                //Logger.log("MinimaxAlgorithm", "Starting search at depth: " + depth);
                Action currentBestAction = null;
                double currentBestValue = Double.NEGATIVE_INFINITY;
                List<String> possibleActionStrings = ActionGenerator.generatePossibleActions(board, player, currentMoveCount);
                List<Action> possibleActions = parseActionStrings(possibleActionStrings, player);
                MoveOrderingHeuristics.orderMoves(possibleActions, board, player, evalFunction);

                if (possibleActions.size() > maxMovesToConsider) {
                    possibleActions = possibleActions.subList(0, maxMovesToConsider);
                }

                for (Action action : possibleActions) {
                    if (System.currentTimeMillis() - startTime > timeLimitMillis) {
                        //Logger.log("MinimaxAlgorithm", "Time limit exceeded at depth: " + depth);
                        throw new InterruptedException("Time limit exceeded during move evaluation.");
                    }

                    Board boardAfterAction = board.copy();
                    try {
                        action.execute(boardAfterAction);
                        double moveValue = minimax(boardAfterAction, depth - 1, false, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player, currentMoveCount + 1);

                        //Logger.log("MinimaxAlgorithm", "Depth " + depth + " | Evaluated move: " + action + " | Score: " + moveValue);

                        if (moveValue > currentBestValue) {
                            currentBestValue = moveValue;
                            currentBestAction = action;
                        }
                    } catch (InvalidMoveException e) {
                        //Logger.log("MinimaxAlgorithm", "Invalid move encountered: " + action);
                        continue;
                    }
                }

                if (currentBestAction != null) {
                    bestAction = currentBestAction;
                    bestValue = currentBestValue;
                    //Logger.log("MinimaxAlgorithm", "Depth " + depth + " | Best move so far: " + bestAction + " | Score: " + bestValue);
                }

                if (System.currentTimeMillis() - startTime > timeLimitMillis * 0.9) {
                    //Logger.log("MinimaxAlgorithm", "Ending search early at depth: " + depth + " due to time constraints.");
                    break;
                }

            } catch (InterruptedException e) {
                //Logger.log("MinimaxAlgorithm", "Search interrupted at depth: " + depth);
                break;
            }
            //Logger.log("MinimaxAlgorithm", "Final best move: " + bestAction + " | Score: " + bestValue);
        }

        return bestAction;
    }

    /**
     * The core Minimax recursive function with Alpha-Beta Pruning.
     *
     * @param board         The current game board.
     * @param depth         The remaining search depth.
     * @param isMaximizing  Indicates whether the current layer is maximizing or minimizing.
     * @param alpha         The best already explored option for the maximizer.
     * @param beta          The best already explored option for the minimizer.
     * @param player        The AIPlayer instance.
     * @param moveCount     The current move count in the game.
     * @return The evaluation score of the board state.
     */
    private double minimax(Board board, int depth, boolean isMaximizing, double alpha, double beta, Player player, int moveCount) throws InterruptedException {
        if (System.currentTimeMillis() - startTime > timeLimitMillis) {
            throw new InterruptedException("Time limit exceeded during minimax.");
        }

        String stateHash = generateStateHash(board, isMaximizing);

        if (transpositionTable.containsKey(stateHash)) {
            return transpositionTable.get(stateHash);
        }

        if (depth == 0 || board.isFull() || evalFunction.evaluate(board, aiPlayer) == Double.POSITIVE_INFINITY || evalFunction.evaluate(board, aiPlayer) == Double.NEGATIVE_INFINITY) {
            double eval = evalFunction.evaluate(board, aiPlayer);
            transpositionTable.put(stateHash, eval);
            return eval;
        }

        //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Evaluating " + (isMaximizing ? "maximizing" : "minimizing") + " player.");


        Player currentPlayer = isMaximizing ? player : player.getOpponent();
        List<String> possibleActionStrings = ActionGenerator.generatePossibleActions(board, currentPlayer, moveCount);
        List<Action> possibleActions = parseActionStrings(possibleActionStrings, currentPlayer);
        MoveOrderingHeuristics.orderMoves(possibleActions, board, currentPlayer, evalFunction);

        if (possibleActions.size() > maxMovesToConsider) {
            possibleActions = possibleActions.subList(0, maxMovesToConsider);
        }

        if (isMaximizing) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Action action : possibleActions) {
                Board boardAfterAction = board.copy();
                try {
                    action.execute(boardAfterAction);
                    double eval = minimax(boardAfterAction, depth - 1, false, alpha, beta, player, moveCount + 1);
                    //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Move: " + action + " | Score: " + eval);
                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, eval);
                    if (beta <= alpha) {
                        //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Pruning occurs (alpha: " + alpha + ", beta: " + beta + ")");
                        break;
                    }
                } catch (InvalidMoveException e) {
                    //Logger.log("MinimaxAlgorithm", "Invalid move encountered: " + action);
                    continue;
                }
            }
            transpositionTable.put(stateHash, maxEval);
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (Action action : possibleActions) {
                Board boardAfterAction = board.copy();
                try {
                    action.execute(boardAfterAction);
                    double eval = minimax(boardAfterAction, depth - 1, true, alpha, beta, player, moveCount + 1);
                    //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Move: " + action + " | Score " + eval);
                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, eval);
                    if (beta <= alpha) {
                        //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Pruning occurs (alpha: " + alpha + ", beta: " + beta + ")");
                        break;
                    }
                } catch (InvalidMoveException e) {
                    continue;
                }
            }
            transpositionTable.put(stateHash, minEval);
            return minEval;
        }
    }

    /**
     * Parses action strings into Action objects.
     *
     * @param actionStrings The list of action strings.
     * @param player        The player performing the actions.
     * @return A list of valid Action objects.
     */
    private List<Action> parseActionStrings(List<String> actionStrings, Player player) {
        List<Action> actions = new ArrayList<>();
        for (String actionStr : actionStrings) {
            try {
                Action action = Action.fromString(actionStr, player.getColor());
                actions.add(action);
            } catch (InvalidMoveException e) {
                continue;
            }
        }
        return actions;
    }

    /**
     * Generates a unique hash for the board state including player turn.
     *
     * @param board         The game board.
     * @param isMaximizing  Indicates if the current layer is maximizing.
     * @return A unique string representing the board state.
     */
    private String generateStateHash(Board board, boolean isMaximizing) {
        StringBuilder sb = new StringBuilder();
        sb.append(isMaximizing ? "Max_" : "Min_");

        for (int y = 0; y < board.getSize(); y++) {
            for (int x = 0; x < board.getSize(); x++) {
                PieceStack stack = board.getBoardStack(x, y);
                if (stack.isEmpty()) {
                    sb.append("E");
                } else {
                    Piece topPiece = stack.getTopPiece();
                    sb.append(topPiece.getPieceType().toString().charAt(0));
                    sb.append(topPiece.getOwner().getColor().toString().charAt(0));
                    sb.append(stack.size());
                }
                sb.append(",");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    /**
     * Clears the transposition table, useful when resetting the AI or starting a new game.
     */
    public void clearTranspositionTable() {
        transpositionTable.clear();
    }
}
