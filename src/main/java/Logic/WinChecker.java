package Logic;

/**
 * The WinChecker class is responsible for determining if a player has won the game.
 * It checks for road wins and flat wins.
 */
public class WinChecker {

    /**
     * Checks whether the current player has achieved a road win.
     * A road win occurs when a continuous line of flat stones or capstones
     * connects two opposite sides of the board (horizontally or vertically).
     *
     * @param player The player whose road win is being checked.
     * @param board  The current state of the board.
     * @return true if the player has achieved a road win, false otherwise.
     */
    public boolean checkForRoadWin(Player player, Board board) {
        int size = board.getSize();
        for (int y = 0; y < size; y++) {
            boolean[][] visited = new boolean[size][size];
            if (dfsRoad(player, board, 0, y, visited, true)) {
                return true;
            }
        }
        for (int x = 0; x < size; x++) {
            boolean[][] visited = new boolean[size][size];
            if (dfsRoad(player, board, x, 0, visited, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs a depth-first search to find a continuous road.
     *
     * @param player       The player.
     * @param board        The game board.
     * @param x            Current X coordinate.
     * @param y            Current Y coordinate.
     * @param visited      2D array to keep track of visited positions.
     * @param isHorizontal true for horizontal road, false for vertical.
     * @return true if a road is found, false otherwise.
     */
    private boolean dfsRoad(Player player, Board board, int x, int y, boolean[][] visited, boolean isHorizontal) {
        if (!board.isWithinBounds(x, y) || visited[x][y]) {
            return false;
        }
        Piece piece = board.getPieceAt(x, y);
        if (piece == null || piece.getOwner() != player || !piece.canBePartOfRoad()) {
            return false;
        }
        visited[x][y] = true;
        int size = board.getSize();

        if (isHorizontal && x == size - 1) {
            return true;
        }
        if (!isHorizontal && y == size - 1) {
            return true;
        }
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] dir : directions) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (dfsRoad(player, board, nx, ny, visited, isHorizontal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a flat win has been achieved.
     * A flat win occurs when the board is full and no road win has been achieved.
     * The player with the most flat stones on top of stacks wins.
     *
     * @param game The current game instance.
     * @return The player who won by flat count, or null if no flat win.
     */
    public Player checkForFlatWin(TakGame game) {
        Board board = game.getBoard();
        if (board.isBoardFull()) {
            Player player1 = game.getPlayer1();
            Player player2 = game.getPlayer2();
            int player1FlatStones = countFlatStones(player1, board);
            int player2FlatStones = countFlatStones(player2, board);
            if (player1FlatStones > player2FlatStones) {
                return player1;
            } else if (player2FlatStones > player1FlatStones) {
                return player2;
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Counts the number of flat stones owned by the player on top of stacks.
     *
     * @param player The player.
     * @param board  The game board.
     * @return The count of flat stones.
     */
    private int countFlatStones(Player player, Board board) {
        int flatStoneCount = 0;
        int size = board.getSize();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Piece piece = board.getPieceAt(x, y);
                if (piece != null && piece.getOwner() == player && piece.getPieceType() == Piece.PieceType.FLAT_STONE) {
                    flatStoneCount++;
                }
            }
        }
        return flatStoneCount;
    }
}
