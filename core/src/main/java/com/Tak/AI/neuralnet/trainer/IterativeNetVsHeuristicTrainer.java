package com.Tak.AI.neuralnet.trainer;

import com.Tak.AI.actions.Action;
import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.neuralnet.net.BoardToInputsConverter;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

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
 *   1) We run N training games. **Half** of them net is BLUE, half net is GREEN.
 *   2) Collect training data from those games.
 *   3) Train the network for a few epochs.
 *   4) Log training metrics.
 */
public class IterativeNetVsHeuristicTrainer {

    private static final String TAG = "IterativeNetVsHeuristicTrainer";

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

    // --------------------------------------------------------------------
    // ADDED: finalTrainLoss, finalValLoss, plus getters
    // --------------------------------------------------------------------
    private double finalTrainLoss = 0.0;
    private double finalValLoss   = 0.0;

    public double getFinalTrainLoss() { return finalTrainLoss; }
    public double getFinalValLoss()   { return finalValLoss; }
    // --------------------------------------------------------------------

    public IterativeNetVsHeuristicTrainer(
        NeuralNetworkTrainer trainer,
        int boardSize,
        int gamesPerRound,
        int totalRounds,
        boolean printFinalBoard
    ) {
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
            Logger.log(TAG, "Initialized training summary log at " + summaryLogPath);
        } catch (IOException e) {
            Logger.log(TAG, "Failed to initialize summary log: " + e.getMessage());
        }
    }

    public void runIterativeTraining() {
        Logger.log(TAG, "\n=== Starting Iterative Net-vs-Heuristic Training ===");
        for (int round = 1; round <= totalRounds; round++) {
            Logger.log(TAG, String.format("\n--- Round %d / %d ---", round, totalRounds));

            double avgLoss = runSingleRound();
            Logger.log(TAG, String.format("Finished Round %d. AvgLoss=%.4f", round, avgLoss));

            // compute a small validation loss
            double valLoss = computeValidationLoss();
            Logger.log(TAG, String.format("Validation Loss=%.4f", valLoss));

            // store these in finalTrainLoss/finalValLoss
            this.finalTrainLoss = avgLoss;
            this.finalValLoss   = valLoss;

            // quick check for over/under-fitting
            if (avgLoss < valLoss * 0.8) {
                Logger.log(TAG, "WARNING: Potential overfitting (trainLoss << valLoss).");
            } else if (avgLoss > valLoss * 1.2) {
                Logger.log(TAG, "WARNING: Potential underfitting (trainLoss >> valLoss).");
            } else {
                Logger.log(TAG, "Train vs Val Loss are close => Possibly OK.");
            }

            logRoundSummary(round, avgLoss);
        }

        Logger.log(TAG, "\n=== Iterative Training Complete ===");
    }

    /**
     * In a single round, we do `gamesPerRound` games.
     * Half the games => net is BLUE, heuristic is GREEN.
     * The other half => net is GREEN, heuristic is BLUE.
     */
    private double runSingleRound() {
        double totalEpochLoss = 0.0;
        List<SelfPlayMinimaxTrainer.TrainingExample> allTrainingExamples = new ArrayList<>();

        int half = gamesPerRound / 2;  // integer division
        int remainder = gamesPerRound % 2;

        // net as BLUE for the first `half + remainder` games
        for (int i = 1; i <= (half + remainder); i++) {
            Logger.log(TAG, String.format("\n--- Game %d / %d (Net=BLUE) ---", i, gamesPerRound));
            List<TrainingGameRecord> gameData = playOneGameCollectStates(/* netAsBlue= */ true);
            List<SelfPlayMinimaxTrainer.TrainingExample> labeled = labelData(gameData);
            allTrainingExamples.addAll(labeled);
        }
        // net as GREEN for the remaining `half` games
        for (int i = (half + remainder) + 1; i <= gamesPerRound; i++) {
            Logger.log(TAG, String.format("\n--- Game %d / %d (Net=GREEN) ---", i, gamesPerRound));
            List<TrainingGameRecord> gameData = playOneGameCollectStates(/* netAsBlue= */ false);
            List<SelfPlayMinimaxTrainer.TrainingExample> labeled = labelData(gameData);
            allTrainingExamples.addAll(labeled);
        }

        if (allTrainingExamples.isEmpty()) {
            Logger.log(TAG, "No training data collected this round. Skipping training.");
            return 0.0;
        }

        // multiple epochs on the combined dataset
        for (int epoch = 1; epoch <= epochsPerRound; epoch++) {
            double epochLoss = trainer.trainAll(allTrainingExamples);
            totalEpochLoss += epochLoss;
            Logger.log(TAG, String.format("  Epoch %d/%d Loss=%.4f", epoch, epochsPerRound, epochLoss));
        }

        return totalEpochLoss / epochsPerRound;
    }

    /**
     * 1 game: net vs heuristic. We decide net's color based on `netAsBlue`.
     * Collect states for final batch training.
     * Also do partial updates each turn (inGameRate).
     */
    private List<TrainingGameRecord> playOneGameCollectStates(boolean netAsBlue) {
        // Build the net-based MinimaxAgent
        Player netPlayer = new MinimaxAgent(
            netAsBlue ? Color.BLUE : Color.GREEN,   // net color
            21, 21, 1,
            2, // search depth
            false,
            new NeuralNetworkEvaluator(trainer.getNetwork())
        );

        // Build the heuristic-based MinimaxAgent
        Player heuristicPlayer = new MinimaxAgent(
            netAsBlue ? Color.GREEN : Color.BLUE,   // heuristic color is the opposite
            21, 21, 1,
            2,
            false,
            new HeuristicEvaluator()
        );

        // Initialize the game with netPlayer, heuristicPlayer
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
                    Logger.log(TAG, "Error during move: " + e.getMessage());
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
            Logger.log(TAG, "  Final Board State:");
            game.getBoard().printBoard();
            Logger.log(TAG, "");
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
                Logger.log(TAG, "Error in random opening: " + e.getMessage());
            }
        } else {
            Logger.log(TAG, "No moves found for random opening.");
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
                // tie
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
        // example shaping approach: last 2 features store ratio?
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
            Logger.log(TAG, "Logged Round " + round + " Summary.");
        } catch (IOException e) {
            Logger.log(TAG, "Failed to log summary: " + e.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    // Minimal "computeValidationLoss()" method for checking net vs. heuristic
    // --------------------------------------------------------------------------
    private double computeValidationLoss() {
        int valGames = 3;
        double totalLoss = 0.0;
        int count = 0;

        for (int i = 0; i < valGames; i++) {
            // net(blue) vs. heuristic(green) as a quick check
            Player netPlayer = new MinimaxAgent(
                Color.BLUE, 21, 21, 1, 2, false,
                new NeuralNetworkEvaluator(trainer.getNetwork())
            );
            Player heuristicPlayer = new MinimaxAgent(
                Color.GREEN, 21, 21, 1, 2, false,
                new HeuristicEvaluator()
            );

            TakGame game = new TakGame(boardSize, Arrays.asList(netPlayer, heuristicPlayer));

            // collect states from net's perspective
            List<double[]> statesBlue = new ArrayList<>();

            while (!game.isGameEnded()) {
                Player current = game.getCurrentPlayer();
                if (current.getColor() == Color.BLUE) {
                    double[] st = BoardToInputsConverter.convert(game.getBoard(), current);
                    statesBlue.add(st);
                }

                try {
                    current.makeMove(game);
                } catch (Exception e) {
                    Logger.log(TAG, "Error during validation move: " + e.getMessage());
                    break;
                }
            }

            // final outcome => label states
            Player winner = game.getWinner();
            for (double[] st : statesBlue) {
                double[] target = new double[2];
                if (winner == null) {
                    // tie
                    target[0] = 0;
                    target[1] = 0;
                } else if (winner.getColor() == Color.BLUE) {
                    target[0] = 1;
                    target[1] = -1;
                } else {
                    target[0] = -1;
                    target[1] = 1;
                }

                // forward pass => measure MSE
                double[] out = trainer.getNetwork().forward(st);
                double mse = 0.0;
                for (int k = 0; k < 2; k++){
                    mse += Math.pow(target[k] - out[k], 2);
                }
                mse /= 2.0;
                totalLoss += mse;
                count++;
            }
        }

        if (count == 0) return 0.0;
        return totalLoss / count;
    }
}
