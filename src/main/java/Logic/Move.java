package Logic;

public class Move {

    // Starting and ending coordinates of the move
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    // Number of pieces being moved in this move (for stack movements)
    private int numberOfPieces;

    // Boolean to check if the move involves a capstone
    private boolean isCapstoneMove;

    // Constructor to initialize the move, taking into account whether the move involves a capstone
    public Move(int startX, int startY, int endX, int endY, int numberOfPieces, boolean isCapstoneMove) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.numberOfPieces = numberOfPieces;
        this.isCapstoneMove = isCapstoneMove;
    }

    // Function to get the starting X position
    public int getStartX() {
        return this.startX;
    }

    // Function to get the starting Y position
    public int getStartY() {
        return this.startY;
    }

    // Function to get the ending X position
    public int getEndX() {
        return this.endX;
    }

    // Function to get the ending Y position
    public int getEndY() {
        return this.endY;
    }

    // Function to get the number of pieces being moved
    public int getNumberOfPieces() {
        return this.numberOfPieces;
    }

    // Function to check if this move involves a capstone
    public boolean isCapstoneMove() {
        return this.isCapstoneMove;
    }

    // Additional logic for validating the move based on the game rules could be included here
}
