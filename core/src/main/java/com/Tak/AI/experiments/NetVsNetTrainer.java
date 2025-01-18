package com.Tak.AI.experiments;

import com.Tak.AI.neuralnet.*;
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
 * Demonstrates training two distinct neural networks: 
 *   - netTrainerA controlling BLUE
 *   - netTrainerB controlling GREEN
 * They each collect and label only their own states, then update.
 */
public class NetVsNetTrainer {

    private final NeuralNetworkTrainer netTrainerA; // for BLUE
    private final NeuralNetworkTrainer netTrainerB; // for GREEN
    private final int boardSize;
    private final int totalGames;
    private final int logInterval;

    public NetVsNetTrainer(NeuralNetworkTrainer netTrainerA,
                           NeuralNetworkTrainer netTrainerB,
                           int boardSize,
                           int totalGames,
                           int logInterval) {
        this.netTrainerA = netTrainerA;
        this.netTrainerB = netTrainerB;
        this.boardSize = boardSize;
        this.totalGames = totalGames;
        this.logInterval = logInterval;
    }

    public void runTraining() {
        System.out.println("\n=== Starting Net-vs-Net Training ===");
        // Create the MinimaxAgents for each color
        Player netAPlayer = new MinimaxAgent(
                Color.BLUE, 21, 21, 1, 3, false,
                new NeuralNetworkEvaluator(netTrainerA.getNetwork())
        );
        Player netBPlayer = new MinimaxAgent(
                Color.GREEN, 21, 21, 1, 3, false,
                new NeuralNetworkEvaluator(netTrainerB.getNetwork())
        );

        for (int i = 1; i <= totalGames; i++) {
            // Play a single game, collecting states from both sides
            GameData data = playOneGameCollectStates(netAPlayer, netBPlayer);

            // Train net A on the states it collected
            double lossA = netTrainerA.trainAll(data.dataA);
            // Train net B on the states it collected
            double lossB = netTrainerB.trainAll(data.dataB);

            if (i % logInterval == 0) {
                System.out.printf("Game %d / %d - Loss(A)=%.4f, Loss(B)=%.4f\n",
                                  i, totalGames, lossA, lossB);
            }
        }

        System.out.println("=== Net-vs-Net Training done! ===");
    }

    /**
     * Plays a single game on a new board, collecting states for each net separately.
     * BLUE = netAPlayer, GREEN = netBPlayer.
     */
    private GameData playOneGameCollectStates(Player netAPlayer, Player netBPlayer) {
        TakGame game = new TakGame(boardSize, Arrays.asList(netAPlayer, netBPlayer));

        List<SelfPlayMinimaxTrainer.TrainingExample> aStates = new ArrayList<>();
        List<SelfPlayMinimaxTrainer.TrainingExample> bStates = new ArrayList<>();

        while (!game.isGameEnded()) {
            Player current = game.getCurrentPlayer();
            double[] input = BoardToInputsConverter.convert(game.getBoard(), current);

            // If it's BLUE, record the state in A's dataset
            if (current == netAPlayer) {
                double[] tmpLabel = new double[]{0.0};
                aStates.add(new SelfPlayMinimaxTrainer.TrainingExample(input, tmpLabel));
            }
            // If it's GREEN, record the state in B's dataset
            else {
                double[] tmpLabel = new double[]{0.0};
                bStates.add(new SelfPlayMinimaxTrainer.TrainingExample(input, tmpLabel));
            }

            // Actually make the move
            try {
                current.makeMove(game);
            } catch (InvalidMoveException | GameOverException e) {
                System.err.println("Error: " + e.getMessage());
                break;
            }
        }

        // Now label each side: +1 if that side is the winner, -1 if it lost, 0 if tie
        Player winner = game.getWinner();
        for (SelfPlayMinimaxTrainer.TrainingExample ex : aStates) {
            if (winner == null) {
                ex.setTarget(new double[]{0.0});
            } else if (winner == netAPlayer) {
                ex.setTarget(new double[]{1.0});
            } else {
                ex.setTarget(new double[]{-1.0});
            }
        }
        for (SelfPlayMinimaxTrainer.TrainingExample ex : bStates) {
            if (winner == null) {
                ex.setTarget(new double[]{0.0});
            } else if (winner == netBPlayer) {
                ex.setTarget(new double[]{1.0});
            } else {
                ex.setTarget(new double[]{-1.0});
            }
        }
        return new GameData(aStates, bStates);
    }

    /** Helper struct for returning both players' data from the game. */
    private static class GameData {
        List<SelfPlayMinimaxTrainer.TrainingExample> dataA;
        List<SelfPlayMinimaxTrainer.TrainingExample> dataB;

        GameData(List<SelfPlayMinimaxTrainer.TrainingExample> a,
                 List<SelfPlayMinimaxTrainer.TrainingExample> b) {
            this.dataA = a;
            this.dataB = b;
        }
    }
}
