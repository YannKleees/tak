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
 *   <li>Call <code>nnTrainer.train(...)</code> or <code>nnTrainer.trainAll(...)</code> with that data.</li>
 *   <li>Repeat for multiple games to (hopefully) improve the network's evaluation ability.</li>
 * </ol>
 */
public class SelfPlayMinimaxTrainer {

    private NeuralNetworkTrainer nnTrainer; 
    private int numberOfGames;

    /**
     * <p>Constructs a SelfPlayMinimaxTrainer with specified parameters.</p>
     *
     * @param nnTrainer     The trainer for the neural net (provides backprop calls).
     * @param numberOfGames How many self-play or AI-vs-AI games to run.
     */
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
                System.out.println("Current player: " + current.getColor());
                current.makeMove(game);
System.out.println("Move made successfully. Switching player.");
                Board board = game.getBoard();
                board.printBoard();
                recordedStates.add(new StateRecord(state, current));
            } catch (InvalidMoveException | GameOverException e) {
                break;
            }
        }

        Player winner = game.getWinner();
        List<TrainingExample> examples = new ArrayList<>();

        for (StateRecord sr : recordedStates) {
            double[] inputFeatures = sr.boardFeatures;
            double[] target = new double[1];

            if (winner == null) {
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
            Color.BLUE, 15, 6, 1, 3, true,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );
        Player player2 = new MinimaxAgent(
            Color.GREEN, 15, 6, 1, 3, true,
            new NeuralNetworkEvaluator(nnTrainer.getNetwork())
        );

        // **Updated Constructor Call: Use List<Player> instead of two Player arguments**
        List<Player> players = Arrays.asList(player1, player2);
        TakGame game = new TakGame(boardSize, players);
        return game;
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
