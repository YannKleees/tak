package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.neuralnet.BoardToInputsConverter;
import com.Tak.AI.neuralnet.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.NeuralNetworkTrainer;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An iterative trainer that repeatedly runs multiple "rounds" of Net-vs-Heuristic training.
 * In each round:
 *   1) We run N training games (the net is always BLUE).
 *   2) We update the network using the data from those games.
 *   3) Optionally, we measure or print some progress (like final boards).
 */
public class IterativeNetVsHeuristicTrainer {

    private final NeuralNetworkTrainer trainer;
    private final int boardSize;
    private final int gamesPerRound;
    private final int totalRounds;
    private final boolean printFinalBoard;  // If true, we'll print the final board each game

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

            // Optionally, additional metrics or evaluations can be added here
        }

        System.out.println("\n=== Iterative Training Complete ===");
    }

    /**
     * Runs 'gamesPerRound' training games vs. heuristic, updating the net after each game.
     *
     * @return the average training loss across all games in this round
     */
    private double runSingleRound() {
        double totalLoss = 0.0;

        for (int i = 1; i <= gamesPerRound; i++) {
            System.out.println("\n--- Game " + i + " / " + gamesPerRound + " in this round ---");

            // 1) Play one game with the net as BLUE and heuristic as GREEN
            List<TrainingGameRecord> gameData = playOneGameCollectStates();

            // 2) Convert to training examples and train once per game
            List<SelfPlayMinimaxTrainer.TrainingExample> trainingExamples = labelData(gameData);

            // 3) Train the network with the collected training examples
            double loss = trainer.trainAll(trainingExamples);
            totalLoss += loss;

            // Log every 5 games to monitor progress
            if (i % 5 == 0) {
                System.out.printf("  Game %d/%d Loss=%.4f\n", i, gamesPerRound, loss);
            }
        }

        return totalLoss / gamesPerRound;
    }

    /**
     * Plays a single game (BLUE=net, GREEN=heuristic),
     * collecting board states for *BLUE moves only* so we can label them post-game.
     *
     * @return a list of (state, currentPlayer, finalTakGame) so we can label them later.
     */
    private List<TrainingGameRecord> playOneGameCollectStates() {
        // Create the net-based player (BLUE)
        Player netPlayer = new MinimaxAgent(
            Color.BLUE, 21, 21, 1, 3, false,
            new NeuralNetworkEvaluator(trainer.getNetwork())
        );
        // Create the heuristic-based player (GREEN)
        Player heuristicPlayer = new MinimaxAgent(
            Color.GREEN, 21, 21, 1, 3, false,
            new HeuristicEvaluator()
        );

        TakGame game = new TakGame(boardSize, Arrays.asList(netPlayer, heuristicPlayer));
        List<TrainingGameRecord> records = new ArrayList<>();

        while (!game.isGameEnded()) {
            Player current = game.getCurrentPlayer();

            // If the current player is BLUE (ANN), store the board state
            if (current == netPlayer) {
                double[] state = BoardToInputsConverter.convert(game.getBoard(), netPlayer);
                records.add(new TrainingGameRecord(state, netPlayer, game));
            }

            try {
                current.makeMove(game);
            } catch (InvalidMoveException | GameOverException e) {
                // Log the exception and terminate the game gracefully
                System.out.println("  An error occurred during the game: " + e.getMessage());
                break;
            }
        }

        // Optionally print the final board state
        if (printFinalBoard) {
            System.out.println("  Final Board State:");
            game.getBoard().printBoard(); // Corrected method call
            System.out.println();
        }

        return records;
    }

    /**
     * Takes the records from a single game and sets label = +1 if BLUE eventually won,
     * -1 if BLUE lost, 0 if tie. Then returns them as a standard list of training examples.
     *
     * @param gameRecords The list of training records collected during the game.
     * @return A list of labeled training examples for the ANN.
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> labelData(List<TrainingGameRecord> gameRecords) {
        // If no records were collected, return an empty list
        if (gameRecords.isEmpty()) {
            return new ArrayList<>();
        }

        // Determine the winner of the game
        TakGame finalGame = gameRecords.get(gameRecords.size() - 1).takGame;
        Player winner = finalGame.getWinner();

        List<SelfPlayMinimaxTrainer.TrainingExample> output = new ArrayList<>();
        for (TrainingGameRecord record : gameRecords) {
            double label;
            if (winner == null) {
                label = 0.0; // Tie
            } else if (winner.getColor() == Color.BLUE) {
                label = 1.0; // ANN won
            } else {
                label = -1.0; // ANN lost
            }

            // Create a training example with the state and the corresponding label
            SelfPlayMinimaxTrainer.TrainingExample ex =
                    new SelfPlayMinimaxTrainer.TrainingExample(record.boardState, new double[]{label});
            output.add(ex);
        }
        return output;
    }

    /**
     * A small wrapper struct to store the board state, the "mover," and the TakGame reference.
     * We use the final TakGame reference to figure out who won after the game ends.
     */
    private static class TrainingGameRecord {
        double[] boardState;
        Player mover;
        TakGame takGame;  // We'll store the final reference to see the winner

        TrainingGameRecord(double[] state, Player mover, TakGame g) {
            this.boardState = state;
            this.mover = mover;
            this.takGame = g;
        }
    }
}
