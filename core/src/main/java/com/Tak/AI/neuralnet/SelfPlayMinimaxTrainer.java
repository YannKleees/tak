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
 *  1) Net-vs-Net or Net-vs-Heuristic.
 *  2) REAL TD mid-game updates for BOTH players:
 *     oldState => discount * nextStateValue (for whichever player just moved).
 *  3) Final correction as +1/-1 for the winner.
 */
public class SelfPlayMinimaxTrainer {

    private NeuralNetworkTrainer nnTrainer;

    // Basic training params
    private int numberOfGames;
    private int validationFrequency;
    private int patience;

    // Net vs Net, or Net vs Heuristic
    private boolean selfPlayNetVsNet = false;

    // If we only want to reward road wins, not implemented
    private boolean pathOnly = false;

    // CSV logging
    private String csvFilename = null;
    private BufferedWriter csvWriter = null;

    // For early stopping and tracking
    private double bestValidationLoss;
    private int patienceCounter;

    // Some counters
    private int blueWinsCount = 0;
    private int lastGameMoves = -1;
    private String lastGameWinnerColor = "None";

    // TD discount factor
    private static final double DISCOUNT = 0.99;

    // Optional: if you want random openings for the first X plies
    private int randomOpeningPlies = 3;

    /**
     * Constructor
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

    // ------------------ Setters ------------------

    public void setSelfPlayNetVsNet(boolean b) {
        this.selfPlayNetVsNet = b;
    }

    public void setCsvLogging(String filename) {
        this.csvFilename = filename;
    }

    /** If you want a certain # of random opening plies. */
    public void setRandomOpeningPlies(int plies) {
        this.randomOpeningPlies = plies;
    }

    // ------------------ Main training loop ------------------

    public void runTraining() {
        initCsvWriter();

        for (int i = 0; i < numberOfGames; i++) {
            long startTime = System.currentTimeMillis();

            System.out.println("=== Self-Play Game " + (i + 1) + "/" + numberOfGames + " ===");
            // 1) Play one game, collecting states
            List<TrainingExample> gameData = playOneGame();

            // 2) Final big correction with afterGameRate
            double oldLR = nnTrainer.getLearningRate();
            nnTrainer.setLearningRate(nnTrainer.getAfterGameRate());
            double gameLoss = nnTrainer.trainAll(gameData);
            nnTrainer.setLearningRate(oldLR);

            // 3) Possibly do a validation step
            double valLoss = Double.NaN;
            if ((i + 1) % validationFrequency == 0) {
                valLoss = validateNetwork();
                System.out.println("Validation Loss after game " + (i + 1) + " = " + valLoss);

                if (valLoss < bestValidationLoss) {
                    bestValidationLoss = valLoss;
                    patienceCounter = 0;
                } else {
                    patienceCounter++;
                    if (patienceCounter >= patience) {
                        System.out.println("Early stopping triggered at game " + (i+1));
                        logCsvRow(i+1, gameLoss, valLoss, bestValidationLoss,
                                  -1, "None", blueWinsCount);
                        break;
                    }
                }
            }

            // 4) Log CSV
            logCsvRow(
                i + 1,
                gameLoss,
                valLoss,
                bestValidationLoss,
                lastGameMoves,
                lastGameWinnerColor,
                blueWinsCount
            );

            long endTime = System.currentTimeMillis();
            long gameTime = endTime - startTime;
            System.out.println("Game " + (i+1) + " finished in " + (gameTime/1000.0) + " sec. Loss=" + gameLoss);
        }

        closeCsvWriter();
    }

    /**
     * Plays one self-play game:
     *   - If randomOpeningPlies > 0, do random moves for that many plies.
     *   - Then do normal minimax moves. 
     *   - After each move, do a partial TD(0) update for whichever side just moved.
     *   - Return all states for a final correction at game end.
     */
    private List<TrainingExample> playOneGame() {
        // Build the game
        TakGame game = createTrainingTakGame(5);

        List<StateRecord> recorded = new ArrayList<>();

        // Keep going until game ends
        while (!game.isGameEnded()) {
            Player current = game.getCurrentPlayer();
            int moveCount = game.getMoveCount();

            try {
                if (moveCount < randomOpeningPlies * 2) {
                    // random opening moves
                    doRandomMove(game, current, moveCount);
                } else {
                    // old state
                    double[] oldState = BoardToInputsConverter.convert(game.getBoard(), current);

                    // do the move
                    current.makeMove(game);

                    // do partial TD update for whomever just moved
                    if (!game.isGameEnded()) {
                        double[] nextState = BoardToInputsConverter.convert(game.getBoard(), current);
                        double[] nextVal = nnTrainer.getNetwork().forward(nextState);
                        for (int k = 0; k < nextVal.length; k++) {
                            nextVal[k] *= DISCOUNT;
                        }
                        nnTrainer.trainSingleSampleWithInGameRate(oldState, nextVal);
                    }

                    recorded.add(new StateRecord(oldState, current));
                }
            } catch (InvalidMoveException | GameOverException e) {
                // game ended or invalid
                break;
            }
        }

        // Summarize
        Player winner = game.getWinner();
        lastGameMoves = game.getMoveCount();
        if (winner == null) {
            lastGameWinnerColor = "None";
        } else {
            lastGameWinnerColor = winner.getColor().toString();
            if (winner.getColor() == Color.BLUE) {
                blueWinsCount++;
            }
        }

        return finalizeLabels(recorded, winner);
    }

    /**
     * If random plies are needed for variety.
     */
    private void doRandomMove(TakGame game, Player current, int moveCount) throws InvalidMoveException, GameOverException {
        List<String> allMoves = com.Tak.AI.utils.ActionGenerator.generatePossibleActions(game.getBoard(), current, moveCount);
        if (allMoves.isEmpty()) {
            System.out.println("No random moves available. Possibly stuck/invalid.");
            return;
        }
        // pick any random
        java.util.Random rnd = new java.util.Random();
        String chosen = allMoves.get(rnd.nextInt(allMoves.size()));
        com.Tak.AI.actions.Action act = com.Tak.AI.actions.Action.fromString(chosen, current.getColor());
        act.execute(game.getBoard());
        game.incrementMoveCount();
        game.checkWinConditions();
        game.switchPlayer();
    }

    /**
     * Build the game with net vs net or net vs heuristic.
     */
    private TakGame createTrainingTakGame(int boardSize) {
        Player blue, green;
        if (selfPlayNetVsNet) {
            // net vs net
            blue = new MinimaxAgent(Color.BLUE, 21,21,1,2,false, new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
            green = new MinimaxAgent(Color.GREEN,21,21,1,2,false, new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
        } else {
            // net vs heuristic
            blue = new MinimaxAgent(Color.BLUE, 21,21,1,2,false, new NeuralNetworkEvaluator(nnTrainer.getNetwork()));
            green = new MinimaxAgent(Color.GREEN,21,21,1,2,false, new HeuristicEvaluator());
            // Or a random side:
            // green = new RandomAIPlayer(Color.GREEN,21,21,1);
        }
        return new TakGame(boardSize, Arrays.asList(blue, green));
    }

    /**
     * Create final [ +1, -1 ] or [ -1, +1 ] labels for each old state in the game, 
     * from that mover's perspective.
     */
    private List<TrainingExample> finalizeLabels(List<StateRecord> records, Player winner) {
        List<TrainingExample> exList = new ArrayList<>();
        for (StateRecord sr : records) {
            double[] finalTarget = new double[2];
            if (winner == null) {
                // tie => (0,0)
                finalTarget[0] = 0.0;
                finalTarget[1] = 0.0;
            } else {
                boolean isMoverWinner = (sr.mover == winner);
                if (sr.mover.getColor() == Color.BLUE) {
                    finalTarget[0] = isMoverWinner ? 1.0 : -1.0;
                    finalTarget[1] = isMoverWinner ? -1.0 : 1.0;
                } else {
                    // green
                    finalTarget[0] = isMoverWinner ? -1.0 : 1.0;
                    finalTarget[1] = isMoverWinner ? 1.0 : -1.0;
                }
            }
            exList.add(new TrainingExample(sr.state, finalTarget));
        }
        return exList;
    }

    // ---------------- Validation ---------------

    private double validateNetwork() {
        // quick example: do 3 small games, measure MSE
        int valGames = 3;
        double totalLoss = 0.0;

        for (int i = 0; i < valGames; i++) {
            TakGame game = createTrainingTakGame(5);
            List<TrainingExample> someStates = new ArrayList<>();

            while (!game.isGameEnded()) {
                try {
                    Player current = game.getCurrentPlayer();
                    double[] s = BoardToInputsConverter.convert(game.getBoard(), current);
                    current.makeMove(game);

                    // record states from e.g. BLUE perspective
                    if (current.getColor() == Color.BLUE) {
                        someStates.add(new TrainingExample(s, new double[]{0,0}));
                    }

                } catch(Exception e) {
                    break;
                }
            }

            Player winner = game.getWinner();
            for (TrainingExample ex : someStates) {
                double[] netOut = nnTrainer.getNetwork().forward(ex.getInput());
                double[] finalLabel = new double[2];
                if (winner == null) {
                    finalLabel[0] = 0; 
                    finalLabel[1] = 0;
                } else if (winner.getColor() == Color.BLUE) {
                    finalLabel[0] = 1;
                    finalLabel[1] = -1;
                } else {
                    finalLabel[0] = -1;
                    finalLabel[1] = 1;
                }
                double thisLoss = 0.0;
                for (int k = 0; k < 2; k++) {
                    thisLoss += Math.pow(finalLabel[k] - netOut[k], 2);
                }
                thisLoss /= 2.0;
                totalLoss += thisLoss;
            }
        }

        return totalLoss / valGames;
    }

    // ------------- CSV Logging -------------

    private void initCsvWriter() {
        if (csvFilename == null) return;
        try {
            csvWriter = new BufferedWriter(new FileWriter(csvFilename, false));
            // header
            csvWriter.write("GameIndex,GameLoss,ValLoss,BestValLoss,GameTimeMillis,GameLength,WinnerColor,BlueWinsSoFar");
            csvWriter.newLine();
            csvWriter.flush();
        } catch (IOException e) {
            csvWriter = null;
            e.printStackTrace();
        }
    }

    private void logCsvRow(int gameIndex, 
                           double gameLoss,
                           double valLoss,
                           double bestValLoss,
                           int gameLength,
                           String winnerColor,
                           int blueWins) {
        if (csvWriter == null) return;
        try {
            String valStr = Double.isNaN(valLoss) ? "" : String.format("%.6f", valLoss);
            // 7 placeholders => 7 arguments
            String row = String.format("%d,%.6f,%s,%.6f,%d,%s,%d",
                                       gameIndex, 
                                       gameLoss,
                                       valStr,
                                       bestValLoss,
                                       gameLength,
                                       winnerColor,
                                       blueWins);
            csvWriter.write(row);
            csvWriter.newLine();
            csvWriter.flush();
        } catch (IOException e) {
            System.err.println("Failed to write row: " + e.getMessage());
        }
    }

    private void closeCsvWriter() {
        if (csvWriter == null) return;
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ------------- Helpers -------------

    private static class StateRecord {
        double[] state;
        Player mover;
        StateRecord(double[] s, Player m) {
            this.state = s;
            this.mover = m;
        }
    }

    public static class TrainingExample {
        private double[] input;
        private double[] target;

        public TrainingExample(double[] in, double[] t) {
            this.input = in;
            this.target = t;
        }
        public double[] getInput() { return input; }
        public double[] getTarget() { return target; }
        public void setTarget(double[] t) { this.target = t; }
    }
}
