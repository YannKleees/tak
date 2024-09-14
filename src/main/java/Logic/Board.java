package Logic;

import java.util.ArrayList;
import java.util.List;

/**
 * The Board class represents the game board for Tak.
 * It manages the placement and movement of pieces, including stack and capstone management.
 */
public class Board {
    private int size; // Board size (e.g., 5 for a 5x5 board)
    private List<Piece>[][] board;  // 2D array to hold stacks of pieces at each board position
    private final int CARRY_LIMIT; // Maximum number of pieces that can be moved

    /**
     * Constructor to initialize the board with a specified size.
     * Initializes the board as an empty grid with the required dimensions.
     *
     * @param size The size of the board.
     */
    @SuppressWarnings("unchecked")
    public Board(int size) {
        this.size = size;
        this.CARRY_LIMIT = size;
        this.board = new ArrayList[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                board[x][y] = new ArrayList<>();
            }
        }
    }

    /**
     * Returns the size of the board.
     *
     * @return The board size.
     */
    public int getSize() {
        return this.size;
    }

    /**
     * Returns the list of pieces (stack) at a specific position on the board.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return The list of pieces at the position.
     */
    public List<Piece> getBoardPosition(int x, int y) {
        if (isWithinBounds(x, y)) {
            return board[x][y];
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Places a piece on the board at a specific location.
     * Checks if the move is valid and if the piece can be placed.
     *
     * @param x             The X coordinate.
     * @param y             The Y coordinate.
     * @param piece         The piece to be placed.
     * @param currentPlayer The player making the move.
     * @throws InvalidMoveException If the move is invalid.
     */
    public void placePiece(int x, int y, Piece piece, Player currentPlayer) throws InvalidMoveException {
        if (!isWithinBounds(x, y)) {
            throw new InvalidMoveException("Position is out of bounds.");
        }
        List<Piece> stack = getBoardPosition(x, y);
        if (stack.isEmpty()) {
            stack.add(piece);
        } else {
            throw new InvalidMoveException("Cannot place a piece on top of an existing stack during placement.");
        }
    }

    /**
     * Moves a stack of pieces from one position along a path, dropping pieces as specified.
     *
     * @param fromX         The starting X coordinate.
     * @param fromY         The starting Y coordinate.
     * @param move          The move object containing movement details.
     * @param currentPlayer The player making the move.
     * @throws InvalidMoveException If the move is invalid.
     */
    public void movePiece(int fromX, int fromY, Move move, Player currentPlayer) throws InvalidMoveException {
        if (!isValidStackMove(fromX, fromY, move, currentPlayer)) {
            throw new InvalidMoveException("Invalid move.");
        }
        int x = fromX;
        int y = fromY;
        List<Piece> fromStack = getBoardPosition(x, y);
        int totalPiecesToMove = move.getNumberOfPieces();
        List<Integer> dropCounts = move.getDropCounts();
        List<Piece> piecesToMove = new ArrayList<>(fromStack.subList(fromStack.size() - totalPiecesToMove, fromStack.size()));
        fromStack.subList(fromStack.size() - totalPiecesToMove, fromStack.size()).clear();
        Direction direction = move.getDirection();
        for (int count : dropCounts) {
            switch (direction) {
                case UP:
                    y += 1;
                    break;
                case DOWN:
                    y -= 1;
                    break;
                case LEFT:
                    x -= 1;
                    break;
                case RIGHT:
                    x += 1;
                    break;
            }
            if (!isWithinBounds(x, y)) {
                throw new InvalidMoveException("Move goes out of bounds.");
            }
            List<Piece> toStack = getBoardPosition(x, y);
            List<Piece> piecesToDrop = new ArrayList<>(piecesToMove.subList(0, count));
            piecesToMove.subList(0, count).clear();
            Piece topPiece = piecesToDrop.get(piecesToDrop.size() - 1);
            if (!toStack.isEmpty()) {
                Piece targetTopPiece = toStack.get(toStack.size() - 1);
                if (!canStackOnTop(topPiece, targetTopPiece)) {
                    throw new InvalidMoveException("Cannot stack on top of the target stack.");
                }
                if (topPiece.isCapstone() && targetTopPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
                    targetTopPiece.setPieceType(Piece.PieceType.FLAT_STONE);
                }
            }
            toStack.addAll(piecesToDrop);
        }
    }

    /**
     * Validates a stack move according to the game rules.
     *
     * @param fromX         The starting X coordinate.
     * @param fromY         The starting Y coordinate.
     * @param move          The move object containing movement details.
     * @param currentPlayer The player making the move.
     * @return true if the move is valid, false otherwise.
     */
    private boolean isValidStackMove(int fromX, int fromY, Move move, Player currentPlayer) {
        if (!isWithinBounds(fromX, fromY)) {
            return false;
        }
        int x = fromX;
        int y = fromY;
        int totalPiecesToMove = move.getNumberOfPieces();
        List<Integer> dropCounts = move.getDropCounts();
        Direction direction = move.getDirection();
        if (totalPiecesToMove > CARRY_LIMIT) {
            return false;
        }
        List<Piece> fromStack = getBoardPosition(x, y);
        if (fromStack.size() < totalPiecesToMove) {
            return false;
        }
        Piece topPiece = fromStack.get(fromStack.size() - 1);
        if (topPiece.getOwner() != currentPlayer) {
            return false;
        }
        int sumDropCounts = dropCounts.stream().mapToInt(Integer::intValue).sum();
        if (sumDropCounts != totalPiecesToMove) {
            return false;
        }
        for (int count : dropCounts) {
            switch (direction) {
                case UP:
                    y += 1;
                    break;
                case DOWN:
                    y -= 1;
                    break;
                case LEFT:
                    x -= 1;
                    break;
                case RIGHT:
                    x += 1;
                    break;
            }
            if (!isWithinBounds(x, y)) {
                return false;
            }
            List<Piece> toStack = getBoardPosition(x, y);
            if (!toStack.isEmpty()) {
                Piece targetTopPiece = toStack.get(toStack.size() - 1);
                if (!canStackOnTop(topPiece, targetTopPiece)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if a move is valid based on the game rules.
     * Ensures that the move is orthogonal, within bounds, and follows stacking rules.
     *
     * @param fromX          The starting X coordinate.
     * @param fromY          The starting Y coordinate.
     * @param toX            The target X coordinate.
     * @param toY            The ending Y coordinate.
     * @param numberOfPieces The number of pieces being moved.
     * @param currentPlayer  The player making the move.
     * @return true if the move is valid, false otherwise.
     */
    public boolean isValidMove(int fromX, int fromY, int toX, int toY, int numberOfPieces, Player currentPlayer) {
        if (!isWithinBounds(fromX, fromY) || !isWithinBounds(toX, toY)) {
            return false;
        }
        if (fromX != toX && fromY != toY) {
            return false;
        }
        List<Piece> fromStack = getBoardPosition(fromX, fromY);
        if (fromStack.size() < numberOfPieces || numberOfPieces > CARRY_LIMIT) {
            return false;
        }
        Piece topPiece = fromStack.get(fromStack.size() - 1);
        if (topPiece.getOwner() != currentPlayer) {
            return false;
        }
        return true;
    }

    /**
     * Checks if a piece can be legally stacked on top of the target piece.
     *
     * @param movingPiece The piece attempting to stack.
     * @param targetPiece The top piece at the target location.
     * @return true if the moving piece can be stacked, false otherwise.
     */
    private boolean canStackOnTop(Piece movingPiece, Piece targetPiece) {
        if (targetPiece.getPieceType() == Piece.PieceType.CAPSTONE) {
            return false;
        }
        if (targetPiece.getPieceType() == Piece.PieceType.STANDING_STONE) {
            return movingPiece.isCapstone();
        }
        return true;
    }

    /**
     * Gets the top piece at a specific location on the board.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return The top piece at the given position, or null if no piece is present.
     */
    public Piece getPieceAt(int x, int y) {
        List<Piece> stack = getBoardPosition(x, y);
        if (!stack.isEmpty()) {
            return stack.get(stack.size() - 1);
        }
        return null;
    }

    /**
     * Checks if a given position is within the bounds of the board.
     *
     * @param x The X coordinate.
     * @param y The Y coordinate.
     * @return true if the position is within bounds, false otherwise.
     */
    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    /**
     * Checks if the board is full (no more empty spaces).
     *
     * @return true if the board is full, false otherwise.
     */
    public boolean isBoardFull() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (getBoardPosition(x, y).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Resets the board to its initial empty state.
     */
    public void resetBoard() {
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                getBoardPosition(x, y).clear();
            }
        }
    }

    /**
     * Prints the current state of the board to the console.
     * Useful for debugging or console-based UI.
     */
    public void printBoard() {
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                Piece topPiece = getPieceAt(x, y);
                if (topPiece == null) {
                    System.out.print(". ");
                } else {
                    char pieceChar = ' ';
                    switch (topPiece.getPieceType()) {
                        case FLAT_STONE:
                            pieceChar = 'F';
                            break;
                        case STANDING_STONE:
                            pieceChar = 'S';
                            break;
                        case CAPSTONE:
                            pieceChar = 'C';
                            break;
                    }
                    System.out.print(pieceChar + " ");
                }
            }
            System.out.println();
        }
    }
}
