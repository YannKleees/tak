package com.Tak.AI.experiments;

import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.neuralnet.*;
import com.Tak.AI.neuralnet.SelfPlayMinimaxTrainer.TrainingExample;
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
 * Demonstrates training a single neural network (as BLUE)
 * against a heuristic-based agent (GREEN).
 * Only the BLUE states are collected and updated.
 */
public class NetVsHeuristicTrainer {

    private final NeuralNetworkTrainer trainer;   // The net we want to train
    private final int boardSize;
    private final int totalGames;
    private final int logInterval;

    /**
     * @param trainer   The network trainer (already loaded or newly created).
     * @param boardSize e.g. 5 for 5x5
     * @param totalGames how many training games to run
     * @param logInterval how often to log progress (e.g., every 100 games)
     */
    public NetVsHeuristicTrainer(NeuralNetworkTrainer trainer,
                                 int boardSize,
                                 int totalGames,
                                 int logInterval) {
        this.trainer = trainer;
        this.boardSize = boardSize;
        this.totalGames = totalGames;
        this.logInterval = logInterval;
    }

    public void runTraining() {
        System.out.println("\n=== Starting Net-vs-Heuristic Training ===");
        // The net is always BLUE, heuristic is GREEN
        Player netPlayer = new MinimaxAgent(
                Color.BLUE, 21, 21, 1, 3, false,
                new NeuralNetworkEvaluator(trainer.getNetwork())
        );
        Player heuristicPlayer = new MinimaxAgent(
                Color.GREEN, 21, 21, 1, 3, false,
                new HeuristicEvaluator()
        );

        for (int gameIndex = 1; gameIndex <= totalGames; gameIndex++) {
            List<SelfPlayMinimaxTrainer.TrainingExample> data =
                    playOneGameCollectBlueStates(netPlayer, heuristicPlayer);

            // Train on the collected data from that single game
            double loss = trainer.trainAll(data);

            if (gameIndex % logInterval == 0) {
                System.out.printf("Game %d of %d completed. Loss: %.4f\n", 
                                  gameIndex, totalGames, loss);
            }
        }
        System.out.println("=== Training complete! ===");
    }

    /**
     * Plays a single game on a new TakGame instance and collects training states
     * ONLY for the BLUE net (i.e. netPlayer).
     *
     * @param netPlayer       The MinimaxAgent (BLUE) powered by the neural net.
     * @param heuristicPlayer The MinimaxAgent (GREEN) with a heuristic.
     * @return A list of (input, target) pairs from BLUE's perspective only.
     */
    private List<SelfPlayMinimaxTrainer.TrainingExample> playOneGameCollectBlueStates(
            Player netPlayer,
            Player heuristicPlayer
    ) {
        TakGame game = new TakGame(boardSize, Arrays.asList(netPlayer, heuristicPlayer));
        List<SelfPlayMinimaxTrainer.TrainingExample> examples = new ArrayList<>();

        while (!game.isGameEnded()) {
            Player current = game.getCurrentPlayer();
            // We only collect states if the current player is the net (BLUE)
            if (current == netPlayer) {
                double[] input = BoardToInputsConverter.convert(game.getBoard(), netPlayer);
                // We put a dummy label for now; we fill real label after we know the winner
                double[] tmpLabel = new double[]{0.0};
                examples.add(new SelfPlayMinimaxTrainer.TrainingExample(input, tmpLabel));
            }

            try {
                current.makeMove(game);
            } catch (InvalidMoveException | GameOverException e) {
                // If there's an invalid move, we break out
                System.err.println("Error: " + e.getMessage());
                break;
            }
        }

        // Now label these states +1 if BLUE wins, -1 if BLUE loses, 0 if tie
        Player winner = game.getWinner();
        for (SelfPlayMinimaxTrainer.TrainingExample ex : examples) {
            if (winner == null) {
                ex.setTarget(new double[]{0.0});
            } else if (winner == netPlayer) {
                ex.setTarget(new double[]{1.0});
            } else {
                ex.setTarget(new double[]{-1.0});
            }
        }
        return examples;
    }
}
