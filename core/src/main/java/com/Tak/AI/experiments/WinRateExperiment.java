package com.Tak.AI.experiments;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.exceptions.GameOverException;

import java.util.ArrayList;

public class WinRateExperiment {
    private static final int NUM_GAMES = 1;
    private static final int BOARD_SIZE = 5;
    private static final int MAX_DEPTH = 3;
    private static final int INITIAL_FLAT_STONES = 10;
    private static final int INITIAL_CAPSTONES = 1;

    public static class WinRateResults {
        double randomFirstWinRate;
        double minimaxFirstWinRate;
    }

    public static WinRateResults runExperiment() {
        WinRateResults results = new WinRateResults();
        int randomFirstWins = 0;
        int minimaxFirstWins = 0;

        for (int game = 0; game < NUM_GAMES; game++) {
            // Setup the first game (Random AI starts)
            Player randomFirst = new RandomAIPlayer(Player.Color.BLUE, INITIAL_FLAT_STONES, 0, INITIAL_CAPSTONES);
            Player minimaxSecond = new MinimaxAgent(Player.Color.GREEN, INITIAL_FLAT_STONES, 0, INITIAL_CAPSTONES, MAX_DEPTH);

            TakGame gameRandom = new TakGame(BOARD_SIZE, true, 2); // 2 AI players for this game
            gameRandom.addPlayer(randomFirst);
            gameRandom.addPlayer(minimaxSecond);

            // Setup the second game (Minimax AI starts)
            Player minimaxFirst = new MinimaxAgent(Player.Color.BLUE, INITIAL_FLAT_STONES, 0, INITIAL_CAPSTONES, MAX_DEPTH);
            Player randomSecond = new RandomAIPlayer(Player.Color.GREEN, INITIAL_FLAT_STONES, 0, INITIAL_CAPSTONES);

            TakGame gameMinimax = new TakGame(BOARD_SIZE, true, 2); // 2 AI players for this game
            gameMinimax.addPlayer(minimaxFirst);
            gameMinimax.addPlayer(randomSecond);

            try {
                playGame(gameRandom);
                playGame(gameMinimax);

                if (gameRandom.getWinner() == randomFirst) randomFirstWins++;
                if (gameMinimax.getWinner() == minimaxFirst) minimaxFirstWins++;
            } catch (GameOverException | InvalidMoveException e) {
                System.err.println("Game error: " + e.getMessage());
            }
        }

        // Calculate win rates
        results.randomFirstWinRate = (double) randomFirstWins / (NUM_GAMES / 2) * 100;
        results.minimaxFirstWinRate = (double) minimaxFirstWins / (NUM_GAMES / 2) * 100;

        return results;
    }

    private static void playGame(TakGame game) throws GameOverException, InvalidMoveException {
        while (!game.isGameEnded()) {
            Player currentPlayer = game.getCurrentPlayer();
            if (currentPlayer instanceof RandomAIPlayer) {
                ((RandomAIPlayer) currentPlayer).makeMove(game);
            } else if (currentPlayer instanceof MinimaxAgent) {
                ((MinimaxAgent) currentPlayer).makeMove(game);
            }
        }
    }

    public static void main(String[] args) {
        WinRateResults results = runExperiment();

        System.out.printf("Random AI First Win Rate: %.2f%%\n", results.randomFirstWinRate);
        System.out.printf("Minimax AI First Win Rate: %.2f%%\n", results.minimaxFirstWinRate);
    }
}
