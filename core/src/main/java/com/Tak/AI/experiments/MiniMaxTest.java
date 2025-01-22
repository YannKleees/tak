package com.Tak.AI.experiments;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player.Color;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;

import java.util.List;
import java.util.ArrayList;

public class MiniMaxTest {

    public static void gameAI() {
        // Set up a basic board scenario
        int boardSize = 5; // You can adjust this for larger or smaller boards
        int currentPlayerIndex = 0;

        // Initialize players
        Player minimaxPlayer = new MinimaxAgent(Color.GREEN, 21, 1, 1, 3, true); // Example piece counts
        Player randomPlayer = new RandomAIPlayer(Color.BLUE, 21, 1, 1);

        List<Player> players = new ArrayList<>();
        players.add(randomPlayer);
        players.add(minimaxPlayer);

        // Set opponents
        minimaxPlayer.setOpponent(randomPlayer);
        randomPlayer.setOpponent(minimaxPlayer);
        int moveCount = 0;

        TakGame game = new TakGame(boardSize, players);

        System.out.println("MiniMaxTest: Game initialized. Starting game loop.");
        System.out.println("MiniMaxTest: Board size: " + boardSize);
        System.out.println("MiniMaxTest: Players: Minimax (GREEN), Random (BLUE)");

        while (!game.isGameEnded()) {
            try {
                System.out.println("MiniMaxTest: Turn: " + moveCount);
                Player currentPlayer = game.getCurrentPlayer();
                System.out.println("MiniMaxTest: Current player: " + currentPlayer.getColor());

                System.out.println("MiniMaxTest: Making move...");
                currentPlayer.makeMove(game);

                moveCount++;
                System.out.println("MiniMaxTest: Move made successfully. Switching player.");

                Board board = game.getBoard();
                board.printBoard();
                System.out.println("MiniMaxTest: Board state logged after move " + moveCount + "\n");

            } catch (InvalidMoveException e) {
                System.err.println("MiniMaxTest: InvalidMoveException: " + e.getMessage() + "\n");
                break;
            } catch (GameOverException e) {
                System.out.println("MiniMaxTest: GameOverException: " + e.getMessage() + "\n");
                break;
            } catch (Exception e) {
                System.err.println("MiniMaxTest: Unexpected exception: " + e.getMessage() + "\n");
                break;
            }
        }

        // Determine the winner and update counts
        Player winner = game.getWinner();
        if (winner == null) {
            System.out.println("MiniMaxTest: The game ended in a draw.");
        } else {
            System.out.println("MiniMaxTest: Winner: " + (winner == minimaxPlayer ? "Minimax AI" : "Random AI"));
        }
        Board finalBoard = game.getBoard();
        finalBoard.printBoard();
        System.out.println("MiniMaxTest: Final Board State:");
        System.out.println("MiniMaxTest: Total moves: " + moveCount);

        // Print metrics
        System.out.println("Player1 (MinimaxAgent) Nodes Evaluated: " + ((MinimaxAgent) minimaxPlayer).getNodesEvaluated());
        System.out.println("Player1 (MinimaxAgent) Time Taken: " + ((MinimaxAgent) minimaxPlayer).getTimeTakenMillis() + "ms");
        System.out.println("Player1 (MinimaxAgent) Pruning Events: " + ((MinimaxAgent) minimaxPlayer).getPruneCount());

        // RandomAIPlayer does not track metrics
        System.out.println("Player2 (RandomAIPlayer) Metrics: N/A");
    }

    public static void main(String[] args) {
        gameAI();
    }
}
