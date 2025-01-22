package com.Tak.AI.neuralnet.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.net.NeuralNetworkTxtManager;
import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.models.TakGame;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A simple interactive experiment tool. Lets you pick pairs of networks (or
 * heuristic AI by entering 0), number of games to play, then runs them
 * and prints out the results.
 *
 * After each matchup, it prints results.
 * Then at the very end, it prints all matchups' results again.
 */
public class VersusExperiment {

    private static final String NETWORKS_DIR = "networks";
    private static final String NETWORK_FILE_EXTENSION = ".txt";
    private static final Scanner scanner = new Scanner(System.in);

    // A small class to describe a "matchup" (net1, net2, numberOfGames).
    private static class Matchup {
        String netFileBlue;   // which net is playing as BLUE (or "HEURISTIC")
        String netFileGreen;  // which net is playing as GREEN (or "HEURISTIC")
        int numGames;
    }

    // A small class to hold stats about each matchup:
    private static class MatchResult {
        int gamesPlayed;
        int blueWins;
        int greenWins;
        int draws;
        int totalMoves;
        long totalDurationMillis;

        double getAverageMoves() {
            if (gamesPlayed == 0) return 0.0;
            return (double) totalMoves / gamesPlayed;
        }

        double getAverageTimeSec() {
            if (gamesPlayed == 0) return 0.0;
            return (totalDurationMillis / 1000.0) / gamesPlayed;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Versus Experiment (Heuristic = 0, or pick net file) ===");

        // 1) Let user schedule multiple matchups:
        List<Matchup> matchups = new ArrayList<>();
        while (true) {
            System.out.println("\nAdd a new matchup? (yes/no): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.equals("no") || ans.equals("n")) {
                break; // done adding matchups
            }

            // Let user pick two "nets" (which could also be "HEURISTIC")
            String netBlue = pickNetwork("Select the network for BLUE");
            if (netBlue == null) {
                System.out.println("Cancelled. No more matchups.");
                break;
            }
            String netGreen = pickNetwork("Select the network for GREEN");
            if (netGreen == null) {
                System.out.println("Cancelled. No more matchups.");
                break;
            }

            // How many games to play?
            int games = promptForNumber("How many games for this matchup? ", 1, 100000);

            // Store a new matchup
            Matchup m = new Matchup();
            m.netFileBlue = netBlue;   // could be "HEURISTIC" or a filename
            m.netFileGreen = netGreen; // could be "HEURISTIC" or a filename
            m.numGames = games;
            matchups.add(m);

            System.out.printf("Added: %s vs %s, %d games.\n", netBlue, netGreen, games);
        }

        // 2) If we have no matchups, exit.
        if (matchups.isEmpty()) {
            System.out.println("\nNo matchups scheduled. Exiting.");
            return;
        }

        // 3) For each matchup, run the experiment and collect stats
        List<MatchResult> results = new ArrayList<>();
        int boardSize = promptForNumber("\nEnter board size (default=5): ", 3, 10);

        for (int i = 0; i < matchups.size(); i++) {
            Matchup mu = matchups.get(i);
            System.out.printf(
                "\n=== Running Matchup %d/%d: %s (BLUE) vs %s (GREEN), %d games ===\n",
                i + 1, matchups.size(),
                mu.netFileBlue, mu.netFileGreen, mu.numGames
            );

            MatchResult r = runMatchup(mu.netFileBlue, mu.netFileGreen, mu.numGames, boardSize);
            results.add(r);

            // Print partial summary after each matchup
            printMatchSummary(mu, r);
        }

        // 4) Print a consolidated summary of **all** matchups
        System.out.println("\n=== ALL MATCHUPS COMPLETE! FINAL SUMMARIES: ===");
        for (int i = 0; i < matchups.size(); i++) {
            Matchup mu = matchups.get(i);
            MatchResult r = results.get(i);
            printMatchSummary(mu, r);
        }

        System.out.println("\n=== Done. You can now analyze or graph these results externally. ===");
    }

    /**
     * Runs a single matchup of netBlue vs netGreen for the given number of games on the given boardSize.
     */
    private static MatchResult runMatchup(String netFileBlue, String netFileGreen,
                                          int numGames, int boardSize)
    {
        MatchResult stat = new MatchResult();

        // Build the evaluation function for BLUE
        IEvaluationFunction blueEval;
        if ("HEURISTIC".equals(netFileBlue)) {
            // If user picked "0", we do heuristic
            blueEval = new HeuristicEvaluator();
        } else {
            // else load the net from file
            NeuralNetworkTrainer trainerBlue = loadNetwork(netFileBlue);
            if (trainerBlue != null) {
                blueEval = new NeuralNetworkEvaluator(trainerBlue.getNetwork());
            } else {
                // fallback if loading failed
                blueEval = new HeuristicEvaluator();
            }
        }

        // Build the evaluation function for GREEN
        IEvaluationFunction greenEval;
        if ("HEURISTIC".equals(netFileGreen)) {
            greenEval = new HeuristicEvaluator();
        } else {
            NeuralNetworkTrainer trainerGreen = loadNetwork(netFileGreen);
            if (trainerGreen != null) {
                greenEval = new NeuralNetworkEvaluator(trainerGreen.getNetwork());
            } else {
                greenEval = new HeuristicEvaluator();
            }
        }

        // Create Player objects for each game, as copies
        for (int g = 1; g <= numGames; g++) {
            // Each new game has fresh player copies:
            Player bluePlayer = new MinimaxAgent(
                Color.BLUE, 21, 21, 1,
                2, // or ask user for depth
                false,
                blueEval
            );

            Player greenPlayer = new MinimaxAgent(
                Color.GREEN, 21, 21, 1,
                2,
                false,
                greenEval
            );

            TakGame game = new TakGame(boardSize, Arrays.asList(bluePlayer, greenPlayer));

            long start = System.currentTimeMillis();
            while (!game.isGameEnded()) {
                try {
                    game.getCurrentPlayer().makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    // means the game ended or no valid moves
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
                (end - start) / 1000.0
            );
        }

        return stat;
    }

    /**
     * Print a quick summary of a single matchup result.
     */
    private static void printMatchSummary(Matchup mu, MatchResult r) {
        System.out.println("\n--- Matchup Summary for " + mu.netFileBlue + " (BLUE) vs "
            + mu.netFileGreen + " (GREEN) ---");
        System.out.println("Games played: " + r.gamesPlayed);
        System.out.printf("  Blue Wins:  %d\n", r.blueWins);
        System.out.printf("  Green Wins: %d\n", r.greenWins);
        System.out.printf("  Draws:      %d\n", r.draws);
        System.out.printf("  Avg Moves:  %.2f\n", r.getAverageMoves());
        System.out.printf("  Avg Time:   %.2f sec/game\n", r.getAverageTimeSec());
    }

    // -------------------------------------------------
    // HELPER METHODS FOR NETWORK LOADING & INPUT PROMPTS
    // -------------------------------------------------

    /**
     * Let the user pick one of the .txt networks in networks/ folder, or "0" for heuristic.
     * If user enters -1, we consider it "cancel".
     * If none found, returns null.
     */
    private static String pickNetwork(String prompt) {
        System.out.println("\n" + prompt + " (-1=cancel, 0=Heuristic)");

        List<String> nets = listNetworks();
        // Show the list
        System.out.println("0) Heuristic");
        if (nets.isEmpty()) {
            System.out.println("No .txt networks found in folder '" + NETWORKS_DIR + "'.");
            // user can still pick 0 for heuristic or -1 to cancel
        } else {
            for (int i = 0; i < nets.size(); i++) {
                System.out.println((i + 1) + ") " + nets.get(i));
            }
        }

        while (true) {
            String in = scanner.nextLine().trim();
            int ch;
            try {
                ch = Integer.parseInt(in);
            } catch (Exception e) {
                ch = -999; // invalid
            }
            if (ch == -1) {
                // user wants to cancel
                return null;
            }
            if (ch == 0) {
                // user picked heuristic
                return "HEURISTIC";
            }
            if (!nets.isEmpty() && ch >= 1 && ch <= nets.size()) {
                // user picked a net from the list
                return nets.get(ch - 1);
            }
            System.out.print("Invalid. Enter -1=cancel, 0=Heuristic, or 1.." + nets.size() + ": ");
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
        if (netFilename == null || netFilename.equals("HEURISTIC")) return null;
        String path = NETWORKS_DIR + File.separator + netFilename;
        // Use your code for loading a network:
        try {
            NeuralNetworkTxtManager.NeuralNetworkLoadData data =
                NeuralNetworkTxtManager.loadTrainer(path);
            NeuralNetworkTrainer t = new NeuralNetworkTrainer(data.network, 0.001);
            System.out.println("Loaded network from " + path);
            return t;
        } catch (IOException e) {
            System.out.println("Failed to load net from " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static int promptForNumber(String prompt, int min, int max) {
        System.out.print(prompt);
        while (true) {
            String in = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(in);
                if (val >= min && val <= max) {
                    return val;
                }
            } catch (Exception ignored) { }
            System.out.print("Invalid. " + prompt);
        }
    }
}
