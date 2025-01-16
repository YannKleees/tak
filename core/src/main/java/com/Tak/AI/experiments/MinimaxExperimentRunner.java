// File: com/Tak/AI/experiments/NeuralNetVsHeuristicExperiment.java
package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator; // Heuristic-based evaluation
import com.Tak.AI.neuralnet.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer;
import com.Tak.AI.neuralnet.BoardToInputsConverter;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NeuralNetVsHeuristicExperiment compares two MinimaxAgents:
 * 1. HeuristicMinimaxAgent using a traditional heuristic evaluation.
 * 2. NeuralNetMinimaxAgent using a neural network-based evaluation.
 *
 * It collects training data from the games and trains the neural network accordingly.
 */
public class MinimaxExperimentRunner {

    /**
     * Inner class to hold experiment statistics.
     */
    public static class ExperimentStats {
        int gamesPlayed = 0;
        int neuralNetWins = 0;
        int heuristicWins = 0;
        int draws = 0;
        long totalNodesNN = 0L;       // Sum of nodes evaluated by the NN-based agent
        long totalNodesHeuristic = 0L; // Sum of nodes evaluated by the Heuristic agent
        long totalTimeNN = 0L;         // Total time taken by NN-based agent
        long totalTimeHeuristic = 0L;  // Total time taken by Heuristic agent
        int totalPrunesNN = 0;
        int totalPrunesHeuristic = 0;

        /**
         * Adds the result of a single game to the statistics.
         *
         * @param winner            The winning player, or null for a draw.
         * @param neuralNetAgent    The neural network-based agent.
         * @param heuristicAgent    The heuristic-based agent.
         */
        public void addResult(Player winner,
                              NeuralNetMinimaxAgent neuralNetAgent,
                              HeuristicMinimaxAgent heuristicAgent) {
            gamesPlayed++;

            // Determine the outcome
            if (winner == null) {
                draws++;
            } else if (winner.equals(neuralNetAgent)) {
                neuralNetWins++;
            } else if (winner.equals(heuristicAgent)) {
                heuristicWins++;
            }

            // Collect performance metrics
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
                "Games Played: %d\n" +
                "NeuralNetAgent Wins: %d (%.2f%%)\n" +
                "HeuristicAgent Wins: %d (%.2f%%)\n" +
                "Draws: %d (%.2f%%)\n\n" +
                "Average Nodes Evaluated per Game:\n" +
                " - NeuralNetAgent: %.2f\n" +
                " - HeuristicAgent: %.2f\n\n" +
                "Average Time Taken per Game (ms):\n" +
                " - NeuralNetAgent: %.2f\n" +
                " - HeuristicAgent: %.2f\n\n" +
                "Average Prunes per Game:\n" +
                " - NeuralNetAgent: %.2f\n" +
                " - HeuristicAgent: %.2f\n",
                gamesPlayed,
                neuralNetWins, (gamesPlayed > 0) ? (100.0 * neuralNetWins / gamesPlayed) : 0.0,
                heuristicWins, (gamesPlayed > 0) ? (100.0 * heuristicWins / gamesPlayed) : 0.0,
                draws, (gamesPlayed > 0) ? (100.0 * draws / gamesPlayed) : 0.0,
                avgNodesNN, avgNodesHeuristic,
                avgTimeNN, avgTimeHeuristic,
                avgPrunesNN, avgPrunesHeuristic
            );
        }
    }

    /**
     * Represents a single training example consisting of input features and target output.
     */
    public static class TrainingExample {
        private double[] input;
        private double[] target;

        public TrainingExample(double[] input, double[] target) {
            this.input = input;
            this.target = target;
        }

        public double[] getInput() {
            return input;
        }

        public double[] getTarget() {
            return target;
        }
    }

    /**
     * Runs the NeuralNet vs Heuristic experiment.
     */
    public void run() {
        // 1. Initialize the NeuralNetworkTrainer
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        nnTrainer.loadNetwork("trained_network.txt"); // Ensure a trained network exists

        // 2. Define experiment parameters
        int boardSize = 5;
        int maxDepth = 3; // Adjust based on computational resources
        int numberOfGames = 1000; // Number of games to run

        // 3. Create ExperimentStats instance
        ExperimentStats stats = new ExperimentStats();

        // 4. List to collect all training examples
        List<TrainingExample> trainingData = new ArrayList<>();

        // 5. Run the specified number of games
        for (int g = 1; g <= numberOfGames; g++) {
            // Initialize agents
            HeuristicMinimaxAgent heuristicAgent = new HeuristicMinimaxAgent(
                    Color.BLUE,
                    21, // Flat stones
                    21, // Standing stones
                    1,  // Capstones
                    maxDepth
            );

            NeuralNetMinimaxAgent neuralNetAgent = new NeuralNetMinimaxAgent(
                    Color.GREEN,
                    21,
                    21,
                    1,
                    maxDepth,
                    new NeuralNetworkEvaluator(nnTrainer.getNetwork()) // Neural network-based evaluation
            );

            // Initialize the game with the two players
            TakGame game = new TakGame(boardSize, Arrays.asList(neuralNetAgent, heuristicAgent));

            // Play the game
            while (!game.isGameEnded()) {
                try {
                    Player currentPlayer = game.getCurrentPlayer();
                    currentPlayer.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    // Game has ended due to a win or invalid move
                    break;
                }
            }

            // Determine the winner
            Player winner = game.getWinner();

            // Update statistics
            stats.addResult(winner, neuralNetAgent, heuristicAgent);

            // Collect training data from both agents
            // NeuralNetAgent's moves
            for (double[] boardState : neuralNetAgent.getMoveBoardStates()) {
                double[] target = new double[1];
                if (winner == neuralNetAgent) {
                    target[0] = 1.0; // Win
                } else if (winner == heuristicAgent) {
                    target[0] = -1.0; // Loss
                } else {
                    target[0] = 0.0; // Draw
                }
                trainingData.add(new TrainingExample(boardState, target));
            }

            // HeuristicAgent's moves
            for (double[] boardState : heuristicAgent.getMoveBoardStates()) {
                double[] target = new double[1];
                if (winner == heuristicAgent) {
                    target[0] = 1.0; // Win
                } else if (winner == neuralNetAgent) {
                    target[0] = -1.0; // Loss
                } else {
                    target[0] = 0.0; // Draw
                }
                trainingData.add(new TrainingExample(boardState, target));
            }

            // Optionally, print the board state after each game
            // game.getBoard().printBoard();

            // Progress update
            if (g % 100 == 0 || g == numberOfGames) {
                System.out.println("Completed " + g + " / " + numberOfGames + " games.");
            }
        }

        // 6. Train the neural network with the collected training data
        if (!trainingData.isEmpty()) {
            System.out.println("Training neural network with collected data...");
            List<SelfPlayMinimaxTrainer.TrainingExample> convertedTrainingData = new ArrayList<>();
            for (TrainingExample example : trainingData) {
                convertedTrainingData.add(new SelfPlayMinimaxTrainer.TrainingExample(example.getInput(), example.getTarget()));
            }
            nnTrainer.trainAll(convertedTrainingData);
            nnTrainer.saveNetwork("trained_network.txt");
            System.out.println("Neural network training completed and saved to trained_network.txt");
        }

        // 7. Display final statistics
        System.out.println("\n=== NeuralNet vs Heuristic Experiment Results ===");
        System.out.println(stats);
    }

    /**
     * Main method to execute the experiment.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        MinimaxExperimentRunner experiment = new MinimaxExperimentRunner();
        experiment.run();
    }
}
