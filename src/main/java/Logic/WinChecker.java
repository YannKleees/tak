package Logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

/**
 * The WinChecker class is responsible for determining whether the game has reached
 * a terminal state based on road win or flat win conditions. It includes methods to
 * check for a road win, flat win, and to assess whether the game has ended.
 */
public class WinChecker {

    /**
     * Default constructor for the WinChecker class.
     */
    public WinChecker() {
        // Initialization code if needed
    }


    /**
     * Checks whether the current player has achieved a road win.
     * A road win occurs when a continuous line of flat stones or capstones 
     * connects two opposite sides of the board (horizontally or vertically).
     *
     * @param player The player whose road win is being checked.
     * @param board The current state of the board.
     * @return true if the player has achieved a road win, false otherwise.
     */
    public boolean checkForRoadWin(Player player, Board board) {
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                if (isValidRoad(x, y, player, board)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Checks whether a flat win has been achieved.
     * A flat win occurs when the board is full and no road win has been achieved.
     * The player with the most flat stones on top of stacks wins.
     *
     * @param board The current state of the board.
     * @return true if a flat win has been achieved, false otherwise.
     */
    public boolean checkForFlatWin(TakGame game) {
        Board board = game.getBoard();
        if (board.isBoardFull()) {
            Player player1 = game.getPlayer1();
            Player player2 = game.getPlayer2();
            int player1FlatStones = countFlatStones(player1, board);
            int player2FlatStones = countFlatStones(player2, board);
            return player1FlatStones != player2FlatStones;
        }
        return false;
    }


    /**
     * Determines if the game has reached a terminal state (either road win or flat win).
     *
     * @param player The current player.
     * @param board The current state of the board.
     * @return true if the game is over (either by road win or flat win), false otherwise.
     */
    public boolean isGameOver(TakGame game) {
        Board board = game.getBoard();
        return checkForRoadWin(game.getCurrentPlayer(), board) || checkForFlatWin(game);
    }


    /**
     * Counts the number of flat stones owned by the player that are on top of stacks
     * on the board. Only flat stones on top of stacks are counted for flat win purposes.
     *
     * @param player The player whose flat stones are being counted.
     * @param board The current state of the board.
     * @return The number of flat stones owned by the player on top of stacks.
     */
    private int countFlatStones(Player player, Board board) {
        int flatStoneCount = 0;
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Piece piece = board.getPieceAt(x, y);
                if (piece != null && piece.getOwner() == player && piece.getPieceType() == Piece.PieceType.FLAT_STONE) {
                    flatStoneCount++;
                }
            }
        }
        return flatStoneCount;
    }


    /**
     * Helper function to check if a given path on the board is a valid road.
     * Uses Depth-First Search (DFS) to find if a continuous road of flat stones 
     * or capstones exists that connects opposite sides of the board.
     *
     * @param startX The starting X coordinate of the potential road.
     * @param startY The starting Y coordinate of the potential road.
     * @param player The player whose road is being checked.
     * @param board The current state of the board.
     * @return true if a valid road exists from the given starting point, false otherwise.
     */
    private boolean isValidRoad(int startX, int startY, Player player, Board board) {
        Piece startPiece = board.getPieceAt(startX, startY);
        if (startPiece == null || startPiece.getOwner() != player || !startPiece.canBePartOfRoad()) {
            return false;
        }
        boolean isHorizontal = startY == 0; 
        Stack<int[]> stack = new Stack<>();
        HashSet<String> visited = new HashSet<>();
        stack.push(new int[] { startX, startY });
        visited.add(startX + "," + startY);
        while (!stack.isEmpty()) {
            int[] position = stack.pop();
            int x = position[0];
            int y = position[1];
            if (hasReachedOppositeSide(x, y, board.getSize(), isHorizontal)) {
                return true;
            }
            for (int[] neighbor : getNeighbors(x, y, board)) {
                int nx = neighbor[0];
                int ny = neighbor[1];
                Piece neighborPiece = board.getPieceAt(nx, ny);
    
                if (neighborPiece != null && neighborPiece.getOwner() == player && neighborPiece.canBePartOfRoad()) {
                    String neighborKey = nx + "," + ny;
                    if (!visited.contains(neighborKey)) {
                        stack.push(new int[] { nx, ny });
                        visited.add(neighborKey);
                    }
                }
            }
        }
        return false;
    }


    /**
     * Helper function to determine if a given position has reached the opposite side of the board.
     * This will depend on whether the player is attempting to build a horizontal or vertical road.
     *
     * @param x The X coordinate of the current position.
     * @param y The Y coordinate of the current position.
     * @param boardSize The size of the board.
     * @param isHorizontal true if the player is attempting to build a horizontal road, false if vertical.
     * @return true if the position is on the opposite side of the board, false otherwise.
     */
    private boolean hasReachedOppositeSide(int x, int y, int boardSize, boolean isHorizontal) {
        if (isHorizontal) {
            // Horizontal road: Check if the piece has reached the rightmost column
            return y == boardSize - 1;
        } else {
            // Vertical road: Check if the piece has reached the bottommost row
            return x == boardSize - 1;
        }
    }


    /**
     * Helper function to get all adjacent positions (up, down, left, right) of a given position.
     *
     * @param x The X coordinate of the current position.
     * @param y The Y coordinate of the current position.
     * @param board The current state of the board.
     * @return A list of int[] representing adjacent coordinates.
     */
    private List<int[]> getNeighbors(int x, int y, Board board) {
        List<int[]> neighbors = new ArrayList<>();
        if (x > 0) neighbors.add(new int[] { x - 1, y });
        if (x < board.getSize() - 1) neighbors.add(new int[] { x + 1, y });
        if (y > 0) neighbors.add(new int[] { x, y - 1 });
        if (y < board.getSize() - 1) neighbors.add(new int[] { x, y + 1 });
        return neighbors;
    }
}
