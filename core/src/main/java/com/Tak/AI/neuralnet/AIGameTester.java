package com.Tak.AI.neuralnet;

import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;

import java.util.List;

/**
 * Demonstrates a quick test: loads a trained network and runs one Tak game with AI players.
 */
public class AIGameTester {
    public static void main(String[] args) {
        // Initialize the trainer (default hidden layer = 20, TANH activation)
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        nnTrainer.loadNetwork("networks/RELU_128_64.txt"); // Load your previously trained network

        // Initialize AI players
        List<Player> aiPlayers = AIPlayerInitializer.createAIPlayers(nnTrainer);

        // Create a TakGame with AI players
        int boardSize = 5;
        TakGame game = new TakGame(boardSize, aiPlayers);

        // Play the game
        while (!game.isGameEnded()) {
            try {
                game.getCurrentPlayer().makeMove(game);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        // Announce the winner
        Player winner = game.getWinner();
        if (winner != null) {
            System.out.println("Winner: " + winner.getColor());
        } else {
            System.out.println("The game ended in a tie.");
        }

        // Optionally, log the final board state
        game.logFinalBoardState();
    }
}
