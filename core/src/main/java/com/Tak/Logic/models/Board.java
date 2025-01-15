package com.Tak.Logic.models;

import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the game board for Tak.
 */
public class Board {
    private final int size;
    private final int carryLimit;
    protected final PieceStack[][] board;
    private List<Player> players;
    private int moveCount;

    /**
     * Constructor to initialize the board with a given size.
     *
     * @param size       The size of the board (e.g., 5 for a 5x5).
     * @param carryLimit The maximum number of pieces that can be carried in a move.
     */
    public Board(int size, int carryLimit) {
        this.size = size;
        this.carryLimit = carryLimit;
        board = new PieceStack[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                board[x][y] = new PieceStack();
            }
        }
        this.players = new ArrayList<>();
        this.moveCount = 0;
    }

    /** Sets the list of players for this board. */
    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    /** Gets the list of players. */
    public List<Player> getPlayers() {
        return this.players;
    }

    public Piece getPieceAt(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[x][y].getTopPiece();
    }

    public int getStackSize(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[x][y].size();
    }

    public PieceStack getBoardStack(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[x][y];
    }

    public boolean isWithinBounds(int x, int y) {
        return (x >= 0 && x < size && y >= 0 && y < size);
    }

    public int getCarryLimit() {
        return carryLimit;
    }

    public void placePiece(int x, int y, Piece piece) throws InvalidMoveException {
        if (!isWithinBounds(x, y)) {
            throw new InvalidMoveException("Board position out of bounds.");
        }
        PieceStack stack = board[x][y];
        if (stack.isEmpty()) {
            stack.addPiece(piece);
        } else {
            throw new InvalidMoveException("Cannot place " + piece.getPieceType()
                + " on occupied cell at (" + x + ", " + y + ").");
        }
    }

    public List<Piece> removePieces(int x, int y, int amount) throws InvalidMoveException {
        if (!isWithinBounds(x, y)) {
            throw new InvalidMoveException("Board position out of bounds.");
        }
        return board[x][y].removePieces(amount);
    }

    /** Checks if the entire board is full (no empty stacks). */
    public boolean isFull() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board[x][y].isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Resets the board to its initial empty state. */
    public void reset() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                board[x][y].clear();
            }
        }
        moveCount = 0;
    }

    /**
     * Creates a deep copy of the current Board, creating new Player copies
     * so ownership references are mapped 1:1 from old to new players.
     *
     * If you want to re-use the same exact players (so AI references don't break),
     * use copy(true, samePlayers).
     */
    public Board copy() {
        return copy(false, null);
    }

    /**
     * Overloaded copy method to optionally reuse the same Player references.
     *
     * @param reusePlayers  If true, we'll use the same Player objects from samePlayers.
     * @param samePlayers   The external player list from TakGame, so piece owners remain the same.
     */
    public Board copy(boolean reusePlayers, List<Player> samePlayers) {
        Board newBoard = new Board(this.size, this.carryLimit);
        newBoard.moveCount = this.moveCount;

        if (!reusePlayers || samePlayers == null) {
            // Old approach: Create new copies of each player
            Map<Player, Player> playerMap = new HashMap<>();
            newBoard.players = new ArrayList<>();
            for (Player player : this.players) {
                Player playerCopy = player.copy();
                newBoard.players.add(playerCopy);
                playerMap.put(player, playerCopy);
            }
            // Reconnect opponents
            for (Player originalPlayer : this.players) {
                Player copiedPlayer = playerMap.get(originalPlayer);
                Player originalOpponent = originalPlayer.getOpponent();
                if (originalOpponent != null) {
                    Player copiedOpp = playerMap.get(originalOpponent);
                    copiedPlayer.setOpponent(copiedOpp);
                }
            }

            // Copy each stack using playerMap
            for (int x = 0; x < this.size; x++) {
                for (int y = 0; y < this.size; y++) {
                    PieceStack originalStack = this.board[x][y];
                    newBoard.board[x][y] = originalStack.copy(playerMap);
                }
            }
        } else {
            // Reuse the exact same Player objects => piece owners remain exactly the same
            newBoard.players = samePlayers;
            for (int x = 0; x < this.size; x++) {
                for (int y = 0; y < this.size; y++) {
                    // We'll do a shallow stack copy that keeps piece references intact
                    // so we don't lose the original owners
                    PieceStack originalStack = this.board[x][y];
                    newBoard.board[x][y] = originalStack.copyNoRemap();
                }
            }
        }

        return newBoard;
    }

    public Player getPlayerByColor(Player.Color color) {
        for (Player p : players) {
            if (p.getColor() == color) {
                return p;
            }
        }
        return null;
    }

    /** Basic rotate logic; you can adapt to reuse vs. new copies. */
    public Board rotateClockwise(int times) {
        times = times % 4;
        // For simplicity, do the old approach (fresh copies).
        Board rotated = this.copy(false, null);
        Map<Player, Player> playerMap = createPlayerMap(rotated, this.players);
        for (int t = 0; t < times; t++) {
            rotated = rotated.singleRotateClockwise(playerMap);
        }
        return rotated;
    }

    private Board singleRotateClockwise(Map<Player, Player> playerMap) {
        Board rotated = new Board(this.size, this.carryLimit);
        for (int x = 0; x < this.size; x++) {
            for (int y = 0; y < this.size; y++) {
                rotated.board[y][this.size - 1 - x] = this.board[x][y].copy(playerMap);
            }
        }
        rotated.moveCount = this.moveCount;
        rotated.players = new ArrayList<>(playerMap.values());
        return rotated;
    }

    private Map<Player, Player> createPlayerMap(Board rotatedBoard, List<Player> originalPlayers) {
        Map<Player, Player> playerMap = new HashMap<>();
        for (int i = 0; i < originalPlayers.size(); i++) {
            Player originalPlayer = originalPlayers.get(i);
            Player rotatedPlayer = rotatedBoard.players.get(i);
            playerMap.put(originalPlayer, rotatedPlayer);
        }
        return playerMap;
    }

    public int countPlayerPieces(Player player) {
        int count = 0;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                PieceStack stack = board[x][y];
                if (!stack.isEmpty()) {
                    Piece topPiece = stack.getTopPiece();
                    if (topPiece.getOwner().equals(player)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int getRows() {
        return size;
    }

    public int getCols() {
        return size;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Board)) return false;
        Board other = (Board) obj;
        if (this.size != other.size) return false;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (!this.board[x][y].equals(other.board[x][y])) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(size);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                result = 31 * result + board[x][y].hashCode();
            }
        }
        return result;
    }

    public int getSize() {
        return size;
    }

    public List<Piece> getBoardPosition(int sourceX, int sourceY) {
        if (!isWithinBounds(sourceX, sourceY)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[sourceX][sourceY].getPieces();
    }

    public void incrementMoveCount() {
        moveCount++;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public boolean canPlacePiece(int x, int y) {
        return isWithinBounds(x, y) && isCellEmpty(x, y);
    }

    public boolean isCellEmpty(int x, int y) {
        return board[x][y].isEmpty();
    }

    /** Example usage, if you want to rotate an already copied board. */
    public Board copyAndRotateClockwise(int times) {
        Board copiedBoard = this.copy(false, null);
        return copiedBoard.rotateClockwise(times);
    }

    /**
     * Prints the current state of the board, including remaining pieces for each player.
     */
    public void printBoard() {
        int totalPieces = 0;
        int bluePiecesOnBoard = 0;
        int greenPiecesOnBoard = 0;

        final String RESET = "\u001B[0m";
        final String BLUE = "\u001B[34m";
        final String GREEN = "\u001B[32m";

        System.out.println("Current Board State:");
        System.out.println("---------------------");

        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < size; x++) {
                PieceStack stack = board[x][y];
                if (stack.isEmpty()) {
                    row.append("[ ] ");
                } else {
                    Piece topPiece = stack.getTopPiece();
                    int stackHeight = stack.size();
                    totalPieces += stackHeight;

                    String colorCode = "";
                    if (topPiece.getOwner().getColor() == Player.Color.BLUE) {
                        bluePiecesOnBoard += stackHeight;
                        colorCode = BLUE;
                    } else if (topPiece.getOwner().getColor() == Player.Color.GREEN) {
                        greenPiecesOnBoard += stackHeight;
                        colorCode = GREEN;
                    }

                    row.append("[").append(colorCode).append(topPiece.toString())
                        .append(":").append(stackHeight).append(RESET).append("] ");
                }
            }
            System.out.println(row.toString());
        }

        System.out.println("---------------------");
        System.out.println("Total Pieces on Board: " + totalPieces);
        System.out.println("Blue Pieces on Board: " + bluePiecesOnBoard);
        System.out.println("Green Pieces on Board: " + greenPiecesOnBoard);

        System.out.println("\nPieces Remaining for Each Player:");
        System.out.println("----------------------------------");

        Player bluePlayer = null;
        Player greenPlayer = null;

        for (Player player : players) {
            if (player.getColor() == Player.Color.BLUE) {
                bluePlayer = player;
            } else if (player.getColor() == Player.Color.GREEN) {
                greenPlayer = player;
            }
        }

        if (bluePlayer != null) {
            System.out.println(BLUE + "Blue Player:" + RESET);
            System.out.println("  Flat Stones Left: " + bluePlayer.getRemainingPieces(Piece.PieceType.FLAT_STONE));
            System.out.println("  Standing Stones Left: " + bluePlayer.getRemainingPieces(Piece.PieceType.STANDING_STONE));
            System.out.println("  Capstones Left: " + bluePlayer.getRemainingPieces(Piece.PieceType.CAPSTONE));
        } else {
            System.out.println("Blue Player not found.");
        }

        if (greenPlayer != null) {
            System.out.println(GREEN + "Green Player:" + RESET);
            System.out.println("  Flat Stones Left: " + greenPlayer.getRemainingPieces(Piece.PieceType.FLAT_STONE));
            System.out.println("  Standing Stones Left: " + greenPlayer.getRemainingPieces(Piece.PieceType.STANDING_STONE));
            System.out.println("  Capstones Left: " + greenPlayer.getRemainingPieces(Piece.PieceType.CAPSTONE));
        } else {
            System.out.println("Green Player not found.");
        }

        System.out.println("----------------------------------");
    }
}
