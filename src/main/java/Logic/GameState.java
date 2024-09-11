package Logic;

public class GameState {

    // Tracks the current state of the board
    private Board board;

    // Tracks the current player (e.g., Player 1 or Player 2)
    private Player currentPlayer;

    // Boolean to track if the game has ended
    private boolean gameEnded;

    // Constructor to initialize the game state
    public GameState(Board board, Player player1, Player player2) {
        this.board = board;
        this.currentPlayer = player1; // Assuming Player 1 starts
        this.gameEnded = false;
    }

    // Function to switch turns between players
    public void switchTurns() {
        // Logic to switch the current player
    }

    // Function to check if the game has ended
    public boolean isGameEnded() {
        return this.gameEnded;
    }

    // Function to end the game
    public void endGame() {
        this.gameEnded = true;
    }

    // Function to get the current player
    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    // Function to set the current player
    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    // Function to get the current state of the board
    public Board getBoard() {
        return this.board;
    }

    // Function to reset the game state for a new game
    public void resetGameState() {
        // Reset the board and other states
    }
}
