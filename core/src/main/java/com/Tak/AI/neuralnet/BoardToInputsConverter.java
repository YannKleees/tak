package com.Tak.AI.neuralnet;

import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.Piece;
import com.Tak.Logic.models.PieceStack;
import com.Tak.Logic.models.Player;

/**
 * <p>Converts a Board state into a neural network input array for evaluation.</p>
 *
 * <p><strong>Example Feature Encoding Strategy:</strong></p>
 * <ul>
 *   <li><b>Top Piece Type:</b> (capstone=±3, stone=±2, wall=±1, 0 if empty)
 *       sign indicates ownership (+ if the `player` in question owns it, - if the opponent does).</li>
 *   <li><b>Stack Height:</b> a direct integer or normalized by some maximum (e.g. / 5).</li>
 *   <li><b>Ownership Ratio:</b> fraction of pieces in stack belonging to `player` minus fraction belonging to opponent.</li>
 *   <li><b>Board Occupancy Ratio:</b> number of non-empty cells / total cells.</li>
 *   <li><b>Current Move Count:</b> e.g., scaled by 100 or something similar.</li>
 * </ul>
 *
 * <p>You can easily adapt these features for your actual board size or add more
 * (like path connectivity, capstone threats, etc.).</p>
 */
public class BoardToInputsConverter {

    /**
     * <p>Converts the board state into a (somewhat) meaningful input array for the neural network.</p>
     */
    public static double[] convert(Board board, Player player) {
        int size = board.getSize();
        // For each cell: (top piece type), (stack height), (ownership ratio)
        // Then add 2 global features: occupancy ratio, scaled moveCount
        int featuresPerCell = 3;
        int totalCellFeatures = size * size * featuresPerCell;
        int totalFeatures = totalCellFeatures + 2;
        double[] inputs = new double[totalFeatures];
        int index = 0;

        int filledCells = 0;
        int totalCells = size * size;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                PieceStack stack = board.getBoardStack(r, c);
                if (stack.isEmpty()) {
                    inputs[index++] = 0.0;
                    inputs[index++] = 0.0;
                    inputs[index++] = 0.0;
                } else {
                    filledCells++;
                    Piece top = stack.getTopPiece();
                    double pieceTypeValue = encodePieceType(top, player);
                    inputs[index++] = pieceTypeValue;

                    int height = stack.size();
                    inputs[index++] = height;

                    double ratio = computeOwnershipRatio(stack, player);
                    inputs[index++] = ratio;
                }
            }
        }

        double occupancyRatio = ((double) filledCells) / totalCells;
        inputs[index++] = occupancyRatio;

        double moveCountScaled = ((double) board.getMoveCount()) / 100.0;
        inputs[index++] = moveCountScaled;

        return inputs;
    }

    private static double encodePieceType(Piece top, Player player) {
        boolean isMyPiece = top.getOwner().equals(player);
        switch (top.getPieceType()) {
            case CAPSTONE:
                return isMyPiece ? 3.0 : -3.0;
            case FLAT_STONE:
                return isMyPiece ? 2.0 : -2.0;
            case STANDING_STONE:
                return isMyPiece ? 1.0 : -1.0;
            default:
                return 0.0;
        }
    }

    private static double computeOwnershipRatio(PieceStack stack, Player player) {
        int myCount = 0;
        int oppCount = 0;
        for (Piece p : stack.getPieces()) {
            if (p.getOwner().equals(player)) {
                myCount++;
            } else {
                oppCount++;
            }
        }
        int total = myCount + oppCount;
        if (total == 0) return 0.0;
        return ((double) (myCount - oppCount)) / total;
    }
}
