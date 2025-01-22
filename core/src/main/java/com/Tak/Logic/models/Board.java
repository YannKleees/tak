package com.Tak.Logic.models;

import com.Tak.Logic.utils.InvalidMoveException;
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
    private final int carryLimit; // clarifies purpose
    protected final PieceStack[][] board;
    private List<Player> players;
    private int moveCount;

    /**
     * Constructor to initialize the board with a given size.
     *
     * @param size       The size of the board (e.g., 5 for a 5x5 board).
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

    /**
     * Sets the list of players.
     *
     * @param players The list of players.
     */
    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    /**
     * Gets the list of players.
     *
     * @return The list of players.
     */
    public List<Player> getPlayers() {
        return this.players;
    }

    /**
     * Retrieves the top piece at the specified position.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The top piece, or null if the stack is empty.
     * @throws IndexOutOfBoundsException If the position is out of bounds.
     */
    public Piece getPieceAt(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[x][y].getTopPiece();
    }

    /**
     * Retrieves the size of the stack at the specified position.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The size of the stack.
     * @throws IndexOutOfBoundsException If the position is out of bounds.
     */
    public int getStackSize(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[x][y].size();
    }

    /**
     * Retrieves the stack of pieces at the specified position.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return The stack of pieces at the specified position.
     * @throws IndexOutOfBoundsException If the position is out of bounds.
     */
    public PieceStack getBoardStack(int x, int y) {
        if (!isWithinBounds(x, y)) {
            throw new IndexOutOfBoundsException("Board position out of bounds.");
        }
        return board[x][y];
    }

    /**
     * Checks if the given coordinates are within the board boundaries.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @return True if within bounds, false otherwise.
     */
    public boolean isWithinBounds(int x, int y) {
        return (x >= 0 && x < size && y >= 0 && y < size);
    }

    /**
     * Gets the carry limit (maximum number of pieces that can be carried in a move).
     *
     * @return The carry limit.
     */
    public int getCarryLimit() {
        return carryLimit;
    }

    /**
     * Places a piece on the board at the specified position.
     *
     * @param x     The x-coordinate.
     * @param y     The y-coordinate.
     * @param piece The piece to place.
     * @throws InvalidMoveException If the move is invalid.
     */
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

    /**
     * Removes a number of pieces from the top of the stack at the given position.
     *
     * @param x      The x-coordinate.
     * @param y      The y-coordinate.
     * @param amount The number of pieces to remove.
     * @return The list of removed pieces.
     * @throws InvalidMoveException If there aren't enough pieces to remove.
     */
    public List<Piece> removePieces(int x, int y, int amount) throws InvalidMoveException {
        if (!isWithinBounds(x, y)) {
            throw new InvalidMoveException("Board position out of bounds.");
        }
        return board[x][y].removePieces(amount);
    }

    /**
     * Checks if the board is full (all positions have at least one piece).
     *
     * @return True if the board is full, false otherwise.
     */
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

    /**
     * Resets the board to its initial empty state.
     */
    public void reset() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                board[x][y].clear();
            }
        }
        moveCount = 0;
    }

    /**
     * Creates a deep copy of the current Board.
     *
     * @return A new Board instance with the same state.
     */
    public Board copy() {
        Board newBoard = new Board(this.size, this.carryLimit);
        newBoard.moveCount = this.moveCount;

        Map<Player, Player> playerMap = new HashMap<>();
        newBoard.players = new ArrayList<>();
        for (Player player : this.players) {
            Player playerCopy = player.copy();
            newBoard.players.add(playerCopy);
            playerMap.put(player, playerCopy);
        }

        for (Player originalPlayer : this.players) {
            Player copiedPlayer = playerMap.get(originalPlayer);
            Player originalOpponent = originalPlayer.getOpponent();
            if (originalOpponent != null) {
                Player copiedOpponent = playerMap.get(originalOpponent);
                copiedPlayer.setOpponent(copiedOpponent);
            }
        }

        for (int x = 0; x < this.size; x++) {
            for (int y = 0; y < this.size; y++) {
                PieceStack originalStack = this.board[x][y];
                PieceStack newStack = originalStack.copy(playerMap);
                newBoard.board[x][y] = newStack;
            }
        }
        return newBoard;
    }

    /**
     * Retrieves a player from the board's player list by their color.
     *
     * @param color The color of the player.
     * @return The player with the specified color, or null if not found.
     */
    public Player getPlayerByColor(Player.Color color) {
        for (Player p : players) {
            if (p.getColor() == color) {
                return p;
            }
        }
        return null;
    }

    /**
     * Rotates the board clockwise by the specified number of 90-degree increments.
     *
     * @param times The number of times to rotate 90 degrees clockwise.
     * @return A new Board instance representing the rotated board.
     */
    public Board rotateClockwise(int times) {
        times = times % 4;
        Board rotated = this.copy();
        Map<Player, Player> playerMap = createPlayerMap(rotated, this.players);
        for (int t = 0; t < times; t++) {
            rotated = rotated.singleRotateClockwise(playerMap);
        }
        return rotated;
    }

    /**
     * Performs a single 90-degree clockwise rotation of the board.
     *
     * @param playerMap The mapping of original players to their copies.
     * @return A new Board instance rotated by 90 degrees clockwise.
     */
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

    /**
     * Helper method to create a player map from rotated board's players to original board's players.
     *
     * @param rotatedBoard    The rotated board.
     * @param originalPlayers The original players list.
     * @return The player map.
     */
    private Map<Player, Player> createPlayerMap(Board rotatedBoard, List<Player> originalPlayers) {
        Map<Player, Player> playerMap = new HashMap<>();
        for (int i = 0; i < originalPlayers.size(); i++) {
            Player originalPlayer = originalPlayers.get(i);
            Player rotatedPlayer = rotatedBoard.players.get(i);
            playerMap.put(originalPlayer, rotatedPlayer);
        }
        return playerMap;
    }

    /**
     * Counts the number of pieces a player has on the board.
     *
     * @param player The player whose pieces to count.
     * @return The number of pieces the player has on the board.
     */
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

    /**
     * Returns the number of rows on the board.
     *
     * @return The number of rows.
     */
    public int getRows() {
        return size;
    }

    /**
     * Returns the number of columns on the board.
     *
     * @return The number of columns.
     */
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
    
    /**
     * Creates a deep copy of the current Board and rotates it clockwise by specified times.
     *
     * @param times The number of times to rotate 90 degrees clockwise.
     * @return A new Board instance representing the rotated board.
     */
    public Board copyAndRotateClockwise(int times) {
        Board copiedBoard = this.copy();
        return copiedBoard.rotateClockwise(times);
    }

    /**
     * Prints the current state of the board, including remaining pieces for each player.
     */
    public void printBoard() {
        int totalPieces = 0; // Counter for total pieces on the board
        int bluePiecesOnBoard = 0;  // Counter for Blue's pieces on the board
        int greenPiecesOnBoard = 0; // Counter for Green's pieces on the board
    
        // ANSI color codes
        final String RESET = "\u001B[0m";
        final String BLUE = "\u001B[34m";
        final String GREEN = "\u001B[32m";
    
        System.out.println("Current Board State:");
        System.out.println("---------------------");
    
        // Loop through each row and column of the board
        for (int y = 0; y < size; y++) {
            StringBuilder row = new StringBuilder();
            
            for (int x = 0; x < size; x++) {
                PieceStack stack = board[x][y];
                // Check if the stack is empty
                if (stack.isEmpty()) {
                    row.append("[ ] "); // Empty space for an empty stack
                } else {
                    Piece topPiece = stack.getTopPiece();
                    int stackHeight = stack.size(); // Get the number of pieces in the stack
                    totalPieces += stackHeight; // Increment total pieces counter
                    
                    // Increment per-player counters based on ownership
                    String colorCode = "";
                    if (topPiece.getOwner().getColor() == Player.Color.BLUE) {
                        bluePiecesOnBoard += stackHeight;
                        colorCode = BLUE;
                    } else if (topPiece.getOwner().getColor() == Player.Color.GREEN) {
                        greenPiecesOnBoard += stackHeight;
                        colorCode = GREEN;
                    }
                    
                    // Format: [TopPiece:Height] with color
                    row.append("[").append(colorCode).append(topPiece.toString())
                       .append(":").append(stackHeight).append(RESET).append("] ");
                }
            }
            // Print the row for each y-coordinate
            System.out.println(row.toString());
        }
        
        System.out.println("---------------------");
        // After printing the board, display the total number of pieces and per-player counts on the board
        System.out.println("Total Pieces on Board: " + totalPieces);
        System.out.println("Blue Pieces on Board: " + bluePiecesOnBoard);
        System.out.println("Green Pieces on Board: " + greenPiecesOnBoard);
        
        // **New Section: Displaying Remaining Pieces for Each Player**
        System.out.println("\nPieces Remaining for Each Player:");
        System.out.println("----------------------------------");
        
        // Assuming you have access to the players, for example via a list or directly
        // Replace `players` with the appropriate reference to your player instances
        // For illustration, let's assume you have two players: bluePlayer and greenPlayer
        Player bluePlayer = null;
        Player greenPlayer = null;
        
        // Retrieve players based on color
        for (Player player : players) { // Replace 'players' with your actual players collection
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
