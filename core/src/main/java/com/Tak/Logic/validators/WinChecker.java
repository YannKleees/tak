package com.Tak.Logic.validators;

import com.Tak.AI.utils.RoadConnectivity;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.Player;
import com.Tak.Logic.utils.Logger;

import java.util.List;

/**
 * The WinChecker class is responsible for determining if a player has won the game.
 * It checks for Road Wins, Flat Wins, and "no moves left" scenarios.
 *
 * <p>Main endgame conditions:
 * <ul>
 *   <li><b>Road Win</b>: If any player has a continuous road connecting opposite sides,
 *       that player immediately wins.</li>
 *   <li><b>Flat Win</b>: If the board is full OR if all players have no legal moves,
 *       the winner is determined by who has the most visible (top) flat stones.
 *       If flats are tied, it’s a tie (returns null).</li>
 * </ul>
 */
public class WinChecker {

    private final RoadConnectivity roadChecker;

    public WinChecker() {
        this.roadChecker = new RoadConnectivity();
    }

    /**
     * Performs a complete check for a winner given the board and players:
     *
     * 1) Check if any player has a road win. If so, returns that player immediately.
     * 2) If no road, check if the board is full OR all players have no moves:
     *      → If so, decide by visible flats. If there's a single highest count, that player wins.
     *        If there's a tie, return null.
     * 3) Otherwise, return null (no winner yet).
     *
     * @param board   The current board state
     * @param players The list of all players
     * @return The winning Player, or null if no winner or it's a tie on flats.
     */
    public Player checkForWin(Board board, List<Player> players) {
        // 1) Road check
        for (Player player : players) {
            if (checkForRoadWin(player, board)) {
                return player; // immediate road winner
            }
        }

        // 2) If no road, check if board is full OR no moves remain for every player
        if (board.isFull() || allPlayersHaveNoMoves(players, board)) {
            // Decide by flat count
            return getTopPlayer(board, players);  // could be null if tie
        }

        // No winner yet
        return null;
    }

    /**
     * Checks whether the specified player has a road win.
     */
    public boolean checkForRoadWin(Player player, Board board) {
        boolean hasRoad = roadChecker.checkForRoadWin(player, board);
        if (hasRoad) {
            //Logger.log("WinChecker", "Player " + player.getColor() + " has a road win!");
        }
        return hasRoad;
    }

    /**
     * Determines if all players have zero legal moves left.
     *
     * @param players The list of players
     * @param board   The current board
     * @return true if *every* player cannot make a move, false otherwise
     */
    private boolean allPlayersHaveNoMoves(List<Player> players, Board board) {
        for (Player p : players) {
            if (p.canMakeAMove(board)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines which player has the most visible flat stones.
     * If tied, returns null.
     * Normally called when board is full or no players can move.
     *
     * @param board   The current board
     * @param players The list of players
     * @return The player with the highest visible flat stone count, or null if tie
     */
    public Player getTopPlayer(Board board, List<Player> players) {
        // Count visible flats for each player
        Player topPlayer = null;
        int maxFlats = -1;
        boolean tie = false;

        for (Player player : players) {
            int flats = countFlatStones(player, board);
            if (flats > maxFlats) {
                maxFlats = flats;
                topPlayer = player;
                tie = false;
            } else if (flats == maxFlats) {
                tie = true; // at least two players have the same max count
            }
        }

        return tie ? null : topPlayer;
    }

    /**
     * Counts how many visible flat stones (top piece in each stack) this player owns.
     * Capstones or walls (standing stones) do not count as flat stones.
     */
    private int countFlatStones(Player player, Board board) {
        int flatStoneCount = 0;
        int size = board.getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Piece topPiece = board.getPieceAt(x, y);
                if (topPiece != null
                    && topPiece.getOwner().equals(player)
                    && topPiece.getPieceType() == Piece.PieceType.FLAT_STONE) {
                    flatStoneCount++;
                }
            }
        }
        return flatStoneCount;
    }
}
