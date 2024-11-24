package com.Tak.AI.experiments;

import com.Tak.AI.actions.Move;
import com.Tak.Logic.models.BluePlayer;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.GreenPlayer;
import com.Tak.Logic.models.Player;
import java.util.List;

/*public class BranchingExperiment {
    private final Board board;
    private final Player greenPlayer;
    private final Players bluePlayer;
    private final int maxDepth;

    public BranchingExperiment(Board board, Player greenPlayer, Player bluePlayer, int maxDepth) {
        this.board = board;
        this.greenPlayer = greenPlayer;
        this.bluePlayer = bluePlayer;
        this.maxDepth = maxDepth;
    }

    public void runExperiment() {
        for (int depth = 1; depth <= maxDepth; depth++) {
            int totalMoves = 0;
            int nodesVisited = 0;

            // Simulate a search for branching factor measurement
            totalMoves = countBranchingFactor(board, depth, greenPlayer, bluePlayer);
            nodesVisited += totalMoves;

            System.out.println("Depth: " + depth + " | Branching Factor: " + (totalMoves / (float) nodesVisited));
        }
    }

    private int countBranchingFactor(Board board, int depth, Player currentPlayer, Player opponent) {
        if (depth == 0) {
            return 0; // Base case: no more moves at depth 0
        }

        // Get all legal moves for the current player
        List<Move> legalMoves = board.getLegalMoves(currentPlayer);
        int branchingFactor = 0;
        int totalMoves = legalMoves.size(); // Total moves available at this depth

        // If no legal moves, return 0 as branching factor
        if (totalMoves == 0) {
            return 0;
        }

        // Count how many nodes (recursive calls) will be visited
        int nodesVisited = 0;

        // Explore all legal moves recursively
        for (Move move : legalMoves) {
            try {
                // Copy the board to avoid mutating the original one
                Board newBoard = board.copy();

                // Execute the current move
                //newBoard.executeMove(move, currentPlayer);

                // Recursive exploration: switch players and reduce depth by 1
                nodesVisited += countBranchingFactor(newBoard, depth - 1, opponent, currentPlayer);
            } catch (Exception e) {
                // Handle invalid move: you might want to log or handle this case differently
                System.out.println("Invalid move: " + move);
            }
        }

        // After exploring all moves, calculate the branching factor for this depth
        if (nodesVisited == 0) {
            return 0; // Prevent division by zero if no nodes were visited
        }

        return totalMoves / nodesVisited;
    }


    public static void main(String[] args) {
        Board board = new Board(5, 3); // Example 5x5 board
        Player greenPlayer = new GreenPlayer(21, 1);
        Player bluePlayer = new BluePlayer(21, 1);
        BranchingExperiment experiment = new BranchingExperiment(board, greenPlayer, bluePlayer, 5);

        experiment.runExperiment();
    }
}
*/
