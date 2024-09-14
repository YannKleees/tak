package Logic;

import java.util.List;

/**
 * The TakGame class manages the overall game flow.
 * It handles player turns, move execution, and checks for win conditions.
 */
public class TakGame {

    private Board board;              // The game board
    private Player player1;           // Player 1 (Black)
    private Player player2;           // Player 2 (White)
    private Player currentPlayer;     // The player whose turn it is
    private WinChecker winChecker;    // Utility to check win conditions
    private boolean gameEnded;        // Flag to indicate if the game has ended

    /**
     * Constructor to initialize the game with the board and players.
     *
     * @param boardSize The size of the board.
     */
    public TakGame(int boardSize) {
        this.board = new Board(boardSize);
        int flatStones = calculateFlatStones(boardSize);
        int capstones = calculateCapstones(boardSize);

        this.player1 = new Player(Player.Color.BLACK, flatStones, 0, capstones);
        this.player2 = new Player(Player.Color.WHITE, flatStones, 0, capstones);
        this.currentPlayer = player1;
        this.winChecker = new WinChecker();
        this.gameEnded = false;
    }

    /**
     * Calculates the number of flat stones based on board size.
     *
     * @param boardSize The size of the board.
     * @return The number of flat stones.
     */
    private int calculateFlatStones(int boardSize) {
        switch (boardSize) {
            case 3:
                return 10;
            case 4:
                return 15;
            case 5:
                return 21;
            case 6:
                return 30;
            case 8:
                return 50;
            default:
                return 21;
        }
    }

    /**
     * Calculates the number of capstones based on board size.
     *
     * @param boardSize The size of the board.
     * @return The number of capstones.
     */
    private int calculateCapstones(int boardSize) {
        switch (boardSize) {
            case 5:
                return 1;
            case 6:
                return 1;
            case 8:
                return 2;
            default:
                return 0;
        }
    }

    /**
     * Starts the game. Any initial setup can be done here.
     */
    public void startGame() {
        // Initialization logic if needed
    }

    /**
     * Performs a move for the current player.
     *
     * @param move The move to be performed.
     * @throws GameOverException     If the game has ended.
     * @throws InvalidMoveException  If the move is invalid.
     */
    public void performMove(Move move) throws GameOverException, InvalidMoveException {
        if (gameEnded) {
            throw new GameOverException("Game has ended.");
        }
        int fromX = move.getStartX();
        int fromY = move.getStartY();
        if (!board.isValidMove(fromX, fromY, fromX, fromY, move.getNumberOfPieces(), currentPlayer)) {
            throw new InvalidMoveException("Invalid move.");
        }
        board.movePiece(fromX, fromY, move, currentPlayer);
        checkWinConditions();
        if (!gameEnded) {
            switchTurns();
        }
    }

    /**
     * Places a piece on the board for the current player.
     *
     * @param x         The X coordinate.
     * @param y         The Y coordinate.
     * @param pieceType The type of piece to place.
     * @throws GameOverException     If the game has ended.
     * @throws InvalidMoveException  If the move is invalid.
     */
    public void placePiece(int x, int y, Piece.PieceType pieceType) throws GameOverException, InvalidMoveException {
        if (gameEnded) {
            throw new GameOverException("Game has ended.");
        }

        if (currentPlayer.hasPiecesLeft(pieceType)) {
            Piece piece = new Piece(pieceType, currentPlayer);
            board.placePiece(x, y, piece, currentPlayer);
            currentPlayer.decrementPiece(pieceType);
            checkWinConditions();
            if (!gameEnded) {
                switchTurns();
            }
        } else {
            throw new InvalidMoveException("No remaining pieces of this type.");
        }
    }

    /**
     * Checks for win conditions after a move.
     */
    private void checkWinConditions() {
        if (winChecker.checkForRoadWin(currentPlayer, board)) {
            System.out.println(currentPlayer.getColor() + " wins by road!");
            gameEnded = true;
        } else {
            Player flatWinPlayer = winChecker.checkForFlatWin(this);
            if (flatWinPlayer != null) {
                System.out.println(flatWinPlayer.getColor() + " wins by flat count!");
                gameEnded = true;
            }
        }
    }

    /**
     * Switches turns between players.
     */
    private void switchTurns() {
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }

    /**
     * Gets the current game board.
     *
     * @return The game board.
     */
    public Board getBoard() {
        return this.board;
    }

    /**
     * Gets the current player.
     *
     * @return The current player.
     */
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    /**
     * Gets Player 1 (Black).
     *
     * @return Player 1.
     */
    public Player getPlayer1() {
        return this.player1;
    }

    /**
     * Gets Player 2 (White).
     *
     * @return Player 2.
     */
    public Player getPlayer2() {
        return this.player2;
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if the game has ended, false otherwise.
     */
    public boolean isGameEnded() {
        return this.gameEnded;
    }

    /**
     * Resets the game to its initial state.
     */
    public void resetGame() {
        board.resetBoard();
        currentPlayer = player1;
        gameEnded = false;
        int flatStones = calculateFlatStones(board.getSize());
        int capstones = calculateCapstones(board.getSize());
        player1 = new Player(Player.Color.BLACK, flatStones, 0, capstones);
        player2 = new Player(Player.Color.WHITE, flatStones, 0, capstones);
    }
}
