package com.Tak.AI.neuralnet.net;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;

import java.util.ArrayDeque;

/**
 * Revised input converter with per-feature scaling for Tanh networks.
 *
 * Key changes:
 *  - Scale down large feature values (like encodePieceType 3 => /3).
 *  - Keep everything roughly in [-1,+1].
 */
public class BoardToInputsConverter {

    public static double[] convert(Board board, Player player) {
        int size = board.getSize();

        // Per-cell features we want (5): [encodedType, stackSz, ownership, adjacency, flattenPotential]
        int featuresPerCell = 5;
        int totalCellFeatures = size * size * featuresPerCell;

        // 5 global features: [occupancy ratio, scaled moveCount, largestGroup(p), largestGroup(opp), difference]
        int globalFeatures = 5;
        int totalFeatures = totalCellFeatures + globalFeatures;

        double[] inputs = new double[totalFeatures];

        int index = 0;
        int filledCells = 0;
        int totalCells = size * size;

        // Per-cell
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                PieceStack stack = board.getBoardStack(r, c);
                if (stack.isEmpty()) {
                    // If empty, all 0
                    for (int k = 0; k < featuresPerCell; k++) {
                        inputs[index++] = 0.0;
                    }
                } else {
                    filledCells++;

                    Piece top = stack.getTopPiece();
                    double encodedType = encodePieceType(top, player);  // => range [-3..+3]
                    double stackSz = stack.size();                      // e.g. 1..5
                    double ownership = computeOwnershipRatio(stack, player); // => [-1..+1]
                    double adjacency = computeAdjacency(board, r, c, player); // => [0..4]
                    double flattenPotential = computeFlattenPotential(board, r, c, player); // => [0..4]

                    // Scale them individually:
                    double scaledType = encodedType / 3.0;      // => [-1..+1]
                    double scaledStack = stackSz / 5.0;         // => [0..1]
                    double scaledOwnership = ownership;         // => [-1..+1]
                    double scaledAdj = adjacency / 4.0;         // => [0..1]
                    double scaledFlatten = flattenPotential / 4.0; // => [0..1]

                    // Now store them
                    inputs[index++] = scaledType;
                    inputs[index++] = scaledStack;
                    inputs[index++] = scaledOwnership;
                    inputs[index++] = scaledAdj;
                    inputs[index++] = scaledFlatten;
                }
            }
        }

        // (A) occupancy ratio => 0..1
        double occupancy = (double) filledCells / totalCells;
        inputs[index++] = occupancy;

        // (B) scaled move count => board.getMoveCount() / 100
        double moveCountScaled = board.getMoveCount() / 100.0;
        if (moveCountScaled > 1.0) {
            moveCountScaled = 1.0; // clamp
        }
        inputs[index++] = moveCountScaled;

        // (C) largest connected groups
        double myLargestGroup = computeLargestConnectedGroup(board, player);
        double oppLargestGroup = computeLargestConnectedGroup(board, player.getOpponent());
        double scaledMyGroup = myLargestGroup / totalCells; // => [0..1]
        double scaledOppGroup = oppLargestGroup / totalCells; // => [0..1]

        inputs[index++] = scaledMyGroup;
        inputs[index++] = scaledOppGroup;

        // (D) difference => scale by totalCells => range ~[-1..+1]
        double diff = (myLargestGroup - oppLargestGroup) / totalCells;
        inputs[index++] = diff;

        return inputs;
    }

    // --------------------------- PER-CELL FEATURES ---------------------------

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
        return (double) (myCount - oppCount) / total; // [-1..1]
    }

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
                    if (!neighborTop.getOwner().equals(player) 
                        && neighborTop.getPieceType() == Piece.PieceType.STANDING_STONE) {
                        flattenCount++;
                    }
                }
            }
        }
        return flattenCount; // 0..4
    }

    // --------------------------- GLOBAL FEATURES ---------------------------

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
