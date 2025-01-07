//package com.Tak.AI.Test;
//
//import com.Tak.Logic.exceptions.GameOverException;
//import com.Tak.Logic.exceptions.InvalidMoveException;
//import com.Tak.Logic.models.Player;
//import com.Tak.Logic.models.TakGame;
//import com.Tak.Logic.utils.Logger;
//import com.Tak.Logic.models.Player.Color;
//
//import java.util.List;
//import java.util.ArrayList;
//
///**
// * AITest class to train and test AIPlayers against each other.
// */
//public class AITest {
//    public static void main(String[] args) {
//        // Initialize TakGame with two AIPlayers (AI vs AI)
//        int boardSize = 5;
//
//        // Initialize AIPlayers with standard Tak piece counts
//        QPlayer aiPlayer1 = new QPlayer(Color.BLUE, 21, 0, 1, true);
//        QPlayer aiPlayer2 = new QPlayer(Color.GREEN, 21, 0, 1, true);
//
//        List<Player> players = new ArrayList<>();
//        players.add(aiPlayer1);
//        players.add(aiPlayer2);
//
//        // Set opponents for AIPlayers
//        aiPlayer1.setOpponent(aiPlayer2);
//        aiPlayer2.setOpponent(aiPlayer1);
//
//        // Use the constructor that accepts a list of players
//        TakGame game = new TakGame(boardSize, players);
//
//        int numGames = 5000; // Number of games for training
//        int BLUEWins = 0;
//        int GREENWins = 0;
//        int draws = 0;
//
//        long startTime = System.currentTimeMillis(); // Track the start time
//
//        for (int i = 0; i < numGames; i++) {
//            game.resetGame(true); // Reset both board and scores
//
//            while (!game.isGameEnded()) {
//                Player currentPlayer = game.getCurrentPlayer();
//                try {
//                    currentPlayer.makeMove(game);
//                } catch (InvalidMoveException | GameOverException e) {
//                    Logger.log("AITest", "Exception encountered: " + e.getMessage());
//                    break;
//                }
//            }
//
//            // Determine the winner and update counts
//            Player winner = game.getWinner();
//            if (winner != null) {
//                if (winner.getColor() == Color.BLUE) {
//                    BLUEWins++;
//                    aiPlayer1.getQLearningAgent().updateQTableAfterGame(100.0); // AIPlayer1 wins
//                    aiPlayer2.getQLearningAgent().updateQTableAfterGame(-100.0); // AIPlayer2 loses
//                } else if (winner.getColor() == Color.GREEN) {
//                    GREENWins++;
//                    aiPlayer1.getQLearningAgent().updateQTableAfterGame(-100.0); // AIPlayer1 loses
//                    aiPlayer2.getQLearningAgent().updateQTableAfterGame(100.0); // AIPlayer2 wins
//                }
//            } else {
//                draws++;
//                // Update Q-Table for a draw
//                aiPlayer1.getQLearningAgent().updateQTableAfterGame(0.0);
//                aiPlayer2.getQLearningAgent().updateQTableAfterGame(0.0);
//            }
//
//            // Decay exploration rate after each game
//            aiPlayer1.decayExplorationRate();
//            aiPlayer2.decayExplorationRate();
//
//            // Save AI States periodically
//            if ((i + 1) % 100 == 0) {
//                aiPlayer1.saveState();
//                aiPlayer2.saveState();
//                // Optionally, print progress
//                System.out.println("Progress after " + (i + 1) + " games:");
//                System.out.println("BLUE Wins: " + BLUEWins);
//                System.out.println("GREEN Wins: " + GREENWins);
//                System.out.println("Draws: " + draws);
//            }
//        }
//
//        // Final Q-Table save
//        aiPlayer1.saveState();
//        aiPlayer2.saveState();
//
//        long endTime = System.currentTimeMillis(); // Track the end time
//        long totalTime = endTime - startTime;
//        double seconds = totalTime / 1000.0;
//
//        // Final summary
//        System.out.println("All training games completed in " + seconds + " seconds.");
//        System.out.println("Final Results after " + numGames + " games:");
//        System.out.println("BLUE Wins: " + BLUEWins);
//        System.out.println("GREEN Wins: " + GREENWins);
//        System.out.println("Draws: " + draws);
//
//        // **//TODO:** Implement functionality to evaluate and display AI performance metrics
//    }
//}
//
