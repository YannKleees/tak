package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.neuralnet.BoardToInputsConverter;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.actions.Action;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * An iterative trainer that repeatedly runs multiple "rounds" of Net-vs-Heuristic training.
 * In each round:
 *   1) Run N training games (the net is always BLUE).
 *   2) Collect training data from those games (both players).
 *   3) Train the network for a few epochs.
 *   4) Log training metrics.
 */
public class IterativeNetVsHeuristicTrainer {

    private final NeuralNetworkTrainer trainer;
    private final int boardSize;
    private final int gamesPerRound;
    private final int totalRounds;
    private final boolean printFinalBoard;
    private final String summaryLogPath = "training_summary.csv";

    // CHANGED from 3 to 6:
    private final int randomOpeningPlies = 6;

    private final Random rnd = new Random();

    // multiple epochs per round
    private final int epochsPerRound = 3;

    // If you want partial “TD-like” updates
    private final boolean doMidGameUpdates = true;

    // UPDATED: discount factor reduced to 0.90 (instead of 0.99)
    private static final double DISCOUNT = 0.90;

    public IterativeNetVsHeuristicTrainer(NeuralNetworkTrainer trainer,
                                          int boardSize,
                                          int gamesPerRound,
                                          int totalRounds,
                                          boolean printFinalBoard) {
        this.trainer = trainer;
        this.boardSize = boardSize;
        this.gamesPerRound = gamesPerRound;
        this.totalRounds = totalRounds;
        this.printFinalBoard = printFinalBoard;

        initializeSummaryLog();
    }

    private void initializeSummaryLog() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryLogPath))) {
            writer.write("Round,AverageLoss");
            writer.newLine();
            System.out.println("Initialized training summary log at " + summaryLogPath);
        } catch (IOException e) {
            System.out.println("Failed to initialize summary log: " + e.getMessage());
        }
    }

    public void runIterativeTraining() {
        System.out.println("\n=== Starting Iterative Net-vs-Heuristic Training ===");
        for (int round = 1; round <= totalRounds; round++) {
            System.out.println("\n--- Round " + round + " / " + totalRounds + " ---");

            double avgLoss = runSingleRound();
            System.out.printf("Finished Round %d. AvgLoss=%.4f\n", round, avgLoss);

            logRoundSummary(round, avgLoss);
        }

        System.out.println("\n=== Iterative Training Complete ===");
    }

    private double runSingleRound() {
        double totalEpochLoss = 0.0;
        List<SelfPlayMinimaxTrainer.TrainingExample> allTrainingExamples = new ArrayList<>();

        for (int i = 1; i <= gamesPerRound; i++) {
            System.out.println("\n--- Game " + i + " / " + gamesPerRound + " ---");
            List<TrainingGameRecord> gameData = playOneGameCollectStates();
            List<SelfPlayMinimaxTrainer.TrainingExample> labeled = labelData(gameData);
            allTrainingExamples.addAll(labeled);
        }

        if (allTrainingExamples.isEmpty()) {
            System.out.println("No training data collected this round. Skipping training.");
            return 0.0;
        }

        // multiple epochs on the combined dataset
        for (int epoch = 1; epoch <= epochsPerRound; epoch++) {
            double epochLoss = trainer.trainAll(allTrainingExamples);
            totalEpochLoss += epochLoss;
            System.out.printf("  Epoch %d/%d Loss=%.4f\n", epoch, epochsPerRound, epochLoss);
        }

        return totalEpochLoss / epochsPerRound;
    }

    /**
     * Play one game: net(BLUE) vs heuristic(GREEN).
     * Collect states for final batch training.
     * Also do partial updates each turn (inGameRate).
     */
    private List<TrainingGameRecord> playOneGameCollectStates() {
        Player netPlayer = new MinimaxAgent(
            Color.BLUE, 21, 21, 1, 2, false,
            new NeuralNetworkEvaluator(trainer.getNetwork())
        );
        Player heuristicPlayer = new MinimaxAgent(
            Color.GREEN, 21, 21, 1, 2, false,
            new HeuristicEvaluator()
        );

        TakGame game = new TakGame(boardSize, Arrays.asList(netPlayer, heuristicPlayer));
        List<TrainingGameRecord> records = new ArrayList<>();

        while (!game.isGameEnded()) {
            Player current = game.getCurrentPlayer();
            int moveCount = game.getMoveCount();

            // random openings for first few plies
            if (moveCount < randomOpeningPlies * 2) {
                randomOpeningMove(game, current, moveCount);
            } else {
                // Store old state
                double[] oldState = BoardToInputsConverter.convert(game.getBoard(), current);

                // current makes a move
                try {
                    current.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    System.out.println("Error during move: " + e.getMessage());
                    break;
                }

                // partial TD updates for BOTH players
                if (!game.isGameEnded() && doMidGameUpdates) {
                    double[] nextState = BoardToInputsConverter.convert(game.getBoard(), current);
                    double[] nextVal = trainer.getNetwork().forward(nextState);
                    for (int k = 0; k < nextVal.length; k++) {
                        nextVal[k] *= DISCOUNT;
                    }
                    trainer.trainSingleSampleWithInGameRate(oldState, nextVal);
                }

                // record state
                records.add(new TrainingGameRecord(oldState, current, game));
            }
        }

        if (printFinalBoard) {
            System.out.println("  Final Board State:");
            game.getBoard().printBoard();
            System.out.println();
        }

        return records;
    }

    private void randomOpeningMove(TakGame game, Player current, int moveCount) {
        List<String> actions = ActionGenerator.generatePossibleActions(game.getBoard(), current, moveCount);
        List<Action> possible = parse(actions, current);
        if (!possible.isEmpty()) {
            Action randomA = possible.get(rnd.nextInt(possible.size()));
            try {
                randomA.execute(game.getBoard());
                game.incrementMoveCount();
                game.checkWinConditions();
                game.switchPlayer();
            } catch (InvalidMoveException e) {
                System.out.println("Error in random opening: " + e.getMessage());
            }
        } else {
            System.out.println("No moves found for random opening.");
        }
    }

    private List<Action> parse(List<String> moveStrs, Player p) {
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

    /**
     * After the game, figure out who won and set final +/-1 labels,
     * plus shaping. (unchanged)
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> labelData(List<TrainingGameRecord> gameRecords) {
        if (gameRecords.isEmpty()) return new ArrayList<>();
        TakGame finalGame = gameRecords.get(gameRecords.size()-1).takGame;
        Player winner = finalGame.getWinner();

        List<SelfPlayMinimaxTrainer.TrainingExample> outputs = new ArrayList<>();
        for (TrainingGameRecord rec : gameRecords) {
            double[] target = new double[2];
            if (winner == null) {
                target[0] = 0.0;
                target[1] = 0.0;
            } else {
                boolean isWin = (rec.mover == winner);
                if (rec.mover.getColor() == Color.BLUE) {
                    target[0] = isWin ? 1.0 : -1.0;
                    target[1] = isWin ? -1.0 : 1.0;
                } else {
                    target[0] = isWin ? -1.0 : 1.0;
                    target[1] = isWin ? 1.0 : -1.0;
                }
            }
            // shaping or not
            double shaping = computeRewardShaping(rec.boardState);
            if (rec.mover.getColor() == Color.BLUE) {
                target[0] += shaping;
            } else {
                target[1] += shaping;
            }
            outputs.add(new SelfPlayMinimaxTrainer.TrainingExample(rec.boardState, target));
        }
        return outputs;
    }

    private double computeRewardShaping(double[] features) {
        // same as your example
        int idx = features.length - 2;
        double ratio = features[idx];
        return 0.1 * ratio;
    }

    private static class TrainingGameRecord {
        double[] boardState;
        Player mover;
        TakGame takGame;
        TrainingGameRecord(double[] s, Player m, TakGame g) {
            this.boardState = s;
            this.mover = m;
            this.takGame = g;
        }
    }

    private void logRoundSummary(int round, double avgLoss) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(summaryLogPath, true))) {
            w.write(round + "," + avgLoss);
            w.newLine();
            System.out.println("Logged Round " + round + " Summary.");
        } catch (IOException e) {
            System.out.println("Failed to log summary: " + e.getMessage());
        }
    }
}
