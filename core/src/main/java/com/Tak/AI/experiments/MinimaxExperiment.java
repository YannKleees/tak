package com.Tak.AI.experiments;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.EvaluationFunction;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.search.MinimaxAlgorithm;
import com.Tak.Logic.models.*;

import java.util.*;

public class MinimaxExperiment {
    private final MinimaxAlgorithm minimaxAlgorithm;
    private final EvaluationFunction evaluationFunction;
    private final Player GREENPlayer;
    private final Player BLUEPlayer;

    public MinimaxExperiment() {
        this.GREENPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3); // Example piece counts
        this.BLUEPlayer = new MinimaxAgent(Player.Color.BLUE, 21, 1, 1, 3); // Example piece counts
        this.GREENPlayer.setOpponent(this.BLUEPlayer);
        this.BLUEPlayer.setOpponent(this.GREENPlayer);
        this.evaluationFunction = new EvaluationFunction();
        this.minimaxAlgorithm = new MinimaxAlgorithm(this.evaluationFunction, 5, this.GREENPlayer);
    }

    public List<Results.DepthExperimentResult> runDepthAnalysis(int startDepth, int endDepth, int gamesPerDepth) {
        List<Results.DepthExperimentResult> depthExperimentResults = new ArrayList<>();

        for (int depth = startDepth; depth <= endDepth; depth++) {
            System.out.println("Running experiments on depth " + depth);

            List<Results.GameResult> gameResults = new ArrayList<>();
            for (int game = 0; game < gamesPerDepth; game++) {
                Results.GameResult gameResult = playGameAtDepth(depth);
                gameResults.add(gameResult);
            }

            depthExperimentResults.add(new Results.DepthExperimentResult(depth, gameResults));
        }
        return depthExperimentResults;
    }

    public List<Results.OpeningExperimentResult> runPositionAnalysis(int searchDepth) {
        List<Results.OpeningExperimentResult> openingExperimentResults = new ArrayList<>();
        List<Board> openingPositions = generateOpeningPositions();

        for (Board boardPosition : openingPositions) {
            long startTime = System.nanoTime();
            Action chosenMove = minimaxAlgorithm.findBestMove(boardPosition, GREENPlayer, searchDepth);
            long endTime = System.nanoTime();

            openingExperimentResults.add(new Results.OpeningExperimentResult(
                boardPosition,
                chosenMove,
                (endTime - startTime) / 1_000_000.0,
                evaluationFunction.evaluate(boardPosition, GREENPlayer)
            ));
        }
        return openingExperimentResults;
    }

    /*public List<Results.TimeExperimentResult> runTimeAnalysis(List<Long> timeRestrictions, int gamesPerRestriction) {
        List<Results.TimeExperimentResult> timeExperimentResults = new ArrayList<>();

        for (long timeRestriction : timeRestrictions) {
            List<Results.GameResult> gameResults = new ArrayList<>();

            for (int game = 0; game < gamesPerRestriction; game++) {
                Results.GameResult gameResult = playGameWithTimeConstraint(timeRestriction);
                if (gameResult != null) {
                    gameResults.add(gameResult);
                }
            }

            timeExperimentResults.add(new Results.TimeExperimentResult(timeRestriction, gameResults));
        }
        return timeExperimentResults;
    }
    */

    public List<Results.BranchingExperimentResult> runBranchingAnalysis(int depth) {
        List<Results.BranchingExperimentResult> branchingExperimentResults = new ArrayList<>();
        List<Board> testingPositions = generateTestPositions();

        for (Board boardPosition : testingPositions) {
            int branchingFactor = countPossibleMoves(boardPosition);
            long startTime = System.nanoTime();
            Action bestMove = minimaxAlgorithm.findBestMove(boardPosition, GREENPlayer, depth);
            long endTime = System.nanoTime();

            branchingExperimentResults.add(new Results.BranchingExperimentResult(
                branchingFactor,
                (endTime - startTime) / 1_000_000.0,
                boardPosition,
                bestMove
            ));
        }
        return branchingExperimentResults;
    }

    private Results.GameResult playGameAtDepth(int depth) {
        Board boardPosition = new Board(5, 2); // Assuming a 5x5 board
        Player GREENPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 1, 1, 3); // Example piece counts
        long startTime = System.nanoTime();
        Action move = minimaxAlgorithm.findBestMove(boardPosition, GREENPlayer, depth);
        long endTime = System.nanoTime();

        return new Results.GameResult(
            depth,
            (endTime - startTime) / 1_000_000.0,
            evaluationFunction.evaluate(boardPosition, GREENPlayer),
            estimateNodesExplored(depth)
        );
    }

    /*private Results.GameResult playGameWithTimeConstraint(long timeConstraint) {
        // Implement a time-constrained game simulation
        Board board = new Board(5); // Placeholder board setup
        long startTime = System.nanoTime();
        // Implement minimax with time constraint
        Action move = minimaxAlgorithm.findBestMoveWithTimeLimit(board, GREENPlayer, timeConstraint);
        long endTime = System.nanoTime();

        return new Results.GameResult(
            -1, // Depth unknown in time-constrained games
            (endTime - startTime) / 1_000_000.0,
            evaluationFunction.evaluate(board, Player.Color.GREEN),
            estimateNodesExploredBasedOnTime((endTime - startTime) / 1_000_000.0)
        );
    }
    */

    private List<Board> generateOpeningPositions() {
        // Generate and return various opening positions
        return new ArrayList<>(); // Placeholder
    }

    private List<Board> generateTestPositions() {
        // Generate and return test positions with varying complexity
        return new ArrayList<>(); // Placeholder
    }

    private int countPossibleMoves(Board position) {
        // Calculate and return the number of legal moves for the given position
        return 20; // Example: placeholder value
    }

    private int estimateNodesExplored(int depth) {
        // Estimate nodes explored based on an average branching factor
        int avgBranchingFactor = 20;
        return (int) Math.pow(avgBranchingFactor, depth);
    }

    private int estimateNodesExploredBasedOnTime(double elapsedTimeMs) {
        // Estimate nodes explored based on time spent and average evaluation speed
        int avgNodesPerMs = 1000; // Placeholder value
        return (int) (elapsedTimeMs * avgNodesPerMs);
    }
}
