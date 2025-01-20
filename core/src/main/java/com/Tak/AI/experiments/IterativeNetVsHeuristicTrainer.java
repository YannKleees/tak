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
 * Enhanced:
 *   - randomOpeningPlies set to 3 by default.
 *   - multiple training epochs on the game data.
 *   - TRUE TD-like mid-game updates: oldState -> nextState's predicted value * discount.
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

    // If you want to do partial “TD-like” updates during the game:
    private final boolean doMidGameUpdates = true;

    // A discount factor for TD updates
    private static final double DISCOUNT = 0.99;

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
            System.out.println("\n--- Game " + i + " / " + gamesPerRound + " in this round ---");
            List<TrainingGameRecord> gameData = playOneGameCollectStates();
            List<SelfPlayMinimaxTrainer.TrainingExample> labeled = labelData(gameData);
            allTrainingExamples.addAll(labeled);
        }

        if (allTrainingExamples.isEmpty()) {
            System.out.println("  No training data collected this round. Skipping training.");
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
     * Plays one game: net(BLUE) vs heuristic(GREEN).
     * Collect states for final batch training. Also do small partial updates each turn (inGameRate),
     * but now with real TD logic: oldState -> discount * nextStateValue
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
                // Store the old state from the current player's perspective
                double[] oldState = BoardToInputsConverter.convert(game.getBoard(), current);

                // Let the current player move
                try {
                    current.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    System.out.println("  Error during normal move: " + e.getMessage());
                    break;
                }

                // If game not ended, we do a partial TD update:
                if (!game.isGameEnded() && doMidGameUpdates && current.getColor() == Color.BLUE) {
                    // next state from the same player's perspective, or from next player's?
                    // Usually we do "bootstrap" from the same player's next turn perspective:
                    double[] nextState = BoardToInputsConverter.convert(game.getBoard(), current);
                    double[] nextVal = trainer.getNetwork().forward(nextState);

                    // Multiply nextVal by discount
                    for (int k = 0; k < nextVal.length; k++) {
                        nextVal[k] *= DISCOUNT;
                    }
                    // Now train oldState -> nextVal
                    trainer.trainSingleSampleWithInGameRate(oldState, nextVal);
                }

                // Record the state for final label
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
            }
        } else {
            System.out.println("No moves found for random opening. Breaking.");
        }
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
     * After the game, figure out who won and set the final +/-1 labels accordingly,
     * plus any shaping. This is the "final correction."
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> labelData(List<TrainingGameRecord> gameRecords) {
        if (gameRecords.isEmpty()) return new ArrayList<>();

        TakGame finalGame = gameRecords.get(gameRecords.size() - 1).takGame;
        Player winner = finalGame.getWinner();

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
                    target[0] = isMoverWinner ? -1.0 : 1.0;
                    target[1] = isMoverWinner ? 1.0 : -1.0;
                }
            }
            // Keep or remove shaping if you want
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

    // Example shaping function
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
