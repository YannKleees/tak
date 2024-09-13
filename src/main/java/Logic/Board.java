package Logic;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private int size; // Board size (e.g., 5 for a 5x5 board)
    private List<Piece>[][] board;  // 2D array to hold stacks of pieces at each board position
    
    /**
     * Constructor to initialize the board with a specified size (e.g., 3x3, 5x5).
     * Initializes the board as an empty grid with the required dimensions.
     *
     * @param size The size of the board.
     */
    public Board(int size) {
        this.size = size;
        this.board = new ArrayList[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                board[x][y] = new ArrayList<>();
            }
        }
    }


    /**
     * Function to place a piece on the board at a specific location.
     * Checks if the move is valid (e.g., if the space is empty and the position is within bounds).
     *
     * @param x     The X coordinate.
     * @param y     The Y coordinate.
     * @param piece The piece to be placed.
     * @return true if the piece is successfully placed, false otherwise.
     */
    public boolean placePiece(int x, int y, Piece piece) {
        if (isWithinBounds(x, y) && (board[x][y].isEmpty() || canStackOnTop(piece, board[x][y]))) {
            board[x][y].add(piece);
            return true;
        }
        return false;
    }


    /**
     * Function to move a piece (or a stack of pieces) from one position to another.
     * Includes logic to check if the move is valid, based on Tak rules (e.g., movement direction, number of pieces).
     *
     * @param fromX          The starting X coordinate.
     * @param fromY          The starting Y coordinate.
     * @param toX            The target X coordinate.
     * @param toY            The target Y coordinate.
     * @param numberOfPieces The number of pieces being moved.
     * @return true if the move is valid and successfully executed, false otherwise.
     */
    public boolean movePiece(int fromX, int fromY, int toX, int toY, int numberOfPieces) {
        if (isValidMove(fromX, fromY, toX, toY, numberOfPieces)) {
            List<Piece> stack = board[fromX][fromY];
            List<Piece> piecesToMove = new ArrayList<>(stack.subList(stack.size() - numberOfPieces, stack.size()));
            board[fromX][fromY] = new ArrayList<>(stack.subList(0, stack.size() - numberOfPieces));
            board[toX][toY].addAll(piecesToMove);
            return true;
        }
        return false;
    }


    /**
     * Function to flatten a standing stone with a capstone.
     * Updates the state of the piece at the given location by flattening the standing stone into a flat stone.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return true if the standing stone is successfully flattened, false otherwise.
     */
    public boolean flattenStandingStone(int x, int y) {
        Piece topPiece = getPieceAt(x, y);
        if (topPiece != null && topPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
            topPiece.setPieceType(Piece.PieceType.FLAT_STONE);
            return true;
        }
        return false;
    }


    /**
     * Function to check if a move is valid.
     * Ensures that the move follows Tak's movement rules (e.g., no diagonal moves, 
     * valid stacking rules, valid number of pieces to move).
     *
     * @param fromX          The starting X coordinate.
     * @param fromY          The starting Y coordinate.
     * @param toX            The target X coordinate.
     * @param toY            The target Y coordinate.
     * @param numberOfPieces The number of pieces being moved.
     * @return true if the move is valid, false otherwise.
     */
    public boolean isValidMove(int fromX, int fromY, int toX, int toY, int numberOfPieces) {
        if (!isWithinBounds(fromX, fromY) || !isWithinBounds(toX, toY)) {
            return false;
        }
        if (fromX != toX && fromY != toY) {
            return false;
        }
        if (numberOfPieces > board[fromX][fromY].size()) {
            return false;
        }
        return true;
    }


    /**
     * Function to check if the board is full (no more moves can be made).
     * This is used to trigger a flat win condition if no road has been completed.
     *
     * @return true if the board is full, false otherwise.
     */
    public boolean isBoardFull() {
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
     * Function to get the piece at a specific board location.
     * Returns the topmost piece in the stack at the given (x, y) position.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return The piece at the given position, or null if no piece is present.
     */
    public Piece getPieceAt(int x, int y) {
        if (isWithinBounds(x, y) && !board[x][y].isEmpty()) {
            return board[x][y].get(board[x][y].size() - 1);  // Return the top piece
        }
        return null;  // No piece at this position
    }


    /**
     * Function to remove a piece from a specific board location.
     * Removes the top piece from the stack at the given (x, y) position.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return true if a piece was successfully removed, false otherwise.
     */
    public boolean removePiece(int x, int y) {
        if (isWithinBounds(x, y) && !board[x][y].isEmpty()) {
            board[x][y].remove(board[x][y].size() - 1);
            return true;
        }
        return false;
    }


    /**
     * Function to reset the board to its initial empty state.
     * Clears all pieces from the board, preparing it for a new game.
     */
    public void resetBoard() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                board[x][y].clear();
            }
        }
    }


    /**
     * Function to print the current state of the board (for debugging or console-based UI).
     * Outputs the board in a human-readable format, showing piece locations and types.
     */
    public void printBoard() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (board[x][y].isEmpty()) {
                    System.out.print(". ");
                } else {
                    Piece topPiece = board[x][y].get(board[x][y].size() - 1);
                    System.out.print(topPiece.getPieceType().name().charAt(0) + " ");
                }
            }
            System.out.println();
        }
    }


    /**
     * Helper function to check if a given position is within the bounds of the board.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return true if the position is within bounds, false otherwise.
     */
    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }


    /**
    * Function to get the size of the board (e.g., 5 for a 5x5 board).
    *
    * @return The size of the board.
    */
    public int getSize() {
        return this.size;
    }


    /**
     * Function to check if a piece can be legally stacked on top of the stack of pieces at a given position.
     * Rules for stacking:
     *  -Flat stones and standing stones can stack on top of flat stones.
     *  -Capstones cannot be stacked upon.
     *  -Standing stones cannot be stacked upon unless a capstone is flattening it.
     * @param piece The piece that is attempting to be placed on top.
     * @param stack The stack of pieces already present at the position.
     * @return true if the piece can be stacked on top, false otherwise.
     */
    private boolean canStackOnTop(Piece piece, List<Piece> stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Piece topPiece = stack.get(stack.size() - 1);
        if (topPiece.getPieceType() == Piece.PieceType.CAPSTONE) {
            return false;
        }
        if (topPiece.getPieceType() == Piece.PieceType.STANDING_STONE && piece.getPieceType() != Piece.PieceType.CAPSTONE) {
            return false;
        }
        return true;
    }
}
