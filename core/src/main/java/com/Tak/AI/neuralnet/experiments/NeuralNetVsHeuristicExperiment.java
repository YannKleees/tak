package com.Tak.AI.neuralnet.experiments;
import com.Tak.AI.evaluation.IEvaluationFunction;
import com.Tak.AI.evaluation.HeuristicEvaluator;
import com.Tak.AI.neuralnet.net.NeuralNetworkEvaluator;
import com.Tak.AI.neuralnet.trainer.NeuralNetworkTrainer;
import com.Tak.AI.players.MinimaxAgent;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
/**
 * Runs experiments comparing the Neural Network-based AI against a heuristic-based AI.
 * It plays a series of games and reports the number of wins, losses, and ties.
 */
public class NeuralNetVsHeuristicExperiment {

    private NeuralNetworkEvaluator nnEvaluator;
    private IEvaluationFunction heuristicEvaluator;
    private NeuralNetworkTrainer nnTrainer;
    private int experimentGames; // Number of games per experiment

    /**
     * Constructs the experiment with specified evaluators, trainer, and number of games.
     *
     * @param nnEvaluator        The neural network evaluator.
     * @param heuristicEvaluator The heuristic evaluator.
     * @param nnTrainer          The neural network trainer.
     * @param experimentGames    The number of games to run in the experiment.
     */
    public NeuralNetVsHeuristicExperiment(NeuralNetworkEvaluator nnEvaluator,
                                         IEvaluationFunction heuristicEvaluator,
                                         NeuralNetworkTrainer nnTrainer,
                                         int experimentGames) {
        this.nnEvaluator = nnEvaluator;
        this.heuristicEvaluator = heuristicEvaluator;
        this.nnTrainer = nnTrainer;
        this.experimentGames = experimentGames;
    }

    /**
     * Runs the experiment and prints out the results.
     *
     * @throws IOException If any I/O error occurs.
     */
    public void run() throws IOException {
        int nnWins = 0;
        int heuristicWins = 0;
        int ties = 0;

        for (int i = 1; i <= experimentGames; i++) {
            TakGame game = createExperimentGame();
            try {
                while (!game.isGameEnded()) {
                    Player current = game.getCurrentPlayer();
                    current.makeMove(game);
                }
            } catch (InvalidMoveException | GameOverException e) {
                System.out.println("Error during game " + i + ": " + e.getMessage());
                continue;
            }

            Player winner = game.getWinner();
            if (winner == null) {
                ties++;
            } else if (isNeuralNetPlayer(winner)) {
                nnWins++;
            } else {
                heuristicWins++;
            }

            System.out.println("Game " + i + " completed. Winner: " + (winner != null ? winner.getColor() : "Tie"));
        }

        // Report results
        System.out.println("\n=== Experiment Results ===");
        System.out.println("Neural Network AI Wins: " + nnWins);
        System.out.println("Heuristic AI Wins: " + heuristicWins);
        System.out.println("Ties: " + ties);
        System.out.println("==========================");
    }

    /**
     * Creates a TakGame instance with one NeuralNet-based AI and one heuristic-based MinimaxAgent for validation.
     *
     * @return A new TakGame instance configured for validation.
     */
    private TakGame createExperimentGame() {
        Player nnPlayer = new MinimaxAgent(
                Color.BLUE, 21, 21, 1, 3, false,
                nnEvaluator
        );

        Player heuristicPlayer = new MinimaxAgent(
                Color.GREEN, 21, 21, 1, 3, false,
                heuristicEvaluator
        );

        List<Player> players = Arrays.asList(nnPlayer, heuristicPlayer);
        TakGame game = new TakGame(5, players);
        return game;
    }

    /**
     * Determines if the given player is the Neural Network-based AI.
     *
     * @param player The player to check.
     * @return True if the player is the Neural Network AI, false otherwise.
     */
    private boolean isNeuralNetPlayer(Player player) {
        return player.getColor() == Color.BLUE; // Assuming NN is BLUE
    }
}
