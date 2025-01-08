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
 *   <li><b>Stack Height:</b> a direct integer or normalized by some maximum (e.g. / 5).
 *   <li><b>Ownership Ratio:</b> fraction of pieces in stack belonging to `player` minus fraction belonging to opponent.</li>
 *   <li><b>Board Occupancy Ratio:</b> number of non-empty cells / total cells.</li>
 *   <li><b>Current Move Count:</b> e.g., scaled by 100 or something similar.</li>
 * </ul>
 *
 * <p>You can easily adapt these features for your actual board size or add more (like path connectivity, capstone threats, etc.).</p>
 */
public class BoardToInputsConverter {

    /**
     * <p>Converts the board state into a (somewhat) meaningful input array for the neural network.</p>
     *
     * @param board  The current Tak game board.
     * @param player The player from whose perspective we're evaluating.
     * @return A double[] representing the board state.
     */
    public static double[] convert(Board board, Player player) {
        // For demonstration, let's assume a 5×5 board. If your board size is flexible,
        // you can do something dynamic based on board.getSize().
        int size = board.getSize();
        // For each cell, store 3 features: top piece type, stack height, ownership ratio
        // => 3 * (size*size). Then add 2 global features: board occupancy, moveCount/100
        int featuresPerCell = 3;
        int totalCellFeatures = size * size * featuresPerCell;
        int totalFeatures = totalCellFeatures + 2; 
        // 2 extra: occupancy ratio, scaled moveCount

        double[] inputs = new double[totalFeatures];
        int index = 0;

        // 1) Gather cell-by-cell features
        int filledCells = 0;
        int totalCells = size * size;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                PieceStack stack = board.getBoardStack(r, c);
                // If stack is empty
                if (stack.isEmpty()) {
                    inputs[index++] = 0.0; // top piece type
                    inputs[index++] = 0.0; // stack height
                    inputs[index++] = 0.0; // ownership ratio
                } else {
                    filledCells++;
                    // top piece
                    Piece top = stack.getTopPiece();
                    double pieceTypeValue = encodePieceType(top, player);
                    inputs[index++] = pieceTypeValue;

                    // stack height (could normalize by e.g. / 5 if you have a max stack)
                    int height = stack.size();
                    inputs[index++] = height;

                    // ownership ratio: (# pieces belonging to 'player' - # belonging to opponent)/stack.size
                    double ratio = computeOwnershipRatio(stack, player);
                    inputs[index++] = ratio;
                }
            }
        }

        // 2) Occupancy ratio: how many cells are non-empty / total
        double occupancyRatio = ((double) filledCells) / totalCells;
        inputs[index++] = occupancyRatio;

        // 3) Example: scaled moveCount. If you have a reference to moveCount, pass it in or store in Board
        // Here we assume board has a getMoveCount() method (if not, adapt).
        // We'll just do a placeholder for demonstration:
        double moveCountScaled = ((double) board.getMoveCount()) / 100.0;
        inputs[index++] = moveCountScaled;

        // The final array has length = 3*(size*size) + 2
        return inputs;
    }

    /**
     * <p>Encodes the top piece type as a numeric value based on ownership.</p>
     *
     * @param top    The top piece on a stack.
     * @param player The perspective player.
     * @return A double in [-3..3].
     */
    private static double encodePieceType(Piece top, Player player) {
        // If top belongs to 'player'
        boolean isMyPiece = top.getOwner().equals(player);
        switch (top.getPieceType()) {
            case CAPSTONE:
                return isMyPiece ? 3.0 : -3.0;
            case FLAT_STONE: 
                // You might differentiate STANDING_STONE vs FLAT_STONE if your rules do
                // Suppose STANDING_STONE => ±2.5, FLAT_STONE => ±2.0, etc.
                return isMyPiece ? 2.0 : -2.0;
            case STANDING_STONE:
                return isMyPiece ? 1.0 : -1.0;
            default:
                return 0.0;
        }
    }

    /**
     * <p>Computes ownership ratio in a stack: #myPieces - #theirPieces, all / total stack size.</p>
     *
     * @param stack  The PieceStack in question.
     * @param player The perspective player.
     * @return A double typically in [-1..1].
     */
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
