package com.Tak.AI.neuralnet;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates self-play or AI vs AI training using Minimax + NeuralNetwork.
 *
 * Upgrades:
 *  1) Option to do Net-vs-Net (param: selfPlayNetVsNet).
 *  2) You can swap in RandomAIPlayer for a fully random side.
 *  3) Expanded CSV logging: logs game length, winner color, Blue's total wins, etc.
 *  4) Basic track of Blue's net wins across games for debugging improvement.
 */
public class SelfPlayMinimaxTrainer {

    private NeuralNetworkTrainer nnTrainer;

    // Basic training parameters
    private int numberOfGames;
    private int validationFrequency;
    private int patience;

    // Net vs Net, or Net vs Heuristic
    private boolean selfPlayNetVsNet = false;

    // Track validation progress
    private double bestValidationLoss;
    private int patienceCounter;

    // Optionally only reward road wins (not implemented, but you can handle it if needed)
    private boolean pathOnly = false;

    // CSV logging
    private String csvFilename = null;
    private BufferedWriter csvWriter = null;

    // For tracking net's progress
    private int blueWinsCount = 0;

    // Fields to record data after each game
    private int lastGameMoves = -1;
    private String lastGameWinnerColor = "None";

    /**
     * Main constructor.
     */
    public SelfPlayMinimaxTrainer(NeuralNetworkTrainer nnTrainer,
                                  int numberOfGames,
                                  int valFrequency,
                                  int patience) {
        this.nnTrainer = nnTrainer;
        this.numberOfGames = numberOfGames;
        this.validationFrequency = valFrequency;
        this.patience = patience;
        this.bestValidationLoss = Double.MAX_VALUE;
        this.patienceCounter = 0;
    }




    // ----------------------
    // Setters
    // ----------------------

    /**
     * If true, both sides (BLUE, GREEN) will use the net-based evaluator.
     * If false, BLUE=net, GREEN=heuristic (the default).
     *
     * If you want a random side, see createTrainingTakGame(...) below and
     * change whichever side you like to new RandomAIPlayer(...).
     */
    public void setSelfPlayNetVsNet(boolean b) {
        this.selfPlayNetVsNet = b;
    }

    /**
     * If you want to log your training progress to a CSV file,
     * call this before runTraining().
     */
    public void setCsvLogging(String filename) {
        this.csvFilename = filename;
    }

    // ---------------------
    // Main Training Process
    // ---------------------

    public void runTraining() {
        // If CSV logging requested, open the file and write a header row:
        initCsvWriter();

        for (int i = 0; i < numberOfGames; i++) {
            long startGameTime = System.currentTimeMillis();

            System.out.println("=== Game " + (i + 1) + "/" + numberOfGames + " ===");
            // Play one self-play game
            List<TrainingExample> gameData = playOneGame();

            // Final big correction at game end (afterGameRate)
            double oldLR = nnTrainer.getLearningRate();
            nnTrainer.setLearningRate(nnTrainer.getAfterGameRate());

            double gameLoss = trainFromGameData(gameData);
            nnTrainer.setLearningRate(oldLR);

            long gameTimeMillis = System.currentTimeMillis() - startGameTime;
            System.out.println("Game " + (i + 1) + " average loss: " + gameLoss);

            // Possibly do validation
            double valLoss = Double.NaN;  // Not computed unless we do a validation step
            if ((i + 1) % validationFrequency == 0) {
                valLoss = validateNetwork();
                System.out.println("Validation loss at game " + (i + 1) + ": " + valLoss);

                if (valLoss < bestValidationLoss) {
                    bestValidationLoss = valLoss;
                    patienceCounter = 0;
                    System.out.println("New best val loss: " + valLoss);
                } else {
                    patienceCounter++;
                    System.out.println("No improvement. patience=" + patienceCounter + "/" + patience);
                    if (patienceCounter >= patience) {
                        System.out.println("Early stopping triggered!");
                        // We'll log CSV and break
                        logCsvRow(i+1, gameLoss, valLoss, bestValidationLoss,
                                  -1, "None", blueWinsCount);
                        break;
                    }
                }
            }

            // Log metrics for this game to CSV
            logCsvRow(
                i + 1,
                gameLoss,
                valLoss,
                bestValidationLoss,
                lastGameMoves,
                lastGameWinnerColor,
                blueWinsCount
            );
        }

        closeCsvWriter();
    }

    /**
     * Play a single self-play game. 
     * By default:
     *   - If selfPlayNetVsNet=true => both sides are net-based Minimax.
     *   - If false => Blue=net-based, Green=heuristic. 
     *   - (If you want random for a side, see createTrainingTakGame() to do so.)
     */
    private List<TrainingExample> playOneGame() {
        TakGame game = createTrainingTakGame(5);

        List<StateRecord> recorded = new ArrayList<>();

        while (!game.isGameEnded()) {
            try {
                Player current = game.getCurrentPlayer();

                // Convert board => features
                double[] state = BoardToInputsConverter.convert(game.getBoard(), current);

                // In-game partial update
                double[] netOut = nnTrainer.getNetwork().forward(state);
                nnTrainer.trainSingleSampleWithInGameRate(state, netOut);

                // Then do actual move (Minimax or Random or Heuristic, depending on the player's type)
                current.makeMove(game);

                // Record the position + mover for final afterGameRate updates
                recorded.add(new StateRecord(state, current));

            } catch (InvalidMoveException | GameOverException e) {
                // Means the game ended or move was invalid
                break;
            }
        }

        // Summarize final results
        Player winner = game.getWinner();
        lastGameMoves = game.getMoveCount();
        if (winner != null) {
            lastGameWinnerColor = winner.getColor().toString();
            if (winner.getColor() == Color.BLUE) {
                blueWinsCount++;
            }
        } else {
            lastGameWinnerColor = "None";
        }

        // Convert states -> training examples
        return convertToTrainingExamples(recorded, winner);
    }

    private double trainFromGameData(List<TrainingExample> examples) {
        return nnTrainer.trainAll(examples);
    }

    /**
     * Evaluate how well the net is doing, returning a "validation loss" measure.
     * We run a few short games (3 by default) to measure MSE on final states from BLUE's perspective.
     */
    private double validateNetwork() {
        int valGames = 3;
        double totalLoss = 0.0;

        for (int i = 0; i < valGames; i++) {
            TakGame game = createTrainingTakGame(5);
            List<TrainingExample> valStates = new ArrayList<>();

            while (!game.isGameEnded()) {
                try {
                    Player current = game.getCurrentPlayer();
                    double[] st = BoardToInputsConverter.convert(game.getBoard(), current);
                    current.makeMove(game);

                    // We'll just record some states from BLUE's perspective
                    if (current.getColor() == Color.BLUE) {
                        valStates.add(new TrainingExample(st, new double[]{0.0, 0.0}));
                    }
                } catch (Exception e) {
                    break;
                }
            }
            Player winner = game.getWinner();

            // Evaluate MSE
            for (TrainingExample ex : valStates) {
                double[] target = new double[2];
                if (winner == null) {
                    // tie => [0,0]
                    target[0] = 0; 
                    target[1] = 0;
                } else if (winner.getColor() == Color.BLUE) {
                    target[0] = 1;
                    target[1] = -1;
                } else {
                    target[0] = -1;
                    target[1] = 1;
                }
                ex.setTarget(target);

                double[] out = nnTrainer.getNetwork().forward(ex.getInput());
                double loss = 0;
                for (int k = 0; k < 2; k++) {
                    loss += Math.pow(ex.getTarget()[k] - out[k], 2);
                }
                loss /= 2.0;
                totalLoss += loss;
            }
        }
        return totalLoss / valGames;
    }

    /**
     * createTrainingTakGame:
     *   - If selfPlayNetVsNet=true, both BLUE and GREEN are net-based MinimaxAgents.
     *   - Else, BLUE=Minimax(Net), GREEN=Heuristic.
     * 
     * If you want a random side, just replace one side with:
     *   new RandomAIPlayer(Color.GREEN, 21,21,1)
     * for example. 
     */
    private TakGame createTrainingTakGame(int boardSize) {
        Player blue, green;

        if (selfPlayNetVsNet) {
            // Net vs Net
            blue = new MinimaxAgent(Color.BLUE, 21,21,1, 2, false, new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
            green = new MinimaxAgent(Color.GREEN, 21,21,1, 2, false, new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
        } else {
            // Net vs Heuristic by default
            blue = new MinimaxAgent(Color.BLUE, 21,21,1, 2, false, new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
            green = new MinimaxAgent(Color.GREEN, 21,21,1, 2, false, new HeuristicEvaluator());
        }

        // EXAMPLE: if you want a random GREEN instead of heuristic:
        // green = new RandomAIPlayer(Color.GREEN, 21,21,1);

        return new TakGame(boardSize, Arrays.asList(blue, green));
    }

    // ------------------------------------------------
    // Convert final recorded states -> training examples
    // ------------------------------------------------
    private List<TrainingExample> convertToTrainingExamples(List<StateRecord> records, Player winner) {
        List<TrainingExample> exList = new ArrayList<>();
        for (StateRecord sr : records) {
            double[] target = new double[2];
            if (winner == null) {
                target[0] = 0.0;
                target[1] = 0.0;
            } else {
                boolean isMoverWinner = (sr.mover == winner);
                Color c = sr.mover.getColor();
                if (c == Color.BLUE) {
                    // BLUE's perspective
                    if (isMoverWinner) {
                        target[0] = 1.0;  // BLUE
                        target[1] = -1.0;
                    } else {
                        target[0] = -1.0;
                        target[1] = 1.0;
                    }
                } else {
                    // GREEN
                    if (isMoverWinner) {
                        target[0] = -1.0;
                        target[1] = 1.0;
                    } else {
                        target[0] = 1.0;
                        target[1] = -1.0;
                    }
                }
            }
            exList.add(new TrainingExample(sr.boardFeatures, target));
        }
        return exList;
    }

    // -----------------------------------
    // CSV Logging Support (expanded columns)
    // -----------------------------------
    private void initCsvWriter() {
        if (csvFilename == null) {
            return;
        }
        try {
            boolean append = false;
            csvWriter = new BufferedWriter(new FileWriter(csvFilename, append));
            // Write header row
            csvWriter.write("GameIndex,GameLoss,ValLoss,BestValLoss,GameTimeMillis,GameLength,WinnerColor,BlueWinsSoFar");
            csvWriter.newLine();
            csvWriter.flush();
        } catch (IOException e) {
            System.err.println("Failed to init CSV writer: " + e.getMessage());
            csvWriter = null;
        }
    }

    private void logCsvRow(int gameIndex, 
                           double gameLoss, 
                           double valLoss, 
                           double bestVal, 
                           int gameLength,
                           String winnerColor,
                           int blueWinsSoFar) {
        if (csvWriter == null) {
            return;
        }
        try {
            String valStr = Double.isNaN(valLoss) ? "" : String.valueOf(valLoss);
            String row = String.format("%d,%.6f,%s,%.6f,%d,%d,%s,%d",
                                       gameIndex, gameLoss, valStr, bestVal,
                                       gameLength, winnerColor, blueWinsSoFar);
            csvWriter.write(row);
            csvWriter.newLine();
            csvWriter.flush();
        } catch (IOException e) {
            System.err.println("Failed to write row to CSV: " + e.getMessage());
        }
    }

    private void closeCsvWriter() {
        if (csvWriter == null) {
            return;
        }
        try {
            csvWriter.close();
        } catch (IOException e) {
            System.err.println("Failed to close CSV: " + e.getMessage());
        }
    }

    // ------------------------------------------------

    private static class StateRecord {
        double[] boardFeatures;
        Player mover;
        StateRecord(double[] feats, Player m) {
            boardFeatures = feats;
            mover = m;
        }
    }

    public static class TrainingExample {
        private double[] input;
        private double[] target;

        public TrainingExample(double[] in, double[] t) {
            input = in;
            target = t;
        }

        public double[] getInput() {
            return input;
        }

        public double[] getTarget() {
            return target;
        }

        public void setTarget(double[] t) {
            target = t;
        }
    }
}
