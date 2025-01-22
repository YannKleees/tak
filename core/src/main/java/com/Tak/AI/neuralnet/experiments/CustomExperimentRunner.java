package com.Tak.AI.neuralnet.experiments;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.net.NeuralNetworkInitializer;
import com.Tak.AI.neuralnet.net.NeuralNetworkTxtManager;
import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.models.TakGame;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A standalone class to run user-selected experiments:
 *   1) Heuristic vs Heuristic
 *   2) Neural-Net vs Neural-Net
 *   3) Heuristic vs Neural-Net
 *
 * For Neural-Net-based experiments, we ask which network file to load and
 * the search depth for the MinimaxAgent. We also ask the user how many games
 * to run. After the runs, we summarize the results: e.g., how many times each side won,
 * average game length, etc.
 *
 * This file is just an example "experiment" to show how you might structure
 * a quick interactive testing environment in your codebase.
 *
 * USAGE:
 *   Run main(...), follow the console prompts.
 */
public class CustomExperimentRunner {

    // Adjust if you want your network folder or file extension in some standardized location
    private static final String NETWORKS_DIR = "networks";
    private static final String NETWORK_FILE_EXTENSION = ".txt";

    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Entry point.
     */
    public static void main(String[] args) {
        System.out.println("=== Welcome to Custom Experiment Runner ===\n");

        // Step 1: Let the user pick experiment type
        int choice = pickExperimentMenu();
        if (choice == 0) {
            System.out.println("Exiting.");
            return;
        }

        // Step 2: Ask user how many games (runs)
        int numGames = promptForNumber("Number of runs (games) to play: ", 1, 5000);

        // We'll store the two players (Blue, Green):
        Player bluePlayer = null;
        Player greenPlayer = null;

        // Step 3: Based on choice, build your two agents
        switch (choice) {
            case 1:
                // Heuristic vs. Heuristic
                System.out.println("\nYou chose Heuristic vs. Heuristic");
                int depthH1 = promptForNumber("Enter search depth for BLUE: ", 1, 10);
                int depthH2 = promptForNumber("Enter search depth for GREEN: ", 1, 10);

                // By default, standingStones in your code are separate from the constructor count,
                // but typically you do something like:
                // new MinimaxAgent(Color, flatStones=21, standing=21, capstones=1, depth, evaluator)
                IEvaluationFunction heuristicEval = new HeuristicEvaluator();
                bluePlayer = new MinimaxAgent(Color.BLUE, 21, 21, 1, depthH1, false, heuristicEval);
                greenPlayer = new MinimaxAgent(Color.GREEN, 21, 21, 1, depthH2, false, heuristicEval);
                break;

            case 2:
                // Neural-Net vs. Neural-Net
                System.out.println("\nYou chose Neural-Net vs. Neural-Net");
                String netFile1 = pickNetworkFile();
                String netFile2 = pickNetworkFile();

                int depthN1 = promptForNumber("Enter search depth for BLUE: ", 1, 10);
                int depthN2 = promptForNumber("Enter search depth for GREEN: ", 1, 10);

                NeuralNetworkTrainer trainer1 = loadNetworkTrainer(netFile1);
                NeuralNetworkTrainer trainer2 = loadNetworkTrainer(netFile2);
                IEvaluationFunction netEval1 = new NeuralNetworkEvaluator(trainer1.getNetwork());
                IEvaluationFunction netEval2 = new NeuralNetworkEvaluator(trainer2.getNetwork());

                bluePlayer = new MinimaxAgent(Color.BLUE, 21, 21, 1, depthN1, false, netEval1);
                greenPlayer = new MinimaxAgent(Color.GREEN, 21, 21, 1, depthN2, false, netEval2);

                break;

            case 3:
                // Heuristic vs. Neural-Net
                System.out.println("\nYou chose Heuristic vs. Neural-Net");
                int side = pickWhichSideIsNet();
                String netFile = pickNetworkFile();
                int depthH = promptForNumber("Enter search depth for Heuristic: ", 1, 10);
                int depthN = promptForNumber("Enter search depth for Neural-Net: ", 1, 10);

                NeuralNetworkTrainer trainer3 = loadNetworkTrainer(netFile);
                IEvaluationFunction netEval3 = new NeuralNetworkEvaluator(trainer3.getNetwork());
                IEvaluationFunction heuristicEval2 = new HeuristicEvaluator();

                if (side == 1) {
                    // Blue is Net, Green is Heuristic
                    bluePlayer = new MinimaxAgent(Color.BLUE, 21, 21, 1, depthN, false, netEval3);
                    greenPlayer = new MinimaxAgent(Color.GREEN, 21, 21, 1, depthH, false, heuristicEval2);
                } else {
                    // Blue is Heuristic, Green is Net
                    bluePlayer = new MinimaxAgent(Color.BLUE, 21, 21, 1, depthH, false, heuristicEval2);
                    greenPlayer = new MinimaxAgent(Color.GREEN, 21, 21, 1, depthN, false, netEval3);
                }
                break;
        }

        // Step 4: Actually run the games
        int boardSize = promptForNumber("\nEnter board size (default=5): ", 3, 10);
        System.out.println("Running " + numGames + " games...");
        ExperimentStats stats = runMultipleGames(bluePlayer, greenPlayer, boardSize, numGames);

        // Step 5: Print summary
        printSummary(stats, bluePlayer, greenPlayer);
        System.out.println("\n=== All done! ===");
    }

    /**
     * Actually plays the given number of games, returning aggregated stats.
     */
    private static ExperimentStats runMultipleGames(Player blue, Player green, int boardSize, int numGames) {
        ExperimentStats stats = new ExperimentStats();
        for (int i = 1; i <= numGames; i++) {
            TakGame game = new TakGame(boardSize, Arrays.asList(blue.copy(), green.copy()));
            // You can also do random openings or other logic as desired.

            long startTime = System.currentTimeMillis();
            while (!game.isGameEnded()) {
                try {
                    Player current = game.getCurrentPlayer();
                    current.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    // Usually means no valid moves or game ended
                    break;
                }
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            Player winner = game.getWinner();
            int moves = game.getMoveCount();

            stats.recordGame(moves, winner, duration);
            System.out.printf("Game %d finished. Winner: %s, Moves=%d, Time=%.2fs\n",
                              i, (winner == null ? "None" : winner.getColor()), moves,
                              (duration / 1000.0));
        }
        return stats;
    }

    /**
     * A helper to print out final aggregated results.
     */
    private static void printSummary(ExperimentStats stats, Player blue, Player green) {
        System.out.println("\n=== Summary of Runs ===");
        System.out.println("Total Games:    " + stats.gamesPlayed);
        System.out.printf("Blue Wins:      %d\n", stats.blueWins);
        System.out.printf("Green Wins:     %d\n", stats.greenWins);
        System.out.printf("Draws:          %d\n", stats.draws);
        System.out.printf("Avg Moves/Game: %.2f\n", stats.getAverageMoves());
        System.out.printf("Avg Time/Game:  %.2f sec\n", stats.getAverageTimeSec());

        // If you want to see agent types, you can do:
        System.out.println("\n--- Agents Info ---");
        System.out.println("Blue Agent Type:  " + blue.getClass().getSimpleName());
        System.out.println("Green Agent Type: " + green.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------------
    // Basic user-menu / prompt methods
    // ------------------------------------------------------------------------

    private static int pickExperimentMenu() {
        System.out.println("Choose experiment type:");
        System.out.println("1) Heuristic vs Heuristic");
        System.out.println("2) Neural-Net vs Neural-Net");
        System.out.println("3) Heuristic vs Neural-Net");
        System.out.println("0) Exit");
        System.out.print("Pick: ");

        while (true) {
            String in = scanner.nextLine().trim();
            if (in.equals("0")) return 0;
            if (in.equals("1")) return 1;
            if (in.equals("2")) return 2;
            if (in.equals("3")) return 3;
            System.out.print("Invalid. Pick again: ");
        }
    }

    private static int pickWhichSideIsNet() {
        System.out.println("\nWhich side is the Neural-Net?");
        System.out.println("1) BLUE is the Net");
        System.out.println("2) GREEN is the Net");
        System.out.print("Pick: ");
        while (true) {
            String in = scanner.nextLine().trim();
            if (in.equals("1")) return 1;
            if (in.equals("2")) return 2;
            System.out.print("Invalid. Pick again: ");
        }
    }

    /**
     * Lists all networks in the "networks" folder (those ending with ".txt").
     * Lets the user pick one. Returns the filename or null if none chosen.
     */
    private static String pickNetworkFile() {
        List<String> nets = listNetworks();
        if (nets.isEmpty()) {
            System.out.println("No networks found in " + NETWORKS_DIR + ". Creating default net in code...");
            // If no net found, you could create a brand new net programmatically or fallback
            return null;
        }

        // Let user pick
        System.out.println("\nAvailable networks:");
        for (int i = 0; i < nets.size(); i++) {
            System.out.println((i + 1) + ") " + nets.get(i));
        }
        System.out.print("Pick network (0=cancel): ");

        while (true) {
            String in = scanner.nextLine().trim();
            int c;
            try {
                c = Integer.parseInt(in);
            } catch (Exception e) {
                c = -1;
            }
            if (c == 0) {
                return null;
            }
            if (c >= 1 && c <= nets.size()) {
                return nets.get(c - 1);
            }
            System.out.print("Invalid. Choose again (0=cancel): ");
        }
    }

    private static List<String> listNetworks() {
        File dir = new File(NETWORKS_DIR);
        File[] files = dir.listFiles((d, n) -> n.endsWith(NETWORK_FILE_EXTENSION));
        List<String> result = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                result.add(f.getName()); // e.g. "MyNet.txt"
            }
        }
        return result;
    }

    /**
     * Helper to load a network + trainer from file.
     * Adjust to match your code’s approach (ANNManager, etc.).
     */
    private static NeuralNetworkTrainer loadNetworkTrainer(String filename) {
        if (filename == null) {
            // fallback: just create a fresh trainer
            System.out.println("No network chosen, using brand-new net in memory...");
            return NeuralNetworkInitializer.initializeTrainer();
        }
        String path = NETWORKS_DIR + File.separator + filename;
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        try {
            NeuralNetworkTxtManager.NeuralNetworkLoadData d =
                NeuralNetworkTxtManager.loadTrainer(path);
            // Put the loaded net inside our trainer
            trainer = new NeuralNetworkTrainer(d.network, 0.001);
            System.out.println("Loaded network from " + path);
        } catch (IOException e) {
            System.out.println("Failed to load net from " + path
                               + ", using fresh net. (" + e.getMessage() + ")");
        }
        return trainer;
    }

    private static int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) {
                    return val;
                }
            } catch (Exception e) {
                // ignore
            }
            System.out.print("Invalid. " + prompt);
        }
    }

    // ------------------------------------------------------------------------
    // Data structure to track aggregated results
    // ------------------------------------------------------------------------
    private static class ExperimentStats {
        int gamesPlayed = 0;
        int blueWins = 0;
        int greenWins = 0;
        int draws = 0;

        double totalMoves = 0;
        double totalTimeMillis = 0;

        void recordGame(int moves, Player winner, long timeMillis) {
            gamesPlayed++;
            totalMoves += moves;
            totalTimeMillis += timeMillis;
            if (winner == null) {
                draws++;
            } else if (winner.getColor() == Color.BLUE) {
                blueWins++;
            } else {
                greenWins++;
            }
        }

        double getAverageMoves() {
            if (gamesPlayed == 0) return 0.0;
            return totalMoves / gamesPlayed;
        }

        double getAverageTimeSec() {
            if (gamesPlayed == 0) return 0.0;
            return (totalTimeMillis / 1000.0) / gamesPlayed;
        }
    }
}
