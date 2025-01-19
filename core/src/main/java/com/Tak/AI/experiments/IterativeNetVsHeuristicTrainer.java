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
 * Changes:
 *   - randomOpeningPlies set to 3 (instead of 2).
 *   - multiple training epochs on the game data to help the net learn.
 */
public class IterativeNetVsHeuristicTrainer {

    private final NeuralNetworkTrainer trainer;
    private final int boardSize;
    private final int gamesPerRound;
    private final int totalRounds;
    private final boolean printFinalBoard;  // If true, we'll print the final board each game
    private final String summaryLogPath = "training_summary.csv"; // Path to save training summaries

    // Increase how many plies to randomize => 3 means each player gets 3 random moves
    private final int randomOpeningPlies = 3;
    private final Random rnd = new Random();

    // Let's do multiple epochs per round to ensure we fully train on the new data
    private final int epochsPerRound = 3;

    /**
     * @param trainer         The network trainer to be improved repeatedly.
     * @param boardSize       e.g., 5 for a 5x5 Tak board
     * @param gamesPerRound   how many training games to run each round
     * @param totalRounds     how many rounds of training
     * @param printFinalBoard if true, log the final board at the end of each game
     */
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

        // Initialize the summary log file with headers
        initializeSummaryLog();
    }

    /**
     * Initialize the summary log with CSV headers.
     */
    private void initializeSummaryLog() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(summaryLogPath))) {
            writer.write("Round,AverageLoss");
            writer.newLine();
            System.out.println("Initialized training summary log at " + summaryLogPath);
        } catch (IOException e) {
            System.out.println("Failed to initialize summary log: " + e.getMessage());
        }
    }

    /**
     * Run iterative training: multiple "rounds" of Net-vs-Heuristic training.
     */
    public void runIterativeTraining() {
        System.out.println("\n=== Starting Iterative Net-vs-Heuristic Training ===");

        // Iterate through each training round
        for (int round = 1; round <= totalRounds; round++) {
            System.out.println("\n--- Round " + round + " / " + totalRounds + " ---");

            // Execute a single round of training and obtain the average loss
            double avgLoss = runSingleRound();

            // Display average loss for the round
            System.out.printf("Finished Round %d. AvgLoss=%.4f\n", round, avgLoss);

            // Log the round summary
            logRoundSummary(round, avgLoss);
        }

        System.out.println("\n=== Iterative Training Complete ===");
    }

    /**
     * Runs 'gamesPerRound' training games vs. heuristic, accumulating the game data,
     * then trains the network multiple epochs on that dataset.
     *
     * @return the average training loss across all games in this round
     */
    private double runSingleRound() {
        double totalLoss = 0.0;
        List<SelfPlayMinimaxTrainer.TrainingExample> allTrainingExamples = new ArrayList<>();

        // Play games, collect data
        for (int i = 1; i <= gamesPerRound; i++) {
            System.out.println("\n--- Game " + i + " / " + gamesPerRound + " in this round ---");
            List<TrainingGameRecord> gameData = playOneGameCollectStates();
            List<SelfPlayMinimaxTrainer.TrainingExample> trainingExamples = labelData(gameData);
            allTrainingExamples.addAll(trainingExamples);
        }

        // Now we train multiple epochs on all the data from this round
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

        totalLoss = totalEpochLoss / epochsPerRound;
        return totalLoss;
    }

    /**
     * Plays a single game (BLUE=net, GREEN=heuristic),
     * collecting board states for *both players* so we can label them post-game.
     *
     * We add randomization to the first few plies to avoid repeating the same openings.
     */
    private List<TrainingGameRecord> playOneGameCollectStates() {
        Player netPlayer = new MinimaxAgent(
            Color.BLUE, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(trainer.getNetwork())
        );
        Player heuristicPlayer = new MinimaxAgent(
            Color.GREEN, 21, 21, 1, 1, false,
            new HeuristicEvaluator()
        );

        TakGame game = new TakGame(boardSize, Arrays.asList(netPlayer, heuristicPlayer));
        List<TrainingGameRecord> records = new ArrayList<>();

        while (!game.isGameEnded()) {
            Player current = game.getCurrentPlayer();
            int moveCount = game.getMoveCount();

            if (moveCount < randomOpeningPlies * 2) {
                // random opening moves for the first 'randomOpeningPlies' plies each
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

                try {
                    current.makeMove(game);
                } catch (InvalidMoveException | GameOverException e) {
                    System.out.println("  An error occurred during normal move: " + e.getMessage());
                    break;
                }
            }
        }

        // Optionally print final board
        if (printFinalBoard) {
            System.out.println("  Final Board State:");
            game.getBoard().printBoard();
            System.out.println();
        }

        return records;
    }

    /**
     * Helper: parse a list of action strings into Action objects
     */
    private List<Action> parseActionStrings(List<String> actionStrings, Player player) {
        List<Action> actions = new ArrayList<>();
        for (String actionStr : actionStrings) {
            try {
                Action action = Action.fromString(actionStr, player.getColor());
                actions.add(action);
            } catch (InvalidMoveException e) {
                // Ignore invalid actions
            }
        }
        return actions;
    }

    /**
     * Takes the records from a single game and sets label = +1 if BLUE eventually won,
     * -1 if BLUE lost, 0 if tie. Then returns them as a standard list of training examples.
     *
     * We do it for each record's mover perspective.
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> labelData(List<TrainingGameRecord> gameRecords) {
        if (gameRecords.isEmpty()) {
            return new ArrayList<>();
        }

        // Determine the winner of the game
        TakGame finalGame = gameRecords.get(gameRecords.size() - 1).takGame;
        Player winner = finalGame.getWinner();

        List<SelfPlayMinimaxTrainer.TrainingExample> output = new ArrayList<>();
        for (TrainingGameRecord record : gameRecords) {
            double[] target = new double[2];
            if (winner == null) {
                // Tie => 0 for both
                target[0] = 0.0;
                target[1] = 0.0;
            } else {
                boolean isMoverWinner = record.mover.equals(winner);
                if (record.mover.getColor() == Color.BLUE) {
                    // If mover is BLUE
                    if (isMoverWinner) {
                        target[0] = 1.0;   // BLUE's perspective
                        target[1] = -1.0;  // GREEN's perspective
                    } else {
                        target[0] = -1.0;
                        target[1] = 1.0;
                    }
                } else {
                    // Mover is GREEN
                    if (isMoverWinner) {
                        target[0] = -1.0; 
                        target[1] = 1.0;
                    } else {
                        target[0] = 1.0;
                        target[1] = -1.0;
                    }
                }
            }

            // (Optional) add a small shaping term
            double shaping = computeRewardShaping(record.boardState);

            if (record.mover.getColor() == Color.BLUE) {
                target[0] += shaping; // slightly shift BLUE's perspective
            } else {
                target[1] += shaping; // shift GREEN's perspective
            }

            output.add(new SelfPlayMinimaxTrainer.TrainingExample(record.boardState, target));
        }
        return output;
    }

    /**
     * Compute reward shaping based on board features.
     * Example: let's say the second-to-last feature is occupancy ratio
     *
     * @param features The feature vector of the board state.
     * @return A small shaping reward (0.1 * ratio).
     */
    private double computeRewardShaping(double[] features) {
        int idx = features.length - 2;
        double ratio = features[idx];
        return 0.1 * ratio; // a small shaped reward
    }

    /**
     * Simple wrapper to store the board state, the "mover," and the game reference
     * so we can label them after the game ends.
     */
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

    /**
     * Log the summary of a training round to the CSV file.
     *
     * @param round    The current round number.
     * @param avgLoss  The average loss for this round.
     */
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
