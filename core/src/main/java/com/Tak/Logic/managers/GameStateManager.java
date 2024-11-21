package com.Tak.Logic.managers;

import java.util.List;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.validators.WinChecker;
import com.Tak.Logic.utils.Logger;

/**
 * Manages the state of the game, including win condition checks.
 */
public class GameStateManager {
    private Board board;
    private List<Player> players;
    private boolean isGameOver;
    private Player winner;

    public GameStateManager(Board board, List<Player> players) {
        this.board = board;
        this.players = players;
        this.isGameOver = false;
        this.winner = null;
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if the game is over, false otherwise.
     */
    public boolean isGameOver() {
        if (isGameOver) {
            return true;
        }

        WinChecker winChecker = new WinChecker();

        // 1. Check for road wins
        for (Player player : players) {
            if (winChecker.checkForRoadWin(player, board)) {
                isGameOver = true;
                winner = player;
                //Logger.log("GameStateManager", "Player " + player.getColor() + " has achieved a road win.");
                return true;
            }
        }

        // 2. Check if the board is full
        if (board.isFull()) {
            Player topPlayer = winChecker.getTopPlayer(board, players); // Pass players list
            if (topPlayer != null) {
                isGameOver = true;
                winner = topPlayer;
                //Logger.log("GameStateManager", "Board is full. Player " + topPlayer.getColor() + " wins by majority of flat stones.");
            } else {
                isGameOver = true;
                winner = null; // Tie
                //Logger.log("GameStateManager", "Board is full. The game is a tie.");
            }
            return true;
        }

        // 3. No game over conditions met
        return false;
    }

    /**
     * Returns whether the game has ended.
     *
     * @return true if the game has ended, false otherwise.
     */
    public boolean hasGameEnded() {
        return isGameOver;
    }

    /**
     * Returns the winner of the game.
     *
     * @return The winning Player, or null if it's a tie.
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Resets the game state.
     */
    public void resetGame() {
        this.isGameOver = false;
        this.winner = null;
        this.board.reset(); // Assuming Board has a method to reset its state
        //Logger.log("GameStateManager", "Game state has been reset.");
    }
}
