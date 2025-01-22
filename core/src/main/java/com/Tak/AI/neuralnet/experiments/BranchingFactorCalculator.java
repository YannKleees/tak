package com.Tak.AI.neuralnet.experiments;

import com.Tak.AI.actions.Action;
import com.Tak.AI.utils.ActionGenerator;
import com.Tak.Logic.utils.GameOverException;
import com.Tak.Logic.utils.InvalidMoveException;
import com.Tak.Logic.models.Board;
import com.Tak.Logic.models.TakGame;
import com.Tak.Logic.models.Player;

import java.util.List;
import java.util.Random;

/**
 * A utility class to estimate the average branching factor (legal moves per position)
 * by running random playouts in a TakGame.
 *
 * This version includes a main() method for direct, stand-alone execution.
 */
public class BranchingFactorCalculator {

    private static final Random RNG = new Random();

    /**
     * Main method so you can run this class directly:
     *
     *  1. Creates a 5x5 TakGame with 2 human players (i.e., no AI).
     *  2. Runs 100 random playouts, each up to 100 moves.
     *  3. Prints the estimated branching factor.
     */
    public static void main(String[] args) {
        // Create a 5x5 TakGame with no AI (both players are human, for simplicity).
        // Adapt this constructor call to match your own code if needed.
        TakGame game = new TakGame(5, false, 0);

        // Create the calculator instance
        BranchingFactorCalculator calculator = new BranchingFactorCalculator();

        // Estimate the branching factor using 100 playouts, each up to 100 moves
        double estimatedBF = calculator.estimateBranchingFactor(game, 100000, 100);
        System.out.println("Estimated Branching Factor from random playouts: " + estimatedBF);
    }

    /**
     * Estimates the branching factor by:
     *  1. Resetting (or copying) the game for each playout.
     *  2. Playing up to maxPlayoutLength moves randomly.
     *  3. Counting how many legal moves were possible at each position visited.
     *
     * @param game              A TakGame instance to reset for each playout.
     * @param numberOfPlayouts  How many random playouts to run.
     * @param maxPlayoutLength  The maximum moves per playout before stopping.
     * @return                  Approximate average branching factor across all sampled positions.
     */
    public double estimateBranchingFactor(TakGame game,
                                          int numberOfPlayouts,
                                          int maxPlayoutLength) {

        double totalMoves = 0.0;       // sum of (# of legal moves) for all visited states
        double totalPositions = 0.0;   // count of all visited states

        for (int i = 0; i < numberOfPlayouts; i++) {
            // Reset the game to a fresh board for each playout
            // (If you want to measure from a mid-game state, you'd need a true copy() method.)
            game.resetGame(false);

            int steps = 0;
            while (!game.isGameEnded() && steps < maxPlayoutLength) {
                Board board = game.getBoard();
                Player currentPlayer = game.getCurrentPlayer();
                int currentMoveCount = game.getMoveCount();

                // 1. Generate all legal moves
                List<String> possibleMoves = ActionGenerator
                    .generatePossibleActions(board, currentPlayer, currentMoveCount);

                // Count them toward branching factor stats
                totalMoves += possibleMoves.size();
                totalPositions++;

                // If no moves are possible, we treat this as a terminal position
                if (possibleMoves.isEmpty()) {
                    break;
                }

                // 2. Pick a random move to execute
                String chosenMove = possibleMoves.get(RNG.nextInt(possibleMoves.size()));
                try {
                    Action action = Action.fromString(chosenMove, currentPlayer.getColor());
                    action.execute(board);
                    game.incrementMoveCount();

                    if (!game.isGameEnded()) {
                        game.checkWinConditions();
                    }
                    if (!game.isGameEnded()) {
                        game.switchPlayer();
                    }
                } catch (InvalidMoveException e) {
                    // If something invalid or game-ending occurs, end this playout
                    break;
                }

                steps++;
            }
        }

        // Avoid division by zero if no positions were counted
        if (totalPositions == 0.0) {
            return 0.0;
        }

        return totalMoves / totalPositions;
    }
}
