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
import com.Tak.Logic.utils.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Implements the Minimax algorithm with Alpha-Beta Pruning for decision-making in the TAK game.
 */
public class MiniMaxAlgorithm2 implements Serializable {
    private static final long serialVersionUID = 1L;

    private EvaluationFunction evalFunction;
    private final int maxDepth;
    private final Player aiPlayer;
    private Map<String, Double> transpositionTable;
    private final long timeLimitMillis = 2000;
    private long startTime;

    /**
     * Constructs a MinimaxAlgorithm with the specified evaluation function, maximum depth, and AI player.
     *
     * @param evalFunction The evaluation function to assess board states.
     * @param maxDepth     The maximum search depth.
     * @param aiPlayer     The AIPlayer instance.
     */
    public MiniMaxAlgorithm2(EvaluationFunction evalFunction, int maxDepth, Player aiPlayer) {
        this.evalFunction = evalFunction;
        this.maxDepth = maxDepth;
        this.aiPlayer = aiPlayer;
        this.transpositionTable = new HashMap<>();
    }

    /**
     * Finds the best move for the AIPlayer using the Minimax algorithm with Alpha-Beta Pruning.
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
    
        //Logger.log("MinimaxAlgorithm", "Starting search from the root (depth 0)");
    
        // Generate all possible moves from the current state at depth 0
        List<String> possibleActionStrings = ActionGenerator.generatePossibleActions(board, player, currentMoveCount);
        List<Action> possibleActions = parseActionStrings(possibleActionStrings, player);
        MoveOrderingHeuristics.orderMoves(possibleActions, board, player, evalFunction);
    
        // Initialize variables to track time limit and best move
        boolean timeLimitExceeded = false;
    
        for (Action action : possibleActions) {
            if (System.currentTimeMillis() - startTime > timeLimitMillis) {
                //Logger.log("MinimaxAlgorithm", "Time limit exceeded before evaluating all moves at depth 0");
                timeLimitExceeded = true;
                break;
            }
    
            Board boardAfterAction = board.copy();
            try {
                action.execute(boardAfterAction);
                double moveValue = minimax(boardAfterAction, maxDepth - 1, false, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, player, currentMoveCount + 1);
    
                //Logger.log("MinimaxAlgorithm", "Depth 0 | Evaluated move: " + action + " | Score: " + moveValue);
    
                if (moveValue > bestValue || bestAction == null) {
                    bestValue = moveValue;
                    bestAction = action;
                }
            } catch (InvalidMoveException e) {
                //Logger.log("MinimaxAlgorithm", "Invalid move encountered at depth 0: " + action);
                continue;
            } catch (InterruptedException e) {
                //Logger.log("MinimaxAlgorithm", "Time limit exceeded during minimax.");
                timeLimitExceeded = true;
                break;
            }
        }
    
        if (bestAction == null) {
            // If no best action was found, select a fallback move
            //Logger.log("MinimaxAlgorithm", "No valid moves found within time limit. Selecting a random move.");
            if (!possibleActions.isEmpty()) {
                bestAction = possibleActions.get(0); // Choose the first available move
            } else {
                throw new IllegalStateException("No valid moves available.");
            }
        }
    
        //Logger.log("MinimaxAlgorithm", "Final best move at depth 0: " + bestAction + " | Score: " + bestValue);
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

        if (depth == 0 || board.isFull() || evalFunction.evaluate(board, aiPlayer) == Double.POSITIVE_INFINITY || evalFunction.evaluate(board, aiPlayer) == Double.NEGATIVE_INFINITY) {
            double eval = evalFunction.evaluate(board, aiPlayer);
            //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Leaf node evaluated with score: " + eval);
            return eval;
        }

        //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Evaluating " + (isMaximizing ? "maximizing" : "minimizing") + " player.");

        Player currentPlayer = isMaximizing ? player : player.getOpponent();
        List<String> possibleActionStrings = ActionGenerator.generatePossibleActions(board, currentPlayer, moveCount);
        List<Action> possibleActions = parseActionStrings(possibleActionStrings, currentPlayer);
        MoveOrderingHeuristics.orderMoves(possibleActions, board, currentPlayer, evalFunction);

        if (isMaximizing) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Action action : possibleActions) {
                Board boardAfterAction = board.copy();
                try {
                    action.execute(boardAfterAction);
                    double eval = minimax(boardAfterAction, depth - 1, false, alpha, beta, player, moveCount + 1);
                    maxEval = Math.max(maxEval, eval);
                    alpha = Math.max(alpha, eval);

                    //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Maximizing move: " + action + " | Score: " + eval);

                    if (beta <= alpha) {
                        //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Pruning occurs (alpha: " + alpha + ", beta: " + beta + ")");
                        break;
                    }
                } catch (InvalidMoveException e) {
                    //Logger.log("MinimaxAlgorithm", "Invalid move encountered at depth " + (maxDepth - depth) + ": " + action);
                    continue;
                }
            }
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (Action action : possibleActions) {
                Board boardAfterAction = board.copy();
                try {
                    action.execute(boardAfterAction);
                    double eval = minimax(boardAfterAction, depth - 1, true, alpha, beta, player, moveCount + 1);
                    minEval = Math.min(minEval, eval);
                    beta = Math.min(beta, eval);

                    //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Minimizing move: " + action + " | Score: " + eval);

                    if (beta <= alpha) {
                        //Logger.log("MinimaxAlgorithm", "Depth " + (maxDepth - depth) + " | Pruning occurs (alpha: " + alpha + ", beta: " + beta + ")");
                        break;
                    }
                } catch (InvalidMoveException e) {
                    continue;
                }
            }
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
