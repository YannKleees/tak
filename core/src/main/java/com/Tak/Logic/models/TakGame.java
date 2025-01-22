package com.Tak.Logic.models;

import com.Tak.AI.players.MinimaxAgent;
import com.Tak.AI.players.RandomAIPlayer;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.utils.Logger;
import com.Tak.Logic.validators.MoveExecutor;
import com.Tak.Logic.validators.MoveValidator;
import com.Tak.Logic.validators.WinChecker;
import com.Tak.Logic.models.Player.Color;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
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

    private transient WinChecker winChecker; // Marked as transient if not serializable

    private static final int MAX_MOVES = 250; // Maximum number of moves to prevent infinite loops
    //private Set<String> previousBoardStates; // To track repeated board states

    /**
     * Constructs a TakGame with the specified board size, AI usage, and number of AI players.
     * This constructor initializes players internally.
     *
     * @param boardSize       The size of the board (e.g., 5 for a 5x5 board).
     * @param useAI           Whether to include AI players.
     * @param aiPlayersCount  Number of AI players (0 for all human players, 1 for one AI player, 2 for AI vs AI).
     */
    public TakGame(int boardSize, boolean useAI, int aiPlayersCount) {
        board = new Board(boardSize, boardSize);
        players = new ArrayList<>();
        board.setPlayers(players); // Set the Board's players list
        currentPlayerIndex = 0;
        moveCount = 0;
        isGameEnded = false;
        winner = null;

        initializePlayers(useAI, aiPlayersCount);
        winChecker = new WinChecker();
        //previousBoardStates = new HashSet<>();
    }

    /**
     * Constructs a TakGame with the specified board size and a custom list of players.
     * This constructor allows for more flexible player initialization.
     *
     * @param boardSize The size of the board (e.g., 5 for a 5x5 board).
     * @param players   The list of players participating in the game.
     */
    public TakGame(int boardSize, List<Player> players) {
        board = new Board(boardSize, boardSize);
        this.players = players;
        board.setPlayers(players); // Set the Board's players list
        currentPlayerIndex = 0;
        moveCount = 0;
        isGameEnded = false;
        winner = null;

        // Ensure opponents are set correctly
        if (players.size() >= 2) {
            players.get(0).setOpponent(players.get(1));
            players.get(1).setOpponent(players.get(0));
            //Logger.log("TakGame", "Set opponents for the first two players.");
        }

        //Logger.log("TakGame", "Initialized TakGame with custom players.");

        winChecker = new WinChecker();
        //previousBoardStates = new HashSet<>();
    }

    /**
     * Replaces an existing player with a new player and updates opponent references.
     *
     * @param oldPlayer The player to replace.
     * @param newPlayer The new player.
     */
    public synchronized void replacePlayer(Player oldPlayer, Player newPlayer) {
        int index = players.indexOf(oldPlayer);
        if (index != -1) {
            players.set(index, newPlayer);

            // Set opponents
            newPlayer.setOpponent(oldPlayer.getOpponent());
            oldPlayer.getOpponent().setOpponent(newPlayer); // 'newPlayer' instead of 'newAI'

            // Update ownership of all Pieces owned by oldPlayer
            Board board = getBoard();
            for (int x = 0; x < board.getSize(); x++) {
                for (int y = 0; y < board.getSize(); y++) {
                    List<Piece> stack = board.getBoardPosition(x, y);
                    for (Piece piece : stack) {
                        if (piece.getOwner().equals(oldPlayer)) {
                            piece.setOwner(newPlayer);
                            Logger.log("TakGame", "Updated piece at (" + x + ", " + y + ") to new owner: " + newPlayer.getColor());
                        }
                    }
                }
            }

            // If the replaced player was the current player, the currentPlayerIndex remains valid
            if (players.get(currentPlayerIndex).equals(oldPlayer)) {
                // currentPlayerIndex now points to newPlayer
            }

            Logger.log("TakGame", "Replaced player " + oldPlayer.getColor() + " with " + newPlayer.getColor());
        } else {
            Logger.log("TakGame", "Player to replace not found.");
        }
    }

    /**
     * Ends the game as a tie.
     */
    public void endGameAsTie() {
        isGameEnded = true;
        winner = null;
        //Logger.log("TakGame", "Game ended as a tie.");
    }

    /**
     * Initializes players based on whether AI is used and the number of AI players.
     *
     * @param useAI           Whether to include AI players.
     * @param aiPlayersCount  Number of AI players to include.
     */
    private void initializePlayers(boolean useAI, int aiPlayersCount) {
        if (!useAI || aiPlayersCount == 0) {
            // Add two HumanPlayers
            Player player1 = new HumanPlayer(Player.Color.BLUE, 21, 21, 1);
            Player player2 = new HumanPlayer(Player.Color.GREEN, 21, 21, 1);
            player1.setOpponent(player2);
            player2.setOpponent(player1);
            players.add(player1);
            players.add(player2);

          //  currentPlayerIndex = 1;
        } else {
            // Add AIPlayers based on aiPlayersCount
            if (aiPlayersCount == 1) {
                Player player1 = new HumanPlayer(Player.Color.BLUE, 21, 21, 1);
                Player aiPlayer = new MinimaxAgent(Player.Color.GREEN, 21, 21, 1, 3);
                player1.setOpponent(aiPlayer);
                aiPlayer.setOpponent(player1);
                players.add(player1);
                players.add(aiPlayer);

             //   currentPlayerIndex = 1;
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
            Logger.log("TakGame", p.getClass().getSimpleName() + " - Color: " + p.getColor());
        }
    }

    /**
     * Adds a player to the game.
     *
     * @param player The player to add.
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
     * Retrieves the opponent player of the current player.
     */
    public Player getOpponentPlayer() {
        return getCurrentPlayer().getOpponent();
    }

    /**
     * Moves a stack of pieces on the board based on the specified direction and drop counts.
     */
    public synchronized void moveStack(int fromX, int fromY, Direction direction, int[] dropCounts)
        throws InvalidMoveException, GameOverException {
        if (isGameEnded) {
            throw new GameOverException("The game has already ended.");
        }

        Player currentPlayer = getCurrentPlayer();
        int numberOfPieces = 0;
        for (int count : dropCounts) {
            numberOfPieces += count;
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

        // Check for repeated board state
        String boardStateHash = getBoardStateHash();
        //if (previousBoardStates.contains(boardStateHash)) {
            //Logger.log("TakGame", "Repeated board state detected. Declaring a tie.");
            //endGameAsTie();
            //return;
        //} else {
            //previousBoardStates.add(boardStateHash);
        //}

        checkWinConditions();

        // Check for maximum move count to prevent infinite loops
        if (moveCount >= MAX_MOVES && !isGameEnded) {
            Logger.log("TakGame", "Maximum move count reached. Declaring a tie to prevent infinite loop.");
            endGameAsTie();
            return;
        }

        switchPlayer();
    }

    /**
     * Places a piece on the board with the current player as owner.
     */
    public synchronized void placePiece(int x, int y, Piece.PieceType pieceType)
        throws InvalidMoveException, GameOverException {
        placePiece(x, y, pieceType, getCurrentPlayer());
    }

    /**
     * Places a piece on the board with a specified owner.
     */
    public synchronized void placePiece(int x, int y, Piece.PieceType pieceType, Player pieceOwner)
        throws InvalidMoveException, GameOverException {
    if (isGameEnded) {
        throw new GameOverException("The game has already ended.");
    }

    // Enforce rule: First two moves must be a flat stone of the opponent's color
    if (moveCount < 2) {
        if (pieceType != Piece.PieceType.FLAT_STONE) {
            throw new InvalidMoveException("Only flat stones allowed in the first two moves.");
        }
        // Force the stone to be owned by the opponent instead
        pieceOwner = pieceOwner.getOpponent();
    }

    // Now do the usual place logic
    board.placePiece(x, y, new Piece(pieceType, pieceOwner));
    pieceOwner.decrementPiece(pieceType);

    incrementMoveCount();
    checkWinConditions();

    // Check for max moves or repeated states, etc.
    if (moveCount >= MAX_MOVES && !isGameEnded) {
        Logger.log("TakGame", "Maximum move count reached. Declaring a tie to prevent infinite loop.");
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
     * Returns the game board.
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Checks for game end conditions:
     * 1) Road Win
     * 2) If a player has no stones left => compare top stones
     * 3) If board is full => flat winner
     * 4) If maximum moves reached => tie
     * 5) If repeated board state detected => tie
     */
    public synchronized void checkWinConditions() {
        if (isGameEnded) return;

        // 1) Road Win Check
        for (Player p : players) {
            boolean hasRoad = winChecker.checkForRoadWin(p, board);
            if (hasRoad) {
                isGameEnded = true;
                winner = p;
                winner.incrementScore(1);
                Logger.log("TakGame", "Player " + winner.getColor() + " wins by road!");
                return;
            }
        }

        // 2) Check if any player has no stones left
        for (Player p : players) {
            if (p.getTotalPiecesLeft() == 0) {
                // Player p has no stones left
                Player opponent = p.getOpponent();

                // Count top stones for both players
                int pTopStones = board.countPlayerPieces(p);
                int opponentTopStones = board.countPlayerPieces(opponent);

                if (pTopStones > opponentTopStones) {
                    winner = p;
                } else if (pTopStones < opponentTopStones) {
                    winner = opponent;
                } else {
                    winner = null; // Tie
                }

                isGameEnded = true;

                if (winner != null) {
                    winner.incrementScore(1);
                    //Logger.log("TakGame", "Player " + winner.getColor() + " wins by top stones!");
                } else {
                    Logger.log("TakGame", "Game ended in a tie based on top stones.");
                }
                return;
            }
        }

        // 3) If board is full => check top flat stones
        if (board.isFull()) {
            isGameEnded = true; // game ends either a tie or flat winner
            Player flatWinner = winChecker.getTopPlayer(board, players);
            winner = flatWinner; // may be null => tie

            if (winner != null) {
                winner.incrementScore(1);
                //Logger.log("TakGame", "Player " + winner.getColor() + " wins by flats!");
            } else {
                //Logger.log("TakGame", "Game ended in a tie (flat).");
            }
            return;
        }

        // 4) Check for maximum move count
        if (moveCount >= MAX_MOVES) {
            isGameEnded = true;
            winner = null;
            Logger.log("TakGame", "Maximum move count reached. Game ended in a tie to prevent infinite loop.");
            return;
        }

        // 5) Repeated board state is handled in moveStack and placePiece
    }

    /**
     * Resets the game state.
     */
    public synchronized void resetGame(boolean resetScores) {
        board.reset();
        moveCount = 0;
        isGameEnded = false;
        winner = null;
        currentPlayerIndex = 0;
        //   previousBoardStates.clear();

        for (Player player : players) {
            player.resetPieces(21, 1); // Standard counts
            if (resetScores) {
                player.resetScore();
            }
        }
    }

    /**
     * Switches the current player to the next player in the list.
     */
    public synchronized void switchPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        //Logger.log("TakGame", "Switched to player: " + getCurrentPlayer().getColor());
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
     * Returns the winner of the game.
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
     * Returns the list of players.
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

    // Serialization methods to handle transient fields
    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, java.io.IOException {
        in.defaultReadObject();
        // Re-initialize transient fields
        winChecker = new WinChecker();
        //previousBoardStates = new HashSet<>();
    }

    /**
     * Generates a hash representation of the current board state.
     * This can be used to detect repeated states.
     *
     * @return A string hash representing the current board state.
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
     * Logs the final board state.
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
