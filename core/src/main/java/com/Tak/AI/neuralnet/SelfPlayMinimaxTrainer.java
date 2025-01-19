package com.Tak.AI.neuralnet;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demonstrates self-play or AI vs AI training using Minimax + NeuralNetwork.
 * Updated for 2D targets: [scoreBLUE, scoreGREEN].
 */
public class SelfPlayMinimaxTrainer {

    private NeuralNetworkTrainer nnTrainer;
    private int numberOfGames;
    private int validationFrequency;
    private int patience;
    private double bestValidationLoss;
    private int patienceCounter;

    public SelfPlayMinimaxTrainer(NeuralNetworkTrainer nnTrainer, int numberOfGames, int valFrequency, int patience) {
        this.nnTrainer = nnTrainer;
        this.numberOfGames = numberOfGames;
        this.validationFrequency = valFrequency;
        this.patience = patience;
        this.bestValidationLoss = Double.MAX_VALUE;
        this.patienceCounter = 0;
    }

    public void runTraining() {
        for (int i = 0; i < numberOfGames; i++) {
            System.out.println("=== Game " + (i+1) + "/" + numberOfGames + " ===");
            List<TrainingExample> gameData = playOneGame();
            double gameLoss = trainFromGameData(gameData);
            System.out.println("Game " + (i+1) + " average loss: " + gameLoss);

            // Early stopping
            if ((i+1) % validationFrequency == 0) {
                double valLoss = validateNetwork();
                if (valLoss < bestValidationLoss) {
                    bestValidationLoss = valLoss;
                    patienceCounter = 0;
                    System.out.println("New best val loss: " + valLoss);
                } else {
                    patienceCounter++;
                    System.out.println("No improvement. Patience = " + patienceCounter + "/" + patience);
                    if (patienceCounter >= patience) {
                        System.out.println("Early Stopping triggered!");
                        break;
                    }
                }
            }
        }
    }

    private List<TrainingExample> playOneGame() {
        List<StateRecord> recorded = new ArrayList<>();
        TakGame game = createTrainingTakGame(5);

        while (!game.isGameEnded()) {
            try {
                Player current = game.getCurrentPlayer();
                double[] state = BoardToInputsConverter.convert(game.getBoard(), current);
                current.makeMove(game);

                recorded.add(new StateRecord(state, current));
            } catch (InvalidMoveException | GameOverException e) {
                break;
            }
        }

        Player winner = game.getWinner();
        return convertToTrainingExamples(recorded, winner);
    }

    private List<TrainingExample> convertToTrainingExamples(List<StateRecord> records, Player winner) {
        List<TrainingExample> exList = new ArrayList<>();
        for (StateRecord sr : records) {
            double[] target = new double[2];
            if (winner == null) {
                target[0] = 0.0;
                target[1] = 0.0;
            } else {
                boolean isMoverWinner = sr.mover.equals(winner);
                if (sr.mover.getColor() == Color.BLUE) {
                    if (isMoverWinner) {
                        target[0] = 1.0;
                        target[1] = -1.0;
                    } else {
                        target[0] = -1.0;
                        target[1] = 1.0;
                    }
                } else {
                    // mover is GREEN
                    if (isMoverWinner) {
                        target[0] = -1.0;
                        target[1] = 1.0;
                    } else {
                        target[0] = 1.0;
                        target[1] = -1.0;
                    }
                }
            }

            // small shaping
            double shaping = computeRewardShaping(sr.boardFeatures);
            if (sr.mover.getColor() == Color.BLUE) {
                target[0] += shaping;
            } else {
                target[1] += shaping;
            }

            exList.add(new TrainingExample(sr.boardFeatures, target));
        }
        return exList;
    }

    private double computeRewardShaping(double[] features) {
        int idx = features.length - 2;
        double ratio = features[idx];
        return 0.1 * ratio;
    }

    private double trainFromGameData(List<TrainingExample> examples) {
        return nnTrainer.trainAll(examples);
    }

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
                    if (current.getColor() == Color.BLUE) {
                        valStates.add(new TrainingExample(st, new double[]{0.0, 0.0}));
                    }
                } catch (Exception e) {
                    break;
                }
            }

            Player winner = game.getWinner();
            for (TrainingExample ex : valStates) {
                double[] target = new double[2];
                if (winner == null) {
                    target[0] = 0.0; 
                    target[1] = 0.0;
                } else if (winner.getColor() == Color.BLUE) {
                    target[0] = 1.0;
                    target[1] = -1.0;
                } else {
                    target[0] = -1.0;
                    target[1] = 1.0;
                }
                ex.setTarget(target);

                double[] out = nnTrainer.getNetwork().forward(ex.getInput());
                double loss = 0.0;
                for (int k = 0; k < 2; k++) {
                    loss += Math.pow(ex.getTarget()[k] - out[k], 2);
                }
                loss /= 2.0;
                totalLoss += loss;
            }
        }
        return totalLoss / valGames;
    }

    private TakGame createTrainingTakGame(int boardSize) {
        Player nnPlayer = new MinimaxAgent(
            Color.BLUE, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );
        Player heuristic = new MinimaxAgent(
            Color.GREEN, 21, 21, 1, 3, false,
            new HeuristicEvaluator()
        );
        return new TakGame(boardSize, Arrays.asList(nnPlayer, heuristic));
    }

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
        public TrainingExample(double[] inp, double[] tar) {
            input = inp;
            target = tar;
        }
        public void setTarget(double[] t) { target = t; }
        public double[] getInput() { return input; }
        public double[] getTarget() { return target; }
    }
}
