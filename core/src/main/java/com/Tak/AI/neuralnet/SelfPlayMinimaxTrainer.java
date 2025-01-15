package com.Tak.AI.neuralnet;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Demonstrates self-play or AI vs. AI training using Minimax + NeuralNetwork.</p>
 *
 * <strong>Basic Steps (Potential Outline):</strong>
 * <ol>
 *   <li>Create a TakGame with two MinimaxAgents that use {@link NeuralNetworkEvaluator}.</li>
 *   <li>Simulate a full game (AI vs. AI).</li>
 *   <li>Gather (state -> outcome) data at each step or final result.</li>
 *   <li>Train the network with that data via {@code nnTrainer.trainAll(...)}.</li>
 *   <li>Repeat for multiple games to improve the network's evaluation ability.</li>
 * </ol>
 */
public class SelfPlayMinimaxTrainer {

    private NeuralNetworkTrainer nnTrainer;
    private int numberOfGames;

    public SelfPlayMinimaxTrainer(NeuralNetworkTrainer nnTrainer, int numberOfGames) {
        this.nnTrainer = nnTrainer;
        this.numberOfGames = numberOfGames;
    }

    public void runTraining() {
        for (int i = 0; i < numberOfGames; i++) {
            System.out.println("Starting game " + (i + 1) + " of " + numberOfGames + "...");
            List<TrainingExample> gameExamples = playOneGame();
            System.out.println("Game " + (i + 1) + " ended. Training from data...");
            trainFromGameData(gameExamples);

            if ((i + 1) % 100 == 0) {
                System.out.println("Completed " + (i + 1) + " games.");
            }
        }
    }

    private List<TrainingExample> playOneGame() {
        List<StateRecord> recordedStates = new ArrayList<>();
        TakGame game = createExampleTakGame();

        while (!game.isGameEnded()) {
            try {
                Player current = game.getCurrentPlayer();
                double[] state = BoardToInputsConverter.convert(game.getBoard(), current);

                current.makeMove(game);

                Board board = game.getBoard();
                // (Optional) board.printBoard(); // if you want to see each step

                // Record the (state -> which player moved)
                recordedStates.add(new StateRecord(state, current));

            } catch (InvalidMoveException | GameOverException e) {
                break;
            }
        }

        Player winner = game.getWinner();
        List<TrainingExample> examples = new ArrayList<>();

        // Convert each recorded state into a training example
        for (StateRecord sr : recordedStates) {
            double[] inputFeatures = sr.boardFeatures;
            double[] target = new double[1];
            if (winner == null) {
                // Tie
                target[0] = 0.0;
            } else if (sr.mover.equals(winner)) {
                target[0] = 1.0;
            } else {
                target[0] = -1.0;
            }
            examples.add(new TrainingExample(inputFeatures, target));
        }

        return examples;
    }

    private TakGame createExampleTakGame() {
        int boardSize = 5;
        Player player1 = new MinimaxAgent(
            Color.BLUE, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );
        Player player2 = new MinimaxAgent(
            Color.GREEN, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );

        List<Player> players = Arrays.asList(player1, player2);
        return new TakGame(boardSize, players);
    }

    private void trainFromGameData(List<TrainingExample> examples) {
        nnTrainer.trainAll(examples);
    }

    private static class StateRecord {
        double[] boardFeatures;
        Player mover;

        StateRecord(double[] boardFeatures, Player mover) {
            this.boardFeatures = boardFeatures;
            this.mover = mover;
        }
    }

    public static class TrainingExample {
        private double[] input;
        private double[] target;

        public TrainingExample(double[] input, double[] target) {
            this.input = input;
            this.target = target;
        }

        public double[] getInput() {
            return input;
        }

        public double[] getTarget() {
            return target;
        }
    }
}
