package com.Tak.Logic.models;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.exceptions.GameOverException;
import com.Tak.Logic.exceptions.InvalidMoveException;
import com.Tak.Logic.players.HumanPlayer;
import com.Tak.Logic.utils.Logger;
import com.Tak.Logic.validators.MoveExecutor;
import com.Tak.Logic.validators.MoveValidator;
import com.Tak.Logic.validators.WinChecker;
import com.Tak.Logic.models.Player.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The TakGame class manages the game state, including the board, players, move counts, and win conditions.
 */
public class TakGame implements Serializable {

    private static final long serialVersionUID = 1L;

    private Board board;
    private List<Player> players;
    private int currentPlayerIndex;
    private int moveCount;
    private boolean isGameEnded;
    private Player winner;

    private transient WinChecker winChecker; // Marked transient if not serializable

    private static final int MAX_MOVES = 1000; // Maximum number of moves
    private Set<String> previousBoardStates;  // Track repeated board states

    /**
     * Constructs a TakGame with the specified board size, optional AI usage, and number of AI players.
     * This constructor initializes players internally.
     *
     * @param boardSize       The size of the board (e.g., 5 for a 5x5).
     * @param useAI           Whether to include AI players.
     * @param aiPlayersCount  Number of AI players (0, 1, or 2).
     */
    public TakGame(int boardSize, boolean useAI, int aiPlayersCount) {
        board = new Board(boardSize, boardSize);
        players = new ArrayList<>();
        board.setPlayers(players);
        currentPlayerIndex = 0;
        moveCount = 0;
        isGameEnded = false;
        winner = null;

        initializePlayers(useAI, aiPlayersCount);
        winChecker = new WinChecker();
        previousBoardStates = new HashSet<>();
    }

    /**
     * Constructs a TakGame with the specified board size and a custom list of players.
     * Allows more flexible player initialization.
     *
     * @param boardSize The size of the board (e.g., 5).
     * @param players   The list of players in this game.
     */
    public TakGame(int boardSize, List<Player> players) {
        board = new Board(boardSize, boardSize);
        this.players = players;
        board.setPlayers(players);
        currentPlayerIndex = 0;
        moveCount = 0;
        isGameEnded = false;
        winner = null;

        // Ensure at least the first two players are set as opponents
        if (players.size() >= 2) {
            players.get(0).setOpponent(players.get(1));
            players.get(1).setOpponent(players.get(0));
        }

        winChecker = new WinChecker();
        previousBoardStates = new HashSet<>();
    }

    /**
     * Replaces an existing player with a new player and updates ownership + opponent references.
     *
     * @param oldPlayer The player to replace.
     * @param newPlayer The new player.
     */
    public synchronized void replacePlayer(Player oldPlayer, Player newPlayer) {
        int index = players.indexOf(oldPlayer);
        if (index != -1) {
            players.set(index, newPlayer);

            // Update opponents
            newPlayer.setOpponent(oldPlayer.getOpponent());
            oldPlayer.getOpponent().setOpponent(newPlayer);

            // Update piece ownership on the board
            Board board = getBoard();
            for (int x = 0; x < board.getSize(); x++) {
                for (int y = 0; y < board.getSize(); y++) {
                    List<Piece> stack = board.getBoardPosition(x, y);
                    for (Piece piece : stack) {
                        if (piece.getOwner().equals(oldPlayer)) {
                            piece.setOwner(newPlayer);
                            Logger.log("TakGame", "Updated piece at (" + x + ", " + y
                                + ") to new owner: " + newPlayer.getColor());
                        }
                    }
                }
            }

            // If we replaced the current player, that index now points to newPlayer
            Logger.log("TakGame", "Replaced player " + oldPlayer.getColor()
                + " with " + newPlayer.getColor());
        } else {
            Logger.log("TakGame", "Player to replace not found.");
        }
    }

    /**
     * Ends the game as a tie (no winner).
     */
    public void endGameAsTie() {
        isGameEnded = true;
        winner = null;
    }

    /**
     * Initializes players based on whether AI is used and how many AI players to add.
     */
    private void initializePlayers(boolean useAI, int aiPlayersCount) {
        if (!useAI || aiPlayersCount == 0) {
            // 2 Human players
            Player player1 = new HumanPlayer(Player.Color.BLUE, 21, 21, 1);
            Player player2 = new HumanPlayer(Player.Color.GREEN, 21, 21, 1);
            player1.setOpponent(player2);
            player2.setOpponent(player1);
            players.add(player1);
            players.add(player2);

        } else {
            // AI mode
            if (aiPlayersCount == 1) {
                Player player1 = new HumanPlayer(Player.Color.BLUE, 21, 21, 1);
                Player aiPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 21, 1, 3);
                player1.setOpponent(aiPlayer);
                aiPlayer.setOpponent(player1);
                players.add(player1);
                players.add(aiPlayer);

                Logger.log("TakGame", "Added HumanPlayer BLUE and AIPlayer GREEN.");
            } else if (aiPlayersCount == 2) {
                Player aiPlayer1 = new MinimaxAgent(Player.Color.BLUE, 21, 21, 1, 3);
                Player aiPlayer2 = new MinimaxAgent(Player.Color.GREEN, 21, 21, 1, 3);
                aiPlayer1.setOpponent(aiPlayer2);
                aiPlayer2.setOpponent(aiPlayer1);
                players.add(aiPlayer1);
                players.add(aiPlayer2);

                Logger.log("TakGame", "Added two AIPlayers: BLUE and GREEN.");
            } else {
                throw new IllegalArgumentException("aiPlayersCount must be 0, 1, or 2.");
            }
        }

        // Log final players
        Logger.log("TakGame", "Final Players List:");
        for (Player p : players) {
            Logger.log("TakGame", p.getClass().getSimpleName()
                + " - Color: " + p.getColor());
        }
    }

    /**
     * Adds a new player (for expansions, etc.).
     */
    public void addPlayer(Player player) {
        if (!players.contains(player)) {
            players.add(player);
            if (players.size() == 2) {
                players.get(0).setOpponent(players.get(1));
                players.get(1).setOpponent(players.get(0));
            }
        }
    }

    /**
     * Returns the opponent of the current player.
     */
    public Player getOpponentPlayer() {
        return getCurrentPlayer().getOpponent();
    }

    /**
     * Moves a stack in the given direction with specified dropCounts distribution.
     */
    public synchronized void moveStack(int fromX, int fromY, Direction direction, int[] dropCounts)
        throws InvalidMoveException, GameOverException {
        if (isGameEnded) {
            throw new GameOverException("The game has already ended.");
        }

        Player currentPlayer = getCurrentPlayer();
        int numberOfPieces = 0;
        for (int c : dropCounts) {
            numberOfPieces += c;
        }
        if (numberOfPieces > board.getCarryLimit()) {
            throw new InvalidMoveException("Cannot carry more than " + board.getCarryLimit() + " pieces.");
        }

        List<Integer> dropCountsList = new ArrayList<>();
        for (int count : dropCounts) {
            dropCountsList.add(count);
        }

        Move move = new Move(fromX, fromY, direction, numberOfPieces, dropCountsList);
        move.setPlayer(currentPlayer);

        MoveValidator validator = new MoveValidator(board, currentPlayer);
        if (!validator.isValidMove(fromX, fromY, move)) {
            throw new InvalidMoveException("Move validation failed.");
        }

        MoveExecutor executor = new MoveExecutor(board, currentPlayer);
        executor.executeMove(fromX, fromY, move);

        incrementMoveCount();

        // Check repeated board state
        String boardStateHash = getBoardStateHash();
        if (previousBoardStates.contains(boardStateHash)) {
            Logger.log("TakGame", "Repeated board state => tie.");
            endGameAsTie();
            return;
        } else {
            previousBoardStates.add(boardStateHash);
        }

        checkWinConditions();

        // Max moves check
        if (moveCount >= MAX_MOVES && !isGameEnded) {
            Logger.log("TakGame", "Max moves => tie.");
            endGameAsTie();
            return;
        }

        switchPlayer();
    }

    /**
     * Places a piece belonging to the current player.
     */
    public synchronized void placePiece(int x, int y, Piece.PieceType pieceType)
        throws InvalidMoveException, GameOverException {
        placePiece(x, y, pieceType, getCurrentPlayer());
    }

    /**
     * Places a piece with a specified owner.
     */
    public synchronized void placePiece(int x, int y, Piece.PieceType pieceType, Player pieceOwner)
        throws InvalidMoveException, GameOverException {
        if (isGameEnded) {
            throw new GameOverException("The game has already ended.");
        }

        board.placePiece(x, y, new Piece(pieceType, pieceOwner));
        pieceOwner.decrementPiece(pieceType);

        incrementMoveCount();

        // Check repeated board state
        String boardStateHash = getBoardStateHash();
        if (previousBoardStates.contains(boardStateHash)) {
            Logger.log("TakGame", "Repeated board state => tie.");
            endGameAsTie();
            return;
        } else {
            previousBoardStates.add(boardStateHash);
        }

        checkWinConditions();

        // Max moves check
        if (moveCount >= MAX_MOVES && !isGameEnded) {
            Logger.log("TakGame", "Max moves => tie.");
            endGameAsTie();
            return;
        }

        switchPlayer();
    }

    /**
     * Increments the move count.
     */
    public synchronized void incrementMoveCount() {
        moveCount++;
    }

    /**
     * Returns the current move count.
     */
    public int getMoveCount() {
        return moveCount;
    }

    /**
     * Returns the current board.
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Allows external code to replace the current board (for stepping back/forward in history, etc.).
     * Make sure to re-inject the same player list into 'newBoard'.
     */
    public synchronized void setBoard(Board newBoard) {
        this.board = newBoard;
        this.board.setPlayers(players);
    }

    /**
     * Checks for game-end conditions (road wins, no stones left, board full, no moves left, etc.).
     */
    public synchronized void checkWinConditions() {
        if (isGameEnded) return;

        // 1) Road Win
        for (Player p : players) {
            if (winChecker.checkForRoadWin(p, board)) {
                isGameEnded = true;
                winner = p;
                winner.incrementScore(1);
                Logger.log("TakGame", "Player " + winner.getColor() + " wins by road!");
                return;
            }
        }

        // 2) If all players have no stones left => top-stone comparison
        for (Player p : players) {
            if (p.getTotalPiecesLeft() == 0) {
                Player opponent = p.getOpponent();
                int pTopStones = board.countPlayerPieces(p);
                int oppTopStones = board.countPlayerPieces(opponent);

                if (pTopStones > oppTopStones) {
                    winner = p;
                } else if (pTopStones < oppTopStones) {
                    winner = opponent;
                } else {
                    winner = null;
                }

                isGameEnded = true;
                if (winner != null) {
                    winner.incrementScore(1);
                } else {
                    Logger.log("TakGame", "Tie: No stones left => equal top stones.");
                }
                return;
            }
        }

        // 2.5) If all players cannot move => do a flat check
        if (allPlayersHaveNoMoves()) {
            isGameEnded = true;
            Player flatWinner = winChecker.getTopPlayer(board, players);
            winner = flatWinner;
            if (winner != null) {
                winner.incrementScore(1);
                Logger.log("TakGame", "No moves left => " + winner.getColor() + " wins on flats.");
            } else {
                Logger.log("TakGame", "No moves left => tie on flats.");
            }
            return;
        }

        // 3) Board full => flat check
        if (board.isFull()) {
            isGameEnded = true;
            Player flatWinner = winChecker.getTopPlayer(board, players);
            winner = flatWinner;
            if (winner != null) {
                winner.incrementScore(1);
            } else {
                Logger.log("TakGame", "Board full => tie on flats.");
            }
            return;
        }

        // 4) Max moves => tie
        if (moveCount >= MAX_MOVES) {
            isGameEnded = true;
            winner = null;
            Logger.log("TakGame", "Max moves => tie.");
            return;
        }
    }

    /**
     * Checks if all players cannot make a move (i.e., each player's canMakeAMove(...) is false).
     */
    private boolean allPlayersHaveNoMoves() {
        for (Player p : players) {
            if (p.canMakeAMove(board)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Resets the game. If resetScores is true, also resets players' scores.
     */
    public synchronized void resetGame(boolean resetScores) {
        board.reset();
        moveCount = 0;
        isGameEnded = false;
        winner = null;
        currentPlayerIndex = 0;
        previousBoardStates.clear();

        for (Player player : players) {
            player.resetPieces(21, 1); // standard counts
            if (resetScores) {
                player.resetScore();
            }
        }
    }

    /**
     * Switches to the next player in the list.
     */
    public synchronized void switchPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        Logger.log("TakGame", "Switched to player: " + getCurrentPlayer().getColor());
    }

    /**
     * Returns the current player.
     */
    public synchronized Player getCurrentPlayer() {
        if (players.isEmpty()) {
            throw new IllegalStateException("Players list is empty. No current player available.");
        }
        return players.get(currentPlayerIndex);
    }

    /**
     * Returns the board size.
     */
    public int getBoardSize() {
        return board.getSize();
    }

    /**
     * Determines if the game has ended.
     */
    public boolean isGameEnded() {
        return isGameEnded;
    }

    /**
     * Gets the winner (or null if tie or not ended).
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Returns Player1 (BLUE).
     */
    public Player getPlayer1() {
        return players.get(0);
    }

    /**
     * Returns Player2 (GREEN).
     */
    public Player getPlayer2() {
        return players.get(1);
    }

    /**
     * Gets the list of players.
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Checks if the board is full.
     */
    public boolean isBoardFull() {
        return board.isFull();
    }

    // Serialization for transient fields
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, java.io.IOException {
        in.defaultReadObject();
        winChecker = new WinChecker();
        previousBoardStates = new HashSet<>();
    }

    /**
     * Generates a simple hash for the current board state (top pieces).
     */
    private String getBoardStateHash() {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < board.getSize(); x++) {
            for (int y = 0; y < board.getSize(); y++) {
                Piece topPiece = board.getPieceAt(x, y);
                if (topPiece != null) {
                    sb.append(topPiece.toString());
                } else {
                    sb.append("null");
                }
                sb.append("|");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    /**
     * Logs the final board state for debugging or analysis.
     */
    public void logFinalBoardState() {
        StringBuilder boardState = new StringBuilder();
        boardState.append("Final Board State:\n");

        for (int row = 0; row < board.getSize(); row++) {
            for (int col = 0; col < board.getSize(); col++) {
                Piece piece = board.getPieceAt(row, col);
                if (piece != null) {
                    boardState.append(piece.toString()).append(" ");
                } else {
                    boardState.append(". ");
                }
            }
            boardState.append("\n");
        }
        Logger.log("Game", boardState.toString());
    }
}
