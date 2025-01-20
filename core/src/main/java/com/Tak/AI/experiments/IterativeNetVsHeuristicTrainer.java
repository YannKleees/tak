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
 *
 * Changes from the default version:
 *   - randomOpeningPlies set to 3.
 *   - multiple training epochs on the game data.
 *   - (Optionally) do an immediate small backprop (inGameRate) after each move. (See ADDED FEATURE block)
 */
public class IterativeNetVsHeuristicTrainer {

    private final NeuralNetworkTrainer trainer;
    private final int boardSize;
    private final int gamesPerRound;
    private final int totalRounds;
    private final boolean printFinalBoard;
    private final String summaryLogPath = "training_summary.csv";

    private final int randomOpeningPlies = 3;
    private final Random rnd = new Random();

    // We do multiple epochs per round on the aggregated dataset
    private final int epochsPerRound = 3;

    // ADDED FEATURE: If you want to do partial “TD-like” updates during the game,
    // define a small inGameRate vs. afterGameRate in your NeuralNetworkTrainer:
    private final boolean doMidGameUpdates = true;

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
        double totalLoss = 0.0;
        List<SelfPlayMinimaxTrainer.TrainingExample> allTrainingExamples = new ArrayList<>();

        for (int i = 1; i <= gamesPerRound; i++) {
            System.out.println("\n--- Game " + i + " / " + gamesPerRound + " in this round ---");
            List<TrainingGameRecord> gameData = playOneGameCollectStates();
            List<SelfPlayMinimaxTrainer.TrainingExample> trainingExamples = labelData(gameData);
            allTrainingExamples.addAll(trainingExamples);
        }

        if (allTrainingExamples.isEmpty()) {
            System.out.println("  No training data collected this round. Skipping training.");
            return 0.0;
        }

        double totalEpochLoss = 0.0;
        for (int epoch = 1; epoch <= epochsPerRound; epoch++) {
            double epochLoss = trainer.trainAll(allTrainingExamples);
            totalEpochLoss += epochLoss;
            System.out.printf("  Epoch %d/%d Loss=%.4f\n", epoch, epochsPerRound, epochLoss);
        }

        return totalEpochLoss / epochsPerRound;
    }

    /**
     * Plays one game: net(BLUE) vs heuristic(GREEN).
     * Collect states for final batch training. Optionally do small partial updates each turn (inGameRate).
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
                List<String> actionStrings =
                    ActionGenerator.generatePossibleActions(game.getBoard(), current, moveCount);
                List<Action> allActions = parseActionStrings(actionStrings, current);

                if (!allActions.isEmpty()) {
                    Action randomAction = allActions.get(rnd.nextInt(allActions.size()));
                    try {
                        randomAction.execute(game.getBoard());
                        game.incrementMoveCount();
                        game.checkWinConditions();
                        game.switchPlayer();
                    } catch (InvalidMoveException e) {
                        System.out.println("  Error during random opening: " + e.getMessage());
                        break;
                    }
                } else {
                    System.out.println("No moves found for random opening. Breaking.");
                    break;
                }
            } else {
                // Store the state from the current player's perspective
                double[] state = BoardToInputsConverter.convert(game.getBoard(), current);
                records.add(new TrainingGameRecord(state, current, game));

                // ADDED FEATURE: If doMidGameUpdates = true, do a partial “TD-like” update using
                // the net's *current* estimate as target. (We use the trainer's inGameRate.)
                if (doMidGameUpdates && current.getColor() == Color.BLUE) {
                    // Example for BLUE only; or do it for both if you prefer:
                    double[] netOut = trainer.getNetwork().forward(state);
                    trainer.trainSingleSampleWithInGameRate(state, netOut);
                }

                try {
                    current.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    System.out.println("  An error occurred during normal move: " + e.getMessage());
                    break;
                }
            }
        }

        if (printFinalBoard) {
            System.out.println("  Final Board State:");
            game.getBoard().printBoard();
            System.out.println();
        }

        // ADDED FEATURE: If you want a final “afterGameRate” fix-up for the last board states,
        // that will happen in labelData() + trainAll() in runSingleRound.
        return records;
    }

    private List<Action> parseActionStrings(List<String> actionStrings, Player player) {
        List<Action> actions = new ArrayList<>();
        for (String actionStr : actionStrings) {
            try {
                Action action = Action.fromString(actionStr, player.getColor());
                actions.add(action);
            } catch (InvalidMoveException e) {
                // skip
            }
        }
        return actions;
    }

    /**
     * After the game, figure out who won and set the final +1/-1 labels accordingly.
     * Optionally ignore non-path wins or do partial pathOnly logic if you like.
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> labelData(List<TrainingGameRecord> gameRecords) {
        if (gameRecords.isEmpty()) {
            return new ArrayList<>();
        }

        TakGame finalGame = gameRecords.get(gameRecords.size() - 1).takGame;
        Player winner = finalGame.getWinner();

        // If you only want to reward path wins, you can check finalGame.getWinReason() if your code supports it.
        // boolean pathOnly = false; // Example toggle

        List<SelfPlayMinimaxTrainer.TrainingExample> output = new ArrayList<>();
        for (TrainingGameRecord record : gameRecords) {
            double[] target = new double[2];
            if (winner == null) {
                // tie => 0.0
                target[0] = 0.0;
                target[1] = 0.0;
            } else {
                boolean isMoverWinner = record.mover.equals(winner);
                if (record.mover.getColor() == Color.BLUE) {
                    target[0] = isMoverWinner ? 1.0 : -1.0;
                    target[1] = isMoverWinner ? -1.0 : 1.0;
                } else {
                    // mover is GREEN
                    target[0] = isMoverWinner ? -1.0 : 1.0;
                    target[1] = isMoverWinner ? 1.0 : -1.0;
                }
            }

            // Example of small shaping
            double shaping = computeRewardShaping(record.boardState);
            if (record.mover.getColor() == Color.BLUE) {
                target[0] += shaping;
            } else {
                target[1] += shaping;
            }

            output.add(new SelfPlayMinimaxTrainer.TrainingExample(record.boardState, target));
        }
        return output;
    }

    private double computeRewardShaping(double[] features) {
        int idx = features.length - 2;
        double ratio = features[idx];
        return 0.1 * ratio;
    }

    private static class TrainingGameRecord {
        double[] boardState;
        Player mover;
        TakGame takGame;

        TrainingGameRecord(double[] state, Player mover, TakGame g) {
            this.boardState = state;
            this.mover = mover;
            this.takGame = g;
        }
    }

    private void logRoundSummary(int round, double avgLoss) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryLogPath, true))) {
            writer.write(round + "," + avgLoss);
            writer.newLine();
            System.out.println("Logged Round " + round + " Summary.");
        } catch (IOException e) {
            System.out.println("Failed to log training summary: " + e.getMessage());
        }
    }
}
