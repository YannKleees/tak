package com.Tak.AI.neuralnet;

import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;

import java.util.List;

public class AIGameTester {
    public static void main(String[] args) {
        // Initialize the trainer and load the network
        NeuralNetworkTrainer nnTrainer = NeuralNetworkInitializer.initializeTrainer();
        nnTrainer.loadNetwork("trained_network.txt"); // Load your trained network

        // Initialize AI players
        List<Player> aiPlayers = AIPlayerInitializer.createAIPlayers(nnTrainer);

        // Create a TakGame with AI players
        int boardSize = 5;
        TakGame game = new TakGame(boardSize, aiPlayers);

        // Play the game until it ends
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
