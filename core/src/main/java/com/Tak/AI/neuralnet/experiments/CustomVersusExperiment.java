package com.Tak.AI.neuralnet.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.net.NeuralNetworkTxtManager;
import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.models.TakGame;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CustomVersusExperiment {

    private static final String NETWORKS_DIR = "networks";
    private static final String NETWORK_FILE_EXTENSION = ".txt";
    private static final Scanner scanner = new Scanner(System.in);

    private static class Matchup {
        String blueStrategy; // "HEURISTIC MINIMAX" or "RANDOM"
        String greenNetwork; // Filename for GREEN's network
        int numGames;
    }

    private static class MatchResult {
        int gamesPlayed;
        int blueWins;
        int greenWins;
        int draws;
        int totalMoves;
        long totalDurationMillis;

        double getAverageMoves() {
            return gamesPlayed == 0 ? 0.0 : (double) totalMoves / gamesPlayed;
        }

        double getAverageTimeSec() {
            return gamesPlayed == 0 ? 0.0 : (totalDurationMillis / 1000.0) / gamesPlayed;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Custom Versus Experiment ===");

        System.out.println("\nAdd a new matchup? (yes/no): ");
        String ans = scanner.nextLine().trim().toLowerCase();
        if (ans.equals("yes") || ans.equals("y")) {
            List<Matchup> matchups = new ArrayList<>();

            String blueStrategy = pickBlueStrategy();
            if (blueStrategy == null) {
                System.out.println("Cancelled. No matchup added.");
                return;
            }

            String greenNetwork = pickNetwork("Select the network for GREEN");
            if (greenNetwork == null) {
                System.out.println("Cancelled. No matchup added.");
                return;
            }

            int games = promptForNumber("How many games for this matchup? ", 1, 100000);

            Matchup m = new Matchup();
            m.blueStrategy = blueStrategy;
            m.greenNetwork = greenNetwork;
            m.numGames = games;
            matchups.add(m);

            System.out.printf("Added: %s vs %s, %d games.\n", blueStrategy, greenNetwork, games);

            int boardSize = promptForNumber("\nEnter board size (default=5): ", 3, 10);

            for (Matchup mu : matchups) {
                System.out.printf("\n=== Running Matchup: %s (BLUE) vs %s (GREEN), %d games ===\n",
                    mu.blueStrategy, mu.greenNetwork, mu.numGames);

                MatchResult r = runMatchup(mu.blueStrategy, mu.greenNetwork, mu.numGames, boardSize);
                saveResultsToCSV(mu, r);
                printMatchSummary(mu, r);
            }

            System.out.println("\n=== Done. Analyze or graph these results externally. ===");
        } else {
            System.out.println("No matchups scheduled. Exiting.");
        }
    }

    private static MatchResult runMatchup(String blueStrategy, String greenNetwork,
                                          int numGames, int boardSize) {
        MatchResult stat = new MatchResult();

        IEvaluationFunction greenEval;
        NeuralNetworkTrainer greenTrainer = loadNetwork(greenNetwork);
        greenEval = greenTrainer != null ? new NeuralNetworkEvaluator(greenTrainer.getNetwork()) : new HeuristicEvaluator();

        for (int g = 1; g <= numGames; g++) {
            Player bluePlayer;
            switch (blueStrategy) {
                case "HEURISTIC MINIMAX":
                    bluePlayer = new MinimaxAgent(Color.BLUE, 21, 21, 1, 2, false, new HeuristicEvaluator());
                    break;
                case "RANDOM":
                    bluePlayer = new RandomAIPlayer(Color.BLUE, 21, 21, 1);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown strategy: " + blueStrategy);
            }

            Player greenPlayer = new MinimaxAgent(Color.GREEN, 21, 21, 1, 2, false, greenEval);

            // Alternate starting player
            List<Player> players = (g % 2 == 1)
                ? Arrays.asList(bluePlayer, greenPlayer) // Blue starts
                : Arrays.asList(greenPlayer, bluePlayer); // Green starts

            TakGame game = new TakGame(boardSize, players);

            long start = System.currentTimeMillis();
            while (!game.isGameEnded()) {
                try {
                    game.getCurrentPlayer().makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    break;
                }
            }
            long end = System.currentTimeMillis();

            stat.gamesPlayed++;
            stat.totalDurationMillis += (end - start);
            stat.totalMoves += game.getMoveCount();

            Player winner = game.getWinner();
            if (winner == null) {
                stat.draws++;
            } else if (winner.getColor() == Color.BLUE) {
                stat.blueWins++;
            } else {
                stat.greenWins++;
            }

            System.out.printf("Game %d finished. Winner=%s, Moves=%d, Time=%.2fs\n",
                g, (winner == null ? "None" : winner.getColor()),
                game.getMoveCount(),
                (end - start) / 1000.0);
        }

        return stat;
    }

    private static void saveResultsToCSV(Matchup mu, MatchResult r) {
        String filename = String.format("results_%s_vs_%s.csv", mu.blueStrategy, mu.greenNetwork);
        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Matchup,Games Played,Blue Wins,Green Wins,Draws,Win Rate (Blue),Win Rate (Green),Avg Moves,Avg Time (sec)\n");

            double blueWinRate = r.gamesPlayed > 0 ? (double) r.blueWins / r.gamesPlayed * 100 : 0.0;
            double greenWinRate = r.gamesPlayed > 0 ? (double) r.greenWins / r.gamesPlayed * 100 : 0.0;

            sb.append(String.format(
                "%s vs %s,%d,%d,%d,%d,%.2f%%,%.2f%%,%.2f,%.2f\n",
                mu.blueStrategy, mu.greenNetwork, r.gamesPlayed, r.blueWins, r.greenWins, r.draws,
                blueWinRate, greenWinRate, r.getAverageMoves(), r.getAverageTimeSec()
            ));

            writer.write(sb.toString());
            System.out.println("Results saved to " + filename);
        } catch (IOException e) {
            System.out.println("Error saving results: " + e.getMessage());
        }
    }

    private static String pickBlueStrategy() {
        System.out.println("\nSelect strategy for BLUE (1=Heuristic MiniMax, 2=Random, -1=Cancel): ");
        while (true) {
            String input = scanner.nextLine().trim();
            switch (input) {
                case "1":
                    return "HEURISTIC MINIMAX";
                case "2":
                    return "RANDOM";
                case "-1":
                    return null;
                default:
                    System.out.print("Invalid choice. Enter 1, 2, or -1 to cancel: ");
            }
        }
    }

    private static void printMatchSummary(Matchup mu, MatchResult r) {
        System.out.printf("\n--- Matchup Summary for %s (BLUE) vs %s (GREEN) ---\n",
            mu.blueStrategy, mu.greenNetwork);
        System.out.println("Games played: " + r.gamesPlayed);
        System.out.printf("  Blue Wins:  %d\n", r.blueWins);
        System.out.printf("  Green Wins: %d\n", r.greenWins);
        System.out.printf("  Draws:      %d\n", r.draws);
        System.out.printf("  Avg Moves:  %.2f\n", r.getAverageMoves());
        System.out.printf("  Avg Time:   %.2f sec/game\n", r.getAverageTimeSec());
    }

    private static String pickNetwork(String prompt) {
        System.out.println("\n" + prompt + " (-1=cancel)");

        List<String> nets = listNetworks();
        if (nets.isEmpty()) {
            System.out.println("No .txt networks found in folder '" + NETWORKS_DIR + "'.");
            return null;
        }

        for (int i = 0; i < nets.size(); i++) {
            System.out.println((i + 1) + ") " + nets.get(i));
        }

        while (true) {
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice == -1) {
                    return null;
                } else if (choice >= 1 && choice <= nets.size()) {
                    return nets.get(choice - 1);
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.print("Invalid choice. Enter a valid number or -1 to cancel: ");
        }
    }

    private static List<String> listNetworks() {
        File dir = new File(NETWORKS_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(NETWORK_FILE_EXTENSION));
        List<String> out = new ArrayList<>();
        if (files != null) {
            for (File f : files) {
                out.add(f.getName());
            }
        }
        return out;
    }

    private static NeuralNetworkTrainer loadNetwork(String netFilename) {
        if (netFilename == null) return null;
        String path = NETWORKS_DIR + File.separator + netFilename;
        try {
            NeuralNetworkTxtManager.NeuralNetworkLoadData data =
                NeuralNetworkTxtManager.loadTrainer(path);
            NeuralNetworkTrainer trainer = new NeuralNetworkTrainer(data.network, 0.001);
            System.out.println("Loaded network from " + path);
            return trainer;
        } catch (IOException e) {
            System.out.println("Failed to load net from " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) {
                    return val;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.print("Invalid input. " + prompt);
        }
    }
}

