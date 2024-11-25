package com.Tak.AI.experiments;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.AI.search.MinimaxAlgorithm;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to run experiments based on varying the depth of the Minimax tree.
 */
public class DepthExperiment {

    private final Board board;
    private final EvaluationFunction evaluationFunction;
    private final Player GREENPlayer;
    private final Player BLUEPlayer;
    private final int maxDepth;
    private final int initialMoveCount;

    /**
     * Constructor to initialize the DepthExperiment class.
     *
     * @param board              The game board.
     * @param evaluationFunction The evaluation function used by Minimax.
     * @param aiPlayer           The AI player for which the experiment is conducted.
     * @param opponentPlayer     The opponent player.
     * @param maxDepth           The maximum depth to test.
     * @param initialMoveCount   The starting move count of the game.
     */
    public DepthExperiment(Board board, EvaluationFunction evaluationFunction, Player aiPlayer, Player opponentPlayer, int maxDepth, int initialMoveCount) {
        this.board = board;
        this.evaluationFunction = evaluationFunction;
        this.GREENPlayer = aiPlayer;
        this.BLUEPlayer = opponentPlayer;
        this.maxDepth = maxDepth;
        this.initialMoveCount = initialMoveCount;
    }

    /**
     * Runs the depth experiment and collects results.
     *
     * @return A list of DepthExperimentResult objects containing the results of the experiment.
     */
    public List<DepthExperimentResult> runExperiment() {
        List<DepthExperimentResult> results = new ArrayList<>();

        for (int depth = 1; depth <= maxDepth; depth++) {
            Logger.log("DepthExperiment", "Running experiment at depth: " + depth);

            // Initialize the Minimax algorithm for the current depth.
            MinimaxAlgorithm minimax = new MinimaxAlgorithm(evaluationFunction, depth, GREENPlayer);

            // Measure the start time.
            long startTime = System.currentTimeMillis();

            // Find the best move using Minimax.
            Action bestAction = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            try {
                bestAction = minimax.findBestMove(board, GREENPlayer, initialMoveCount);
                if (bestAction != null) {
                    Board boardAfterAction = board.copy();
                    bestAction.execute(boardAfterAction);
                    bestScore = evaluationFunction.evaluate(boardAfterAction, GREENPlayer);
                }
            } catch (Exception e) {
                Logger.log("DepthExperiment", "Error occurred while evaluating depth " + depth + ": " + e.getMessage());
                e.printStackTrace();
            }

            // Measure the end time and calculate elapsed time.
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;

            // Log results and add them to the list.
            Logger.log("DepthExperiment", "Depth: " + depth + " | Best Action: " + bestAction + " | Score: " + bestScore + " | Time: " + timeTaken + "ms");

            results.add(new DepthExperimentResult(depth, bestAction, bestScore, timeTaken));
        }

        return results;
    }

    /**
     * Inner class to store the results of each depth experiment.
     */
    public static class DepthExperimentResult {
        private final int depth;
        private final Action bestAction;
        private final double score;
        private final long timeTaken;

        /**
         * Constructor for DepthExperimentResult.
         *
         * @param depth     The depth of the Minimax tree.
         * @param bestAction The best action found at this depth.
         * @param score     The evaluation score of the best action.
         * @param timeTaken The time taken to compute the best action.
         */
        public DepthExperimentResult(int depth, Action bestAction, double score, long timeTaken) {
            this.depth = depth;
            this.bestAction = bestAction;
            this.score = score;
            this.timeTaken = timeTaken;
        }

        public int getDepth() {
            return depth;
        }

        public Action getBestAction() {
            return bestAction;
        }

        public double getScore() {
            return score;
        }

        public long getTimeTaken() {
            return timeTaken;
        }

        @Override
        public String toString() {
            return "Depth: " + depth +
                ", Best Action: " + bestAction +
                ", Score: " + score +
                ", Time Taken: " + timeTaken + "ms";
        }
    }
}
