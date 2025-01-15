// File: com/Tak/AI/experiments/NeuralNetVsHeuristicExperiment.java
package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.EvaluationFunction; // This is your heuristic-based evaluation
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.BoardToInputsConverter;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.util.List;

import org.lwjgl.system.CallbackI.B;

/**
 * Compares two MinimaxAgents: one using a neural network (NeuralNetworkEvaluator),
 * the other using a traditional heuristic (EvaluationFunction).
 */
public class NeuralNetVsHeuristicExperiment {

    public static class ExperimentStats {
        int gamesPlayed = 0;
        int neuralNetWins = 0;
        int heuristicWins = 0;
        int draws = 0;
        long totalNodesNN = 0L;       // Sum of nodes evaluated by the NN-based agent
        long totalNodesHeuristic = 0L;
        long totalTimeNN = 0L;
        long totalTimeHeuristic = 0L;
        int totalPrunesNN = 0;
        int totalPrunesHeuristic = 0;

        public void addResult(Player winner,
                              MinimaxAgent neuralNetAgent,
                              MinimaxAgent heuristicAgent) {
            gamesPlayed++;

            // Who won?
            if (winner == null) {
                draws++;
            } else if (winner.equals(neuralNetAgent)) {
                neuralNetWins++;
            } else if (winner.equals(heuristicAgent)) {
                heuristicWins++;
            }

            // Collect metrics
            totalNodesNN += neuralNetAgent.getNodesEvaluated();
            totalNodesHeuristic += heuristicAgent.getNodesEvaluated();
            totalTimeNN += neuralNetAgent.getTimeTakenMillis();
            totalTimeHeuristic += heuristicAgent.getTimeTakenMillis();
            totalPrunesNN += neuralNetAgent.getPruneCount();
            totalPrunesHeuristic += heuristicAgent.getPruneCount();
        }

        @Override
        public String toString() {
            double avgNodesNN = (gamesPlayed > 0) ? (double) totalNodesNN / gamesPlayed : 0;
            double avgNodesHeuristic = (gamesPlayed > 0) ? (double) totalNodesHeuristic / gamesPlayed : 0;
            double avgTimeNN = (gamesPlayed > 0) ? (double) totalTimeNN / gamesPlayed : 0;
            double avgTimeHeuristic = (gamesPlayed > 0) ? (double) totalTimeHeuristic / gamesPlayed : 0;
            double avgPrunesNN = (gamesPlayed > 0) ? (double) totalPrunesNN / gamesPlayed : 0;
            double avgPrunesHeuristic = (gamesPlayed > 0) ? (double) totalPrunesHeuristic / gamesPlayed : 0;

            return String.format(
                "Games: %d\n" +
                "NeuralNetAgent Wins: %d | HeuristicAgent Wins: %d | Draws: %d\n" +
                "Avg Nodes (NN): %.2f | Avg Nodes (Heuristic): %.2f\n" +
                "Avg Time (NN ms): %.2f | Avg Time (Heuristic ms): %.2f\n" +
                "Avg Prunes (NN): %.2f | Avg Prunes (Heuristic): %.2f\n",
                gamesPlayed, neuralNetWins, heuristicWins, draws,
                avgNodesNN, avgNodesHeuristic,
                avgTimeNN, avgTimeHeuristic,
                avgPrunesNN, avgPrunesHeuristic
            );
        }
    }

    public void run() {
        // 1) Initialize your neural network
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();

        // 2) Create the two different evaluation functions
        IEvaluationFunction neuralNetEval = new NeuralNetworkEvaluator(nnTrainer.getNetwork());
        IEvaluationFunction heuristicEval = new EvaluationFunction(); // your existing heuristic

        // 3) Settings for the experiment
        int boardSize = 5;
        int numberOfGames = 5;
        boolean useMoveOrdering = false;
        int minimaxDepth = 3; // both agents, or you can vary them separately

        ExperimentStats stats = new ExperimentStats();

        for (int g = 0; g < numberOfGames; g++) {
            // Agent 1: Neural Net
            MinimaxAgent neuralNetAgent = new MinimaxAgent(
                Color.GREEN, 21, 1, 1,
                minimaxDepth, useMoveOrdering,
                neuralNetEval
            );
            // Agent 2: Heuristic
            MinimaxAgent heuristicAgent = new MinimaxAgent(
                Color.BLUE, 21, 1, 1,
                minimaxDepth, useMoveOrdering,
                heuristicEval
            );

            // Set opponents
            neuralNetAgent.setOpponent(heuristicAgent);
            heuristicAgent.setOpponent(neuralNetAgent);

            // Build a new game (Neural Net agent moves first)
            TakGame game = new TakGame(boardSize, List.of(neuralNetAgent, heuristicAgent));

            // Run
            while (!game.isGameEnded()) {
                Player current = game.getCurrentPlayer();
                try {
                    current.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    break;
                }
            }

            // Collect results
            Player winner = game.getWinner();
            stats.addResult(winner, neuralNetAgent, heuristicAgent);
            //Print the state of the Board after each game
            game.getBoard().printBoard();
        }

        // Print final stats
        System.out.println("=== NeuralNet vs Heuristic Experiment ===");
        System.out.println(stats);
    }

    public static void main(String[] args) {
        new NeuralNetVsHeuristicExperiment().run();
    }
}
