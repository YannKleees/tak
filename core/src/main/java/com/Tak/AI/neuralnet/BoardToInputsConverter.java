package com.Tak.AI.neuralnet;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * More "refined" converter for neural network inputs:
 * Per-cell features:
 *   1) pieceType encoding
 *   2) stackSize
 *   3) ownershipRatio
 *   4) adjacencyCount
 *   5) (optional) capstoneFlattenPotential
 *
 * Global features:
 *   1) occupancy ratio
 *   2) scaled moveCount
 *   3) largest connected group (this player)
 *   4) largest connected group (opponent)
 *   5) difference in largest-group (this - opponent), or ratio
 */
public class BoardToInputsConverter {

    public static double[] convert(Board board, Player player) {
        int size = board.getSize();

        // Let’s define how many per-cell features we now want:
        // previously we had 4, let's add 1 more for "capstone flatten potential"
        int featuresPerCell = 5; 

        int totalCellFeatures = size * size * featuresPerCell;

        // Let’s define 5 global features now:
        // 1) occupancy ratio
        // 2) scaled moveCount
        // 3) largest group (player)
        // 4) largest group (opponent)
        // 5) difference (largestGroupPlayer - largestGroupOpponent) or ratio
        int globalFeatures = 5;  

        int totalFeatures = totalCellFeatures + globalFeatures;
        double[] inputs = new double[totalFeatures];

        int index = 0;
        int filledCells = 0;
        int totalCells = size * size;

        // 1) For each cell, compute the local features
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                PieceStack stack = board.getBoardStack(r, c);
                if (stack.isEmpty()) {
                    // pieceType => 0
                    // stackSize => 0
                    // ownership => 0
                    // adjacency => 0
                    // flattenPotential => 0
                    for (int k = 0; k < featuresPerCell; k++) {
                        inputs[index++] = 0.0;
                    }
                } else {
                    filledCells++;

                    Piece top = stack.getTopPiece();
                    double encodedType = encodePieceType(top, player);
                    double stackSz = stack.size();
                    double ownership = computeOwnershipRatio(stack, player);
                    double adjacency = computeAdjacency(board, r, c, player);

                    // New: if there's a capstone belonging to 'player' at (r,c),
                    // compute how many opponent standing stones are adjacent
                    double flattenPotential = computeFlattenPotential(board, r, c, player);

                    inputs[index++] = encodedType;
                    inputs[index++] = stackSz;
                    inputs[index++] = ownership;
                    inputs[index++] = adjacency;
                    inputs[index++] = flattenPotential;
                }
            }
        }

        // 2) occupancy ratio
        inputs[index++] = (double) filledCells / totalCells;

        // 3) scaled move count
        double moveCountScaled = board.getMoveCount() / 100.0;
        inputs[index++] = moveCountScaled;

        // 4) largest connected group for player
        double myLargestGroup = computeLargestConnectedGroup(board, player);
        // 5) largest connected group for opponent
        Player opp = player.getOpponent();
        double oppLargestGroup = computeLargestConnectedGroup(board, opp);

        inputs[index++] = myLargestGroup / totalCells;   // optional scale
        inputs[index++] = oppLargestGroup / totalCells;  // optional scale

        // 6) difference or ratio
        double diff = (myLargestGroup - oppLargestGroup) / (double) totalCells;
        inputs[index++] = diff; 
        // Alternatively, you could do ratio => myLargestGroup / (oppLargestGroup + 1).

        return inputs;
    }

    // ---------------------------
    //      PER-CELL FEATURES
    // ---------------------------

    private static double encodePieceType(Piece top, Player player) {
        boolean isMine = top.getOwner().equals(player);
        switch (top.getPieceType()) {
            case CAPSTONE:
                return isMine ? 3.0 : -3.0;
            case FLAT_STONE:
                return isMine ? 2.0 : -2.0;
            case STANDING_STONE:
                return isMine ? 1.0 : -1.0;
            default:
                return 0.0;
        }
    }

    private static double computeOwnershipRatio(PieceStack stack, Player player) {
        int myCount = 0, oppCount = 0;
        for (Piece p : stack.getPieces()) {
            if (p.getOwner().equals(player)) {
                myCount++;
            } else {
                oppCount++;
            }
        }
        int total = myCount + oppCount;
        if (total == 0) return 0.0;
        return (double)(myCount - oppCount) / total; // [-1..1]
    }

    /**
     * Up to 4 neighbors that have top piece of the same color => adjacency
     */
    private static double computeAdjacency(Board board, int r, int c, Player player) {
        int size = board.getSize();
        int[][] offsets = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        PieceStack thisStack = board.getBoardStack(r, c);
        if (thisStack.isEmpty()) return 0.0;

        boolean myCellTop = (thisStack.getTopPiece().getOwner().equals(player));
        if (!myCellTop) return 0.0;

        int sameNeighbors = 0;
        for (int[] off : offsets) {
            int nr = r + off[0];
            int nc = c + off[1];
            if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                PieceStack neighborStack = board.getBoardStack(nr, nc);
                if (!neighborStack.isEmpty()) {
                    boolean neighborOwner = neighborStack.getTopPiece().getOwner().equals(player);
                    if (neighborOwner) {
                        sameNeighbors++;
                    }
                }
            }
        }
        return sameNeighbors; // 0..4
    }

    /**
     * If there's a capstone for 'player' at (r,c), 
     * check how many adjacent opponent standing stones are flattenable.
     */
    private static double computeFlattenPotential(Board board, int r, int c, Player player) {
        PieceStack stack = board.getBoardStack(r, c);
        if (stack.isEmpty()) return 0.0;

        Piece top = stack.getTopPiece();
        if (!top.getOwner().equals(player) 
            || top.getPieceType() != Piece.PieceType.CAPSTONE) {
            return 0.0;
        }

        int size = board.getSize();
        int[][] offsets = {{-1,0},{1,0},{0,-1},{0,1}};
        int flattenCount = 0;
        for (int[] off : offsets) {
            int nr = r + off[0];
            int nc = c + off[1];
            if (nr >= 0 && nr < size && nc >= 0 && nc < size) {
                PieceStack neighbor = board.getBoardStack(nr, nc);
                if (!neighbor.isEmpty()) {
                    Piece neighborTop = neighbor.getTopPiece();
                    // If neighbor is an opponent's standing stone => flatten possible
                    if (!neighborTop.getOwner().equals(player) 
                        && neighborTop.getPieceType() == Piece.PieceType.STANDING_STONE) {
                        flattenCount++;
                    }
                }
            }
        }
        return flattenCount; // 0..4 in most cases
    }

    // ---------------------------
    //      GLOBAL FEATURES
    // ---------------------------

    /**
     * BFS-based approach to find the largest connected group of top pieces
     * owned by the specified player. This is the same logic from your code,
     * but let's keep it separate for player's perspective or opponent's perspective.
     */
    private static double computeLargestConnectedGroup(Board board, Player player) {
        int size = board.getSize();
        boolean[][] visited = new boolean[size][size];
        int maxGroupSize = 0;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (!visited[r][c]) {
                    PieceStack stack = board.getBoardStack(r, c);
                    if (!stack.isEmpty() && stack.getTopPiece().getOwner().equals(player)) {
                        int groupSize = bfsComponent(board, r, c, visited, player);
                        if (groupSize > maxGroupSize) {
                            maxGroupSize = groupSize;
                        }
                    }
                }
            }
        }
        return maxGroupSize; 
    }

    private static int bfsComponent(Board board, int startR, int startC, boolean[][] visited, Player player) {
        int size = board.getSize();
        int[][] offsets = {{-1,0},{1,0},{0,-1},{0,1}};
        int count = 0;

        java.util.Queue<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startR, startC});
        visited[startR][startC] = true;

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            count++;
            int r = cell[0], c = cell[1];

            for (int[] off : offsets) {
                int nr = r + off[0];
                int nc = c + off[1];
                if (nr >= 0 && nr < size && nc >= 0 && nc < size && !visited[nr][nc]) {
                    PieceStack neighborStack = board.getBoardStack(nr, nc);
                    if (!neighborStack.isEmpty() 
                        && neighborStack.getTopPiece().getOwner().equals(player)) {
                        visited[nr][nc] = true;
                        queue.add(new int[]{nr, nc});
                    }
                }
            }
        }
        return count;
    }
}
