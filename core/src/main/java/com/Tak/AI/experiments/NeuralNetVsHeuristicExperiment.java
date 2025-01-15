// File: com/Tak/AI/experiments/NeuralNetVsHeuristicExperiment.java
package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.EvaluationFunction; // Heuristic-based evaluation
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * NeuralNetVsHeuristicExperiment compares two MinimaxAgents:
 * 1. NeuralNetMinimaxAgent using a neural network-based evaluation.
 * 2. HeuristicMinimaxAgent using a traditional heuristic evaluation.
 *
 * It collects training data from the games, trains the neural network,
 * and monitors performance to retrain when the neural network outperforms the heuristic agent.
 */
public class NeuralNetVsHeuristicExperiment {

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
            double winRateNN = (gamesPlayed > 0) ? ((double) neuralNetWins / gamesPlayed) * 100.0 : 0.0;
            double winRateHeuristic = (gamesPlayed > 0) ? ((double) heuristicWins / gamesPlayed) * 100.0 : 0.0;
            double drawRate = (gamesPlayed > 0) ? ((double) draws / gamesPlayed) * 100.0 : 0.0;

            double avgNodesNN = (gamesPlayed > 0) ? ((double) totalNodesNN / gamesPlayed) : 0.0;
            double avgNodesHeuristic = (gamesPlayed > 0) ? ((double) totalNodesHeuristic / gamesPlayed) : 0.0;

            double avgTimeNN = (gamesPlayed > 0) ? ((double) totalTimeNN / gamesPlayed) : 0.0;
            double avgTimeHeuristic = (gamesPlayed > 0) ? ((double) totalTimeHeuristic / gamesPlayed) : 0.0;

            double avgPrunesNN = (gamesPlayed > 0) ? ((double) totalPrunesNN / gamesPlayed) : 0.0;
            double avgPrunesHeuristic = (gamesPlayed > 0) ? ((double) totalPrunesHeuristic / gamesPlayed) : 0.0;

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
                gamesPlayed, neuralNetWins, winRateNN,
                heuristicWins, winRateHeuristic,
                draws, drawRate,
                avgNodesNN, avgNodesHeuristic,
                avgTimeNN, avgTimeHeuristic,
                avgPrunesNN, avgPrunesHeuristic
            );
        }
    }

    private NeuralNetworkEvaluator neuralNetEvaluator;
    private EvaluationFunction heuristicEvaluator;
    private NeuralNetworkTrainer nnTrainer; // To allow retraining

    /**
     * Constructor to initialize the experiment with the selected neural network evaluator.
     *
     * @param neuralNetEvaluator The neural network-based evaluation function.
     * @param nnTrainer          The neural network trainer.
     */
    public NeuralNetVsHeuristicExperiment(NeuralNetworkEvaluator neuralNetEvaluator, NeuralNetworkTrainer nnTrainer) {
        this.neuralNetEvaluator = neuralNetEvaluator;
        this.heuristicEvaluator = new EvaluationFunction();
        this.nnTrainer = nnTrainer;
    }

    /**
     * Runs the experiment where the NeuralNetMinimaxAgent plays against the HeuristicMinimaxAgent.
     *
     * @throws IOException If an I/O error occurs during training.
     */
    public void run() throws IOException {
        // Settings for the experiment
        int boardSize = 5;
        int numberOfGames = promptForNumber("Enter the number of games to run in the experiment: ", 1, Integer.MAX_VALUE);
        int minimaxDepth = 3; // Adjust based on computational resources
        double performanceThreshold = 60.0; // Example threshold

        ExperimentStats stats = new ExperimentStats();
        List<NeuralNetVsHeuristicExperiment.TrainingExample> trainingData = new ArrayList<>();

        for (int g = 1; g <= numberOfGames; g++) {
            System.out.println("\n=== Game " + g + " of " + numberOfGames + " ===");

            // Initialize agents
            NeuralNetMinimaxAgent neuralNetAgent = new NeuralNetMinimaxAgent(
                    Color.GREEN,
                    21, // Flat stones
                    21, // Standing stones
                    1,  // Capstones
                    minimaxDepth,
                    neuralNetEvaluator
            );

            HeuristicMinimaxAgent heuristicAgent = new HeuristicMinimaxAgent(
                    Color.BLUE,
                    21, // Flat stones
                    21, // Standing stones
                    1,  // Capstones
                    minimaxDepth
            );

            // Set opponents
            neuralNetAgent.setOpponent(heuristicAgent);
            heuristicAgent.setOpponent(neuralNetAgent);

            // Initialize the game with the two players
            TakGame game = new TakGame(boardSize, List.of(neuralNetAgent, heuristicAgent));

            // Play the game
            while (!game.isGameEnded()) {
                Player currentPlayer = game.getCurrentPlayer();
                try {
                    currentPlayer.makeMove(game);
                    game.getBoard().printBoard(); // Print board state after each move
                } catch (InvalidMoveException | GameOverException e) {
                    System.out.println("Game ended prematurely: " + e.getMessage());
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

            // Progress update
            if (g % 10 == 0 || g == numberOfGames) {
                System.out.println("Completed " + g + " / " + numberOfGames + " games.");
            }
        }

        // Train the neural network with the collected training data
        if (!trainingData.isEmpty()) {
            System.out.println("\nTraining neural network with collected data...");
            nnTrainer.trainAll(convertTrainingData(trainingData));
            nnTrainer.saveNetwork("neuralnet/trained_network.txt");
            System.out.println("Neural network training completed and saved to trained_network.txt");
        }

        // Display final statistics
        System.out.println("\n=== NeuralNet vs Heuristic Experiment Results ===");
        System.out.println(stats);

        // Check performance and decide if retraining is needed
        double nnWinRate = (stats.neuralNetWins / (double) stats.gamesPlayed) * 100.0;
        if (nnWinRate >= performanceThreshold) {
            System.out.printf("NeuralNetAgent has surpassed the threshold with a win rate of %.2f%%.\n", nnWinRate);
            System.out.println("Consider retraining or creating a new neural network for further improvements.");
        } else {
            System.out.println("NeuralNetAgent has not yet surpassed the threshold. Continue training.");
        }
    }

    /**
     * Converts a list of TrainingExample to SelfPlayMinimaxTrainer.TrainingExample
     *
     * @param trainingData List of TrainingExample
     * @return List of SelfPlayMinimaxTrainer.TrainingExample
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> convertTrainingData(List<NeuralNetVsHeuristicExperiment.TrainingExample> trainingData) {
        List<SelfPlayMinimaxTrainer.TrainingExample> converted = new ArrayList<>();
        for (NeuralNetVsHeuristicExperiment.TrainingExample ex : trainingData) {
            converted.add(new SelfPlayMinimaxTrainer.TrainingExample(ex.getInput(), ex.getTarget()));
        }
        return converted;
    }

    /**
     * Prompts the user for a number input.
     *
     * @param prompt The prompt message.
     * @param min    The minimum acceptable value.
     * @param max    The maximum acceptable value.
     * @return The validated number.
     */
    private int promptForNumber(String prompt, int min, int max) {
        Scanner scanner = new Scanner(System.in);
        System.out.print(prompt);
        while (true) {
            try {
                int number = Integer.parseInt(scanner.nextLine().trim());
                if (number >= min && number <= max) {
                    return number;
                } else {
                    System.out.print("Please enter a number between " + min + " and " + max + ": ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. " + prompt);
            }
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
}
