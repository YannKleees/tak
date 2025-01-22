package com.Tak.AI.neuralnet.trainer;

import com.Tak.AI.actions.Action;
import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.net.BoardToInputsConverter;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.net.NeuralNetworkInitializer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.models.TakGame;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Refined experiment that tests multiple (learningRate, momentum) pairs,
 * with Net-vs-Random training.
 *
 * - "training_summary.csv" will collect the *per-round* logs for all runs
 *   (so you see everything appended in one big file).
 * - "Refined_LR_Momentum_Results.csv" will store the final line for each run
 *   (one line per LR+momentum pair).
 */
public class RefinedAutoLMExperiment {

    // Example param sweeps
    private static final double[] LEARNING_RATES   = {1e-4, 1e-3, 1e-2};
    private static final double[] MOMENTUM_VALUES  = {0.0, 0.9, 0.99};

    // Board/training setup
    private static final int BOARD_SIZE       = 5;   // e.g. 5x5 board
    private static final int GAMES_PER_ROUND  = 5;   // # games each round
    private static final int TOTAL_ROUNDS     = 5;   // # rounds total
    private static final boolean PRINT_BOARD  = false;

    // Where to store network .txt files
    private static final String NET_DIR = "networks";

    // Summaries from each (LR, momentum) run
    private static final String CSV_FINAL_SUMMARY = "Refined_LR_Momentum_Results.csv";

    // The file in which we append *all* round-based logs across all runs
    private static final String CSV_ROUND_LOGS    = "training_summary.csv";

    // If true, remove each net file after finishing each run
    private static final boolean DELETE_NETWORKS_WHEN_DONE = true;

    public static void main(String[] args) {
        ensureNetworkDir();

        // Clear or create the "final summary" CSV
        initFinalSummaryCsv();

        // Also ensure "training_summary.csv" either has a header or you skip if it exists
        // We'll do that just once, so the file grows across runs.
        initRoundLogsCsv(CSV_ROUND_LOGS);

        // Run over all (LR, momentum)
        for (double lr : LEARNING_RATES) {
            for (double m : MOMENTUM_VALUES) {
                runSingleExperiment(lr, m);
            }
        }

        System.out.println("\nAll runs complete!");
        System.out.println(" - Round-by-round logs in: " + CSV_ROUND_LOGS);
        System.out.println(" - One-line final results in: " + CSV_FINAL_SUMMARY);
    }

    /**
     * For a given (LR, momentum), we:
     * 1) Create a brand-new net
     * 2) Iterative train vs random
     * 3) Append final line to "Refined_LR_Momentum_Results.csv"
     * 4) Keep (or delete) the net file
     */
    private static void runSingleExperiment(double learningRate, double momentum) {
        // 1) new net
        NeuralNetworkTrainer trainer = NeuralNetworkInitializer.initializeTrainer();
        trainer.setLearningRate(learningRate);
        trainer.setMomentum(momentum);

        // We'll name the file e.g. "AutoExp_RND_LR1e-03_M0.90.txt"
        String netName = String.format("AutoExp_RND_LR%.0e_M%.2f", learningRate, momentum);
        String netPath = NET_DIR + File.separator + netName + ".txt";

        // 2) build iterative trainer
        IterativeNetVsRandomTrainer it = new IterativeNetVsRandomTrainer(
            trainer,
            BOARD_SIZE,
            GAMES_PER_ROUND,
            TOTAL_ROUNDS,
            PRINT_BOARD,
            CSV_ROUND_LOGS  // pass the path we want to append logs
        );

        System.out.printf("\n=== Starting LR=%.0e, M=%.2f ===\n", learningRate, momentum);

        long start = System.currentTimeMillis();
        it.runIterativeTraining();  // logs all rounds to training_summary.csv
        long end = System.currentTimeMillis();
        double sec = (end - start)/1000.0;

        // 3) Save net
        trainer.saveNetwork(netPath);

        // 4) parse final line from training_summary.csv => final averageLoss
        double finalLoss = it.getLastRoundLoss();

        // 5) append to final summary CSV
        appendToFinalSummary(learningRate, momentum, finalLoss, sec);

        // 6) optionally delete the net
        if (DELETE_NETWORKS_WHEN_DONE) {
            File f = new File(netPath);
            if (f.delete()) {
                System.out.println("Deleted " + netPath);
            } else {
                System.out.println("Warning: could not delete " + netPath);
            }
        }
    }

    // ----------------------------------------------------------------
    // CSV: final summary (1 line per (LR,momentum) run)
    // ----------------------------------------------------------------
    private static void initFinalSummaryCsv() {
        File f = new File(CSV_FINAL_SUMMARY);
        if (!f.exists()) {
            // create + write header
            try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
                w.write("LearningRate,Momentum,FinalAvgLoss,TimeSeconds");
                w.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void appendToFinalSummary(double lr, double m, double finalLoss, double seconds) {
        DecimalFormat df6 = new DecimalFormat("0.######");
        DecimalFormat df2 = new DecimalFormat("0.00");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(CSV_FINAL_SUMMARY, true))) {
            // e.g. "1.0e-04,0.90,0.123456,3.04"
            String row = String.format("%.0e,%s,%s,%.2f",
                                       lr,
                                       df2.format(m),
                                       df6.format(finalLoss),
                                       seconds);
            w.write(row);
            w.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------
    // CSV: round logs. We'll do all runs in "training_summary.csv"
    // ----------------------------------------------------------------
    private static void initRoundLogsCsv(String path) {
        File f = new File(path);
        if (!f.exists()) {
            // If it does not exist, we create it with a header
            try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
                // CHANGED: new header with more columns
                w.write("Round,AverageLoss,NetWinRate,RoundTimeSec");
                w.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // If it exists, do nothing => we'll keep appending
    }

    // ----------------------------------------------------------------
    // ensures we have "networks/" folder
    // ----------------------------------------------------------------
    private static void ensureNetworkDir() {
        File d = new File(NET_DIR);
        if (!d.exists()) {
            if (!d.mkdir()) {
                System.err.println("Failed to create " + NET_DIR + " folder. Exiting.");
                System.exit(1);
            }
        }
    }

    // =========================================================================
    // Inner class: IterativeNetVsRandomTrainer
    // Logs each round to "training_summary.csv" with extra columns now.
    // =========================================================================
    private static class IterativeNetVsRandomTrainer {

        private final NeuralNetworkTrainer trainer;
        private final int boardSize;
        private final int gamesPerRound;
        private final int totalRounds;
        private final boolean printFinalBoard;
        private final String summaryCsvPath;  // e.g. "training_summary.csv"

        private final int randomOpeningPlies = 6;
        private final boolean doMidGameUpdates = true;
        private static final double DISCOUNT = 0.90;

        private final Random rnd = new Random();
        private final int epochsPerRound = 3;

        // We'll store the last average loss at the end of each round
        private double lastRoundLoss = Double.NaN;

        public IterativeNetVsRandomTrainer(NeuralNetworkTrainer trainer,
                                           int boardSize,
                                           int gamesPerRound,
                                           int totalRounds,
                                           boolean printFinalBoard,
                                           String summaryCsvPath) {
            this.trainer = trainer;
            this.boardSize = boardSize;
            this.gamesPerRound = gamesPerRound;
            this.totalRounds = totalRounds;
            this.printFinalBoard = printFinalBoard;
            this.summaryCsvPath = summaryCsvPath;
        }

        public void runIterativeTraining() {
            System.out.println("=== Starting Iterative Net-vs-Random Training ===");
            for (int round = 1; round <= totalRounds; round++) {
                System.out.println("\n--- Round " + round + "/" + totalRounds + " ---");

                long roundStart = System.currentTimeMillis();
                double avgLossThisRound = runSingleRound();
                long roundEnd = System.currentTimeMillis();
                double roundSec = (roundEnd - roundStart)/1000.0;

                System.out.printf("Finished Round %d, AvgLoss=%.6f\n", round, avgLossThisRound);

                // store for later retrieval
                lastRoundLoss = avgLossThisRound;

                // ADDED: we can also compute how many wins the net got 
                // in runSingleRound() (stored in netWinsThisRound)
                double netWinRate = (double) netWinsThisRound / (double) gamesPerRound;

                // Now we append these columns: Round, AvgLoss, NetWinRate, RoundTimeSec
                appendRoundSummary(round, avgLossThisRound, netWinRate, roundSec);
            }
            System.out.println("=== Iterative Training Complete ===");
        }

        // This is the final average loss from the last round we performed
        public double getLastRoundLoss() {
            return lastRoundLoss;
        }

        // We'll store # of net wins each round
        private int netWinsThisRound; // ADDED

        private double runSingleRound() {
            netWinsThisRound = 0; // reset each round

            double totalEpochLoss = 0.0;
            List<SelfPlayMinimaxTrainer.TrainingExample> allData = new ArrayList<>();

            // Play games vs random
            for (int i = 1; i <= gamesPerRound; i++) {
                System.out.printf("Game %d / %d...\n", i, gamesPerRound);
                List<GameRecord> gameData = playOneGameCollectStates();
                // Check if net (BLUE) won
                if (!gameData.isEmpty()) {
                    Player winner = gameData.get(gameData.size() - 1).game.getWinner();
                    if (winner != null && winner.getColor() == Color.BLUE) {
                        netWinsThisRound++;
                    }
                }
                List<SelfPlayMinimaxTrainer.TrainingExample> labeled = labelData(gameData);
                allData.addAll(labeled);
            }

            if (allData.isEmpty()) {
                System.out.println("No data this round, skipping...");
                return 0.0;
            }

            // Train multiple epochs on the combined dataset
            for (int epoch = 1; epoch <= epochsPerRound; epoch++) {
                double epochLoss = trainer.trainAll(allData);
                totalEpochLoss += epochLoss;
                System.out.printf("  Epoch %d/%d, Loss=%.6f\n", epoch, epochsPerRound, epochLoss);
            }
            return totalEpochLoss / epochsPerRound;
        }

        private List<GameRecord> playOneGameCollectStates() {
            // Blue=net, Green=random
            Player netPlayer = new MinimaxAgent(
                Color.BLUE, 21, 21, 1, 2, false,
                new NeuralNetworkEvaluator(trainer.getNetwork())
            );
            Player randomPlayer = new RandomAIPlayer(
                Color.GREEN, 21, 21, 1
            );

            TakGame game = new TakGame(boardSize, Arrays.asList(netPlayer, randomPlayer));
            List<GameRecord> records = new ArrayList<>();

            while (!game.isGameEnded()) {
                Player current = game.getCurrentPlayer();
                int moveCount = game.getMoveCount();

                // random opening
                if (moveCount < randomOpeningPlies * 2) {
                    doRandomOpeningMove(game, current, moveCount);
                } else {
                    // store old state
                    double[] oldState = BoardToInputsConverter.convert(game.getBoard(), current);

                    // make move
                    try {
                        current.makeMove(game);
                    } catch (InvalidMoveException | GameOverException e) {
                        System.out.println("Error during move: " + e.getMessage());
                        break;
                    }

                    // partial update (for some RL-style shaping) if game not ended
                    if (!game.isGameEnded() && doMidGameUpdates) {
                        double[] nextState = BoardToInputsConverter.convert(game.getBoard(), current);
                        double[] nextVal = trainer.getNetwork().forward(nextState);
                        for (int k = 0; k < nextVal.length; k++) {
                            nextVal[k] *= DISCOUNT;
                        }
                        trainer.trainSingleSampleWithInGameRate(oldState, nextVal);
                    }
                    records.add(new GameRecord(oldState, current, game));
                }
            }

            if (printFinalBoard) {
                System.out.println("Final Board:");
                game.getBoard().printBoard();
                System.out.println();
            }

            return records;
        }

        private void doRandomOpeningMove(TakGame game, Player current, int moveCount) {
            List<String> moves = ActionGenerator.generatePossibleActions(game.getBoard(), current, moveCount);
            List<Action> possible = parseActions(moves, current);
            if (!possible.isEmpty()) {
                Action pick = possible.get(rnd.nextInt(possible.size()));
                try {
                    pick.execute(game.getBoard());
                    game.incrementMoveCount();
                    game.checkWinConditions();
                    game.switchPlayer();
                } catch (InvalidMoveException e) {
                    System.out.println("Random opening error: " + e.getMessage());
                }
            }
        }

        private List<Action> parseActions(List<String> moveStrs, Player p) {
            List<Action> out = new ArrayList<>();
            for (String ms : moveStrs) {
                try {
                    Action a = Action.fromString(ms, p.getColor());
                    out.add(a);
                } catch(Exception e) {
                    // skip
                }
            }
            return out;
        }

        // label states after we know the winner
        private List<SelfPlayMinimaxTrainer.TrainingExample> labelData(List<GameRecord> recs) {
            if (recs.isEmpty()) return new ArrayList<>();
            TakGame finalGame = recs.get(recs.size()-1).game;
            Player winner = finalGame.getWinner();

            List<SelfPlayMinimaxTrainer.TrainingExample> out = new ArrayList<>();
            for (GameRecord r : recs) {
                double[] target = new double[2];
                if (winner == null) {
                    target[0] = 0.0;
                    target[1] = 0.0;
                } else {
                    boolean isWin = (r.mover == winner);
                    if (r.mover.getColor() == Color.BLUE) {
                        target[0] = isWin ? 1.0 : -1.0;
                        target[1] = isWin ? -1.0 : 1.0;
                    } else {
                        target[0] = isWin ? -1.0 : 1.0;
                        target[1] = isWin ? 1.0 : -1.0;
                    }
                }
                // optional shaping
                double shaping = computeShaping(r.state);
                if (r.mover.getColor() == Color.BLUE) {
                    target[0] += shaping;
                } else {
                    target[1] += shaping;
                }
                out.add(new SelfPlayMinimaxTrainer.TrainingExample(r.state, target));
            }
            return out;
        }

        private double computeShaping(double[] features) {
            // Example: take the second-last feature as ratio, then scale by 0.1
            int idx = features.length - 2;
            double ratio = features[idx];
            return 0.1 * ratio;
        }

        // Updated to handle new columns
        private void appendRoundSummary(int round, double avgLoss, double netWinRate, double roundTimeSec) {
            DecimalFormat df6 = new DecimalFormat("0.######");
            try (BufferedWriter w = new BufferedWriter(new FileWriter(summaryCsvPath, true))) {
                // Round,AverageLoss,NetWinRate,RoundTimeSec
                String row = String.format(Locale.US, "%d,%s,%.4f,%.2f",
                                           round,
                                           df6.format(avgLoss),
                                           netWinRate,
                                           roundTimeSec);
                w.write(row);
                w.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // minimal record structure
        private static class GameRecord {
            double[] state;
            Player mover;
            TakGame game;
            GameRecord(double[] s, Player m, TakGame g) {
                this.state = s;
                this.mover = m;
                this.game = g;
            }
        }
    }
}
